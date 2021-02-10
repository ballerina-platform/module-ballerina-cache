/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.stdlib.cache.nativeimpl.concurrentlinkedhashmap;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @param <K>
 * @param <V>
 */
public class ConcurrentLinkedHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {

    /** The maximum weighted capacity of the map. */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /** The maximum weight of a value. */
    static final int MAXIMUM_WEIGHT = 1 << 29;

    /** The maximum number of pending operations per buffer. */
    static final int MAXIMUM_BUFFER_SIZE = 1 << 20;

    /** The number of pending operations per buffer before attempting to drain. */
    static final int BUFFER_THRESHOLD = 16;

    /** The number of buffers to use. */
    static final int NUMBER_OF_BUFFERS;

    /** Mask value for indexing into the buffers. */
    static final int BUFFER_MASK;

    /** The maximum number of operations to perform per amortized drain. */
    static final int AMORTIZED_DRAIN_THRESHOLD;

//    /** A queue that discards all entries. */
//    static final Queue<?> DISCARDING_QUEUE = new DiscardingQueue();

    static {
        int buffers = ceilingNextPowerOfTwo(Runtime.getRuntime().availableProcessors());
        AMORTIZED_DRAIN_THRESHOLD = (1 + buffers) * BUFFER_THRESHOLD;
        NUMBER_OF_BUFFERS = buffers;
        BUFFER_MASK = buffers - 1;
    }

    static int ceilingNextPowerOfTwo(int x) {
        // From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(x - 1));
    }

    /** The draining status of the buffers. */
    enum DrainStatus {

        /** A drain is not taking place. */
        IDLE,

        /** A drain is required due to a pending write modification. */
        REQUIRED,

        /** A drain is in progress. */
        PROCESSING
    }

    // The backing data store holding the key-value associations
    final ConcurrentMap<K, Node> data;
    final int concurrencyLevel;

    // These fields provide support to bound the map by a maximum capacity
    transient LinkedDeque<Node> evictionDeque;

    // must write under lock
    volatile int weightedSize;

    // must write under lock
    volatile int capacity;

    volatile int nextOrder;
    int drainedOrder;

    final Lock evictionLock;
    final Queue<Task>[] buffers;
    transient ExecutorService executor;
    final Weigher<? super V> weigher;
    final AtomicIntegerArray bufferLengths;
    final AtomicReference<DrainStatus> drainStatus;

    transient Set<K> keySet;
    transient Collection<V> values;
    transient Set<Entry<K, V>> entrySet;

    /**
     * Creates an instance based on the builder's configuration.
     */
    @SuppressWarnings({
            "unchecked", "cast"
    })
    ConcurrentLinkedHashMap(Builder<K, V> builder) {
        // The data store and its maximum capacity
        concurrencyLevel = builder.concurrencyLevel;
        capacity = Math.min(builder.capacity, MAXIMUM_CAPACITY);
        data = new ConcurrentHashMap<>(
                builder.initialCapacity,
                0.75f,
                concurrencyLevel);

        // The eviction support
        weigher = builder.weigher;
        executor = builder.executor;
        nextOrder = Integer.MIN_VALUE;
        drainedOrder = Integer.MIN_VALUE;
        evictionLock = new ReentrantLock();
        evictionDeque = new LinkedDeque<>();
        drainStatus = new AtomicReference<>(DrainStatus.IDLE);

        buffers = (Queue<Task>[]) new Queue[NUMBER_OF_BUFFERS];
        bufferLengths = new AtomicIntegerArray(NUMBER_OF_BUFFERS);
        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            buffers[i] = new ConcurrentLinkedQueue<>();
        }
    }

    /** Asserts that the object is not null. */
    static void checkNotNull(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
    }

    /* ---------------- Eviction Support -------------- */

    /**
     * Retrieves the maximum weighted capacity of the map.
     *
     * @return the maximum weighted capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Sets the maximum weighted capacity of the map and eagerly evicts entries until it
     * shrinks to the appropriate size.
     *
     * @param capacity the maximum weighted capacity of the map
     * @throws IllegalArgumentException if the capacity is negative
     */
    public void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }

        evictionLock.lock();
        try {
            this.capacity = Math.min(capacity, MAXIMUM_CAPACITY);
            drainBuffers(AMORTIZED_DRAIN_THRESHOLD);
            evict();
        } finally {
            evictionLock.unlock();
        }
    }

    /** Determines whether the map has exceeded its capacity. */
    boolean hasOverflowed() {
        return weightedSize > capacity;
    }

    /**
     * Evicts entries from the map while it exceeds the capacity and appends evicted
     * entries to the notification queue for processing.
     */
    void evict() {
        // Attempts to evict entries from the map if it exceeds the maximum
        // capacity. If the eviction fails due to a concurrent removal of the
        // victim, that removal may cancel out the addition that triggered this
        // eviction. The victim is eagerly unlinked before the removal task so
        // that if an eviction is still required then a new victim will be chosen
        // for removal.
        while (hasOverflowed()) {
            Node node = evictionDeque.poll();

            // If weighted values are used, then the pending operations will adjust
            // the size to reflect the correct weight
            if (node == null) {
                return;
            }
            node.makeDead();
        }
    }

    /**
     * Performs the post-processing work required after the map operation.
     *
     * @param task the pending operation to be applied
     */
    void afterCompletion(Task task) {
        boolean delayable = schedule(task);
        if (shouldDrainBuffers(delayable)) {
            tryToDrainBuffers(AMORTIZED_DRAIN_THRESHOLD);
        }
    }

    /**
     * Schedules the task to be applied to the page replacement policy.
     *
     * @param task the pending operation
     * @return if the draining of the buffers can be delayed
     */
    private boolean schedule(Task task) {
        int index = bufferIndex();
        int buffered = bufferLengths.incrementAndGet(index);

        if (task.isWrite()) {
            buffers[index].add(task);
            drainStatus.set(DrainStatus.REQUIRED);
            return false;
        }

        // A buffer may discard a read task if its length exceeds a tolerance level
        if (buffered <= MAXIMUM_BUFFER_SIZE) {
            buffers[index].add(task);
            return (buffered <= BUFFER_THRESHOLD);
        } else { // not optimized for fail-safe scenario
            bufferLengths.decrementAndGet(index);
            return false;
        }
    }

    /** Returns the index to the buffer that the task should be scheduled on. */
    static int bufferIndex() {
        // A buffer is chosen by the thread's id so that tasks are distributed in a
        // pseudo evenly manner. This helps avoid hot entries causing contention due
        // to other threads trying to append to the same buffer.
        return (int) Thread.currentThread().getId() & BUFFER_MASK;
    }

    /** Returns the ordering value to assign to a task. */
    int nextOrdering() {
        // The next ordering is acquired in a racy fashion as the increment is not
        // atomic with the insertion into a buffer. This means that concurrent tasks
        // can have the same ordering and the buffers are in a weakly sorted order.
        return nextOrder++;
    }

    /**
     * Determines whether the buffers should be drained.
     *
     * @param delayable if a drain should be delayed until required
     * @return if a drain should be attempted
     */
    boolean shouldDrainBuffers(boolean delayable) {
        if (executor.isShutdown()) {
            DrainStatus status = drainStatus.get();
            return (status != DrainStatus.PROCESSING)
                    && (!delayable || (status == DrainStatus.REQUIRED));
        }
        return false;
    }

    /**
     * Attempts to acquire the eviction lock and apply the pending operations to the page
     * replacement policy.
     *
     * @param maxToDrain the maximum number of operations to drain
     */
    void tryToDrainBuffers(int maxToDrain) {
        if (evictionLock.tryLock()) {
            try {
                drainStatus.set(DrainStatus.PROCESSING);
                drainBuffers(maxToDrain);
            } finally {
                drainStatus.compareAndSet(DrainStatus.PROCESSING, DrainStatus.IDLE);
                evictionLock.unlock();
            }
        }
    }

    /**
     * Drains the buffers and applies the pending operations.
     *
     * @param maxToDrain the maximum number of operations to drain
     */
    void drainBuffers(int maxToDrain) {
        // A mostly strict ordering is achieved by observing that each buffer
        // contains tasks in a weakly sorted order starting from the last drain.
        // The buffers can be merged into a sorted list in O(n) time by using
        // counting sort and chaining on a collision.

        // The output is capped to the expected number of tasks plus additional
        // slack to optimistically handle the concurrent additions to the buffers.
        Task[] tasks = new Task[maxToDrain];

        // Moves the tasks into the output array, applies them, and updates the
        // marker for the starting order of the next drain.
        int maxTaskIndex = moveTasksFromBuffers(tasks);
        runTasks(tasks, maxTaskIndex);
        updateDrainedOrder(tasks, maxTaskIndex);
    }

    /**
     * Moves the tasks from the buffers into the output array.
     *
     * @param tasks the ordered array of the pending operations
     * @return the highest index location of a task that was added to the array
     */
    int moveTasksFromBuffers(Task[] tasks) {
        int maxTaskIndex = -1;
        for (int i = 0; i < buffers.length; i++) {
            int maxIndex = moveTasksFromBuffer(tasks, i);
            maxTaskIndex = Math.max(maxIndex, maxTaskIndex);
        }
        return maxTaskIndex;
    }

    /**
     * Moves the tasks from the specified buffer into the output array.
     *
     * @param tasks the ordered array of the pending operations
     * @param bufferIndex the buffer to drain into the tasks array
     * @return the highest index location of a task that was added to the array
     */
    int moveTasksFromBuffer(Task[] tasks, int bufferIndex) {
        // While a buffer is being drained it may be concurrently appended to. The
        // number of tasks removed are tracked so that the length can be decremented
        // by the delta rather than set to zero.
        Queue<Task> buffer = buffers[bufferIndex];
        int removedFromBuffer = 0;

        Task task;
        int maxIndex = -1;
        while ((task = buffer.poll()) != null) {
            removedFromBuffer++;

            // The index into the output array is determined by calculating the offset
            // since the last drain
            int index = task.getOrder() - drainedOrder;
            if (index < 0) {
                // The task was missed by the last drain and can be run immediately
                task.run();
            } else if (index >= tasks.length) {
                // Due to concurrent additions, the order exceeds the capacity of the
                // output array. It is added to the end as overflow and the remaining
                // tasks in the buffer will be handled by the next drain.
                maxIndex = tasks.length - 1;
                addTaskToChain(tasks, task, maxIndex);
                break;
            } else {
                maxIndex = Math.max(index, maxIndex);
                addTaskToChain(tasks, task, index);
            }
        }
        bufferLengths.addAndGet(bufferIndex, -removedFromBuffer);
        return maxIndex;
    }

    /**
     * Adds the task as the head of the chain at the index location.
     *
     * @param tasks the ordered array of the pending operations
     * @param task the pending operation to add
     * @param index the array location
     */
    void addTaskToChain(Task[] tasks, Task task, int index) {
        task.setNext(tasks[index]);
        tasks[index] = task;
    }

    /**
     * Runs the pending page replacement policy operations.
     *
     * @param tasks the ordered array of the pending operations
     * @param maxTaskIndex the maximum index of the array
     */
    void runTasks(Task[] tasks, int maxTaskIndex) {
        for (int i = 0; i <= maxTaskIndex; i++) {
            runTasksInChain(tasks[i]);
        }
    }

    /**
     * Runs the pending operations on the linked chain.
     *
     * @param task the first task in the chain of operations
     */
    void runTasksInChain(Task task) {
        while (task != null) {
            Task current = task;
            task = task.getNext();
            current.setNext(null);
            current.run();
        }
    }

    /**
     * Updates the order to start the next drain from.
     *
     * @param tasks the ordered array of operations
     * @param maxTaskIndex the maximum index of the array
     */
    void updateDrainedOrder(Task[] tasks, int maxTaskIndex) {
        if (maxTaskIndex >= 0) {
            Task task = tasks[maxTaskIndex];
            drainedOrder = task.getOrder() + 1;
        }
    }

    /** Updates the node's location in the page replacement policy. */
    class ReadTask extends AbstractTask {

        final Node node;

        ReadTask(Node node) {
            this.node = node;
        }

        public void run() {
            // An entry may scheduled for reordering despite having been previously
            // removed. This can occur when the entry was concurrently read while a
            // writer was removing it. If the entry is no longer linked then it does
            // not need to be processed.
            if (evictionDeque.contains(node)) {
                evictionDeque.moveToBack(node);
            }
        }

        public boolean isWrite() {
            return false;
        }
    }

    /** Adds the node to the page replacement policy. */
    final class AddTask extends AbstractTask {

        final Node node;
        final int weight;

        AddTask(Node node, int weight) {
            this.weight = weight;
            this.node = node;
        }

        public void run() {
            weightedSize += weight;

            // ignore out-of-order write operations
            if (node.get().isAlive()) {
                evictionDeque.add(node);
                evict();
            }
        }

        public boolean isWrite() {
            return true;
        }
    }

    /** Removes a node from the page replacement policy. */
    final class RemovalTask extends AbstractTask {

        final Node node;

        RemovalTask(Node node) {
            this.node = node;
        }

        public void run() {
            // add may not have been processed yet
            evictionDeque.remove(node);
            node.makeDead();
        }

        public boolean isWrite() {
            return true;
        }
    }

    /** Updates the weighted size and evicts an entry on overflow. */
    final class UpdateTask extends ReadTask {

        final int weightDifference;

        public UpdateTask(Node node, int weightDifference) {
            super(node);
            this.weightDifference = weightDifference;
        }

        @Override
        public void run() {
            super.run();
            weightedSize += weightDifference;
            evict();
        }

        @Override
        public boolean isWrite() {
            return true;
        }
    }

    /* ---------------- Concurrent Map Support -------------- */

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int size() {
        return data.size();
    }

    /**
     * Returns the weighted size of this map.
     *
     * @return the combined weight of the values in this map
     */
    public int weightedSize() {
        return Math.max(0, weightedSize);
    }

    @Override
    public void clear() {
        // The alternative is to iterate through the keys and call #remove(), which
        // adds unnecessary contention on the eviction lock and buffers.
        evictionLock.lock();
        try {
            Node node;
            while ((node = evictionDeque.poll()) != null) {
                data.remove(node.key, node);
                node.makeDead();
            }

            // Drain the buffers and run only the write tasks
            for (int i = 0; i < buffers.length; i++) {
                Queue<Task> buffer = buffers[i];
                int removed = 0;
                Task task;
                while ((task = buffer.poll()) != null) {
                    if (task.isWrite()) {
                        task.run();
                    }
                    removed++;
                }
                bufferLengths.addAndGet(i, -removed);
            }
        } finally {
            evictionLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        checkNotNull(value);

        for (Node node : data.values()) {
            if (node.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        final Node node = data.get(key);
        if (node == null) {
            return null;
        }
        afterCompletion(new ReadTask(node));
        return node.getValue();
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, false);
    }

    public V putIfAbsent(K key, V value) {
        return put(key, value, true);
    }

    /**
     * Adds a node to the list and the data store. If an existing node is found, then its
     * value is updated if allowed.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param onlyIfAbsent a write is performed only if the key is not already associated
     *            with a value
     * @return the prior value in the data store or null if no mapping was found
     */
    V put(K key, V value, boolean onlyIfAbsent) {
        checkNotNull(value);

        final int weight = weigher.weightOf(value);
        final WeightedValue<V> weightedValue = new WeightedValue<>(value, weight);
        final Node node = new Node(key, weightedValue);

        for (;;) {
            final Node prior = data.putIfAbsent(node.key, node);
            if (prior == null) {
                afterCompletion(new AddTask(node, weight));
                return null;
            } else if (onlyIfAbsent) {
                afterCompletion(new ReadTask(prior));
                return prior.getValue();
            }
            for (;;) {
                final WeightedValue<V> oldWeightedValue = prior.get();
                if (!oldWeightedValue.isAlive()) {
                    break;
                }
                if (prior.compareAndSet(oldWeightedValue, weightedValue)) {
                    final int weightedDifference = weight - oldWeightedValue.weight;
                    final Task task = (weightedDifference == 0)
                            ? new ReadTask(prior)
                            : new UpdateTask(prior, weightedDifference);
                    afterCompletion(task);
                    return oldWeightedValue.value;
                }
            }
        }
    }

    @Override
    public V remove(Object key) {
        final Node node = data.remove(key);
        if (node == null) {
            return null;
        }

        node.makeRetired();
        afterCompletion(new RemovalTask(node));
        return node.getValue();
    }

    public boolean remove(Object key, Object value) {
        Node node = data.get(key);
        if ((node == null) || (value == null)) {
            return false;
        }

        WeightedValue<V> weightedValue = node.get();
        for (;;) {
            if (weightedValue.hasValue(value)) {
                if (node.tryToRetire(weightedValue)) {
                    if (data.remove(key, node)) {
                        afterCompletion(new RemovalTask(node));
                        return true;
                    }
                } else {
                    weightedValue = node.get();
                    if (weightedValue.isAlive()) {
                        // retry as an intermediate update may have replaced the value
                        // with
                        // an equal instance that has a different reference identity
                        continue;
                    }
                }
            }
            return false;
        }
    }

    public V replace(K key, V value) {
        checkNotNull(value);

        final int weight = weigher.weightOf(value);
        final WeightedValue<V> weightedValue = new WeightedValue<>(value, weight);

        final Node node = data.get(key);
        if (node == null) {
            return null;
        }
        for (;;) {
            WeightedValue<V> oldWeightedValue = node.get();
            if (!oldWeightedValue.isAlive()) {
                return null;
            }
            if (node.compareAndSet(oldWeightedValue, weightedValue)) {
                int weightedDifference = weight - oldWeightedValue.weight;
                final Task task = (weightedDifference == 0)
                        ? new ReadTask(node)
                        : new UpdateTask(node, weightedDifference);
                afterCompletion(task);
                return oldWeightedValue.value;
            }
        }
    }

    public boolean replace(K key, V oldValue, V newValue) {
        checkNotNull(oldValue);
        checkNotNull(newValue);

        final int weight = weigher.weightOf(newValue);
        final WeightedValue<V> newWeightedValue = new WeightedValue<>(newValue, weight);

        final Node node = data.get(key);
        if (node == null) {
            return false;
        }
        for (;;) {
            final WeightedValue<V> weightedValue = node.get();
            if (!weightedValue.isAlive() || !weightedValue.hasValue(oldValue)) {
                return false;
            }
            if (node.compareAndSet(weightedValue, newWeightedValue)) {
                int weightedDifference = weight - weightedValue.weight;
                final Task task = (weightedDifference == 0)
                        ? new ReadTask(node)
                        : new UpdateTask(node, weightedDifference);
                afterCompletion(task);
                return true;
            }
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks == null) ? (keySet = new KeySet()) : ks;
    }


    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs == null) ? (values = new Values()) : vs;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = entrySet;
        return (es == null) ? (entrySet = new EntrySet()) : es;
    }

    /** A value, its weight, and the entry's status. */
    static final class WeightedValue<V> {

        final int weight;
        final V value;

        WeightedValue(V value, int weight) {
            this.weight = weight;
            this.value = value;
        }

        boolean hasValue(Object o) {
            return (o == value) || value.equals(o);
        }

        /**
         * If the entry is available in the hash-table and page replacement policy.
         */
        boolean isAlive() {
            return weight > 0;
        }

        /**
         * If the entry was removed from the hash-table and is awaiting removal from the
         * page replacement policy.
         */
        boolean isRetired() {
            return weight < 0;
        }

        /**
         * If the entry was removed from the hash-table and the page replacement policy.
         */
        boolean isDead() {
            return weight == 0;
        }
    }

    /**
     * A node contains the key, the weighted value, and the linkage pointers on the
     * page-replacement algorithm's data structures.
     */
    @SuppressWarnings("serial")
    final class Node extends AtomicReference<WeightedValue<V>> implements Linked<Node> {
        static final long serialVersionUID = 1;
        final K key;

        Node prev;
        Node next;
        /** Creates a new, unlinked node. */
        Node(K key, WeightedValue<V> weightedValue) {
            super(weightedValue);
            this.key = key;
        }

        public Node getPrevious() {
            return prev;
        }

        public void setPrevious(Node prev) {
            this.prev = prev;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }

        /** Retrieves the value held by the current <tt>WeightedValue</tt>. */
        V getValue() {
            return get().value;
        }

        /**
         * Attempts to transition the node from the <tt>alive</tt> state to the
         * <tt>retired</tt> state.
         *
         * @param expect the expected weighted value
         * @return if successful
         */
        boolean tryToRetire(WeightedValue<V> expect) {
            if (expect.isAlive()) {
                WeightedValue<V> retired = new WeightedValue<>(
                        expect.value,
                        -expect.weight);
                return compareAndSet(expect, retired);
            }
            return false;
        }

        /**
         * Atomically transitions the node from the <tt>alive</tt> state to the
         * <tt>retired</tt> state, if a valid transition.
         */
        void makeRetired() {
            for (;;) {
                WeightedValue<V> current = get();
                if (!current.isAlive()) {
                    return;
                }
                WeightedValue<V> retired = new WeightedValue<>(
                        current.value,
                        -current.weight);
                if (compareAndSet(current, retired)) {
                    return;
                }
            }
        }

        /**
         * Atomically transitions the node to the <tt>dead</tt> state and decrements the
         * <tt>weightedSize</tt>.
         */
        void makeDead() {
            for (;;) {
                WeightedValue<V> current = get();
                WeightedValue<V> dead = new WeightedValue<>(current.value, 0);
                if (compareAndSet(current, dead)) {
                    weightedSize -= Math.abs(current.weight);
                    return;
                }
            }
        }
    }

    /** An adapter to safely externalize the keys. */
    final class KeySet extends AbstractSet<K> {

        final ConcurrentLinkedHashMap<K, V> map = ConcurrentLinkedHashMap.this;

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        @Override
        public boolean contains(Object obj) {
            return containsKey(obj);
        }

        @Override
        public boolean remove(Object obj) {
            return (map.remove(obj) != null);
        }

        @Override
        public Object[] toArray() {
            return map.data.keySet().toArray();
        }

        @Override
        public <T> T[] toArray(T[] array) {
            return map.data.keySet().toArray(array);
        }
    }

    /** An adapter to safely externalize the key iterator. */
    final class KeyIterator implements Iterator<K> {

        final Iterator<K> iterator = data.keySet().iterator();
        K current;

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public K next() {
            current = iterator.next();
            return current;
        }

        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            }
            ConcurrentLinkedHashMap.this.remove(current);
            current = null;
        }
    }

    /** An adapter to safely externalize the values. */
    final class Values extends AbstractCollection<V> {

        @Override
        public int size() {
            return ConcurrentLinkedHashMap.this.size();
        }

        @Override
        public void clear() {
            ConcurrentLinkedHashMap.this.clear();
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
    }

    /** An adapter to safely externalize the value iterator. */
    final class ValueIterator implements Iterator<V> {

        final Iterator<Node> iterator = data.values().iterator();
        Node current;

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public V next() {
            current = iterator.next();
            return current.getValue();
        }

        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            }
            ConcurrentLinkedHashMap.this.remove(current.key);
            current = null;
        }
    }

    /** An adapter to safely externalize the entries. */
    final class EntrySet extends AbstractSet<Entry<K, V>> {

        final ConcurrentLinkedHashMap<K, V> map = ConcurrentLinkedHashMap.this;

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object obj) {
            if (!(obj instanceof Entry<?, ?>)) {
                return false;
            }
            Entry<?, ?> entry = (Entry<?, ?>) obj;
            Node node = map.data.get(entry.getKey());
            return (node != null) && (node.getValue().equals(entry.getValue()));
        }

        @Override
        public boolean add(Entry<K, V> entry) {
            return (map.putIfAbsent(entry.getKey(), entry.getValue()) == null);
        }

        @Override
        public boolean remove(Object obj) {
            if (!(obj instanceof Entry<?, ?>)) {
                return false;
            }
            Entry<?, ?> entry = (Entry<?, ?>) obj;
            return map.remove(entry.getKey(), entry.getValue());
        }
    }

    /** An adapter to safely externalize the entry iterator. */
    final class EntryIterator implements Iterator<Entry<K, V>> {

        final Iterator<Node> iterator = data.values().iterator();
        Node current;

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Entry<K, V> next() {
            current = iterator.next();
            return new WriteThroughEntry(current);
        }

        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            }
            ConcurrentLinkedHashMap.this.remove(current.key);
            current = null;
        }
    }

    /** An entry that allows updates to write through to the map. */
     class WriteThroughEntry extends AbstractMap.SimpleEntry<K, V> {

        static final long serialVersionUID = 1;
//
        WriteThroughEntry(Node node) {
            super(node.key, node.getValue());
        }

        @Override
        public V setValue(V value) {
            put(getKey(), value);
            return super.setValue(value);
        }

        Object writeReplace() {
            return new AbstractMap.SimpleEntry<>(this);
        }
    }

    /** A weigher that enforces that the weight falls within a valid range. */
    static final class BoundedWeigher<V> implements Weigher<V>, Serializable {

        static final long serialVersionUID = 1;
        final Weigher<? super V> weigher;

        BoundedWeigher(Weigher<? super V> weigher) {
            checkNotNull(weigher);
            this.weigher = weigher;
        }

        public int weightOf(V value) {
            int weight = weigher.weightOf(value);
            if ((weight < 1) || (weight > MAXIMUM_WEIGHT)) {
                throw new IllegalArgumentException("invalid weight");
            }
            return weight;
        }

        Object writeReplace() {
            return weigher;
        }
    }

//    /** A task that catches up the page replacement policy. */
//    static final class CatchUpTask implements Runnable {
//
//        final WeakReference<ConcurrentLinkedHashMap<?, ?>> mapRef;
//
//        CatchUpTask(ConcurrentLinkedHashMap<?, ?> map) {
//            this.mapRef = new WeakReference<ConcurrentLinkedHashMap<?, ?>>(map);
//        }
//
//        public void run() {
//            ConcurrentLinkedHashMap<?, ?> map = mapRef.get();
//            if (map == null) {
//                throw new CancellationException();
//            }
//            int pendingTasks = 0;
//            for (int i = 0; i < map.buffers.length; i++) {
//                pendingTasks += map.bufferLengths.get(i);
//            }
//            if (pendingTasks != 0) {
//                map.tryToDrainBuffers(pendingTasks + BUFFER_THRESHOLD);
//            }
//        }
//    }

    /** An executor that is always terminated. */
    static final class DisabledExecutorService extends AbstractExecutorService {

        public boolean isShutdown() {
            return true;
        }

        public boolean isTerminated() {
            return true;
        }

        public void shutdown() {
        }

        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        public void execute(Runnable command) {
            throw new RejectedExecutionException();
        }
    }

//    /** A queue that discards all additions and is always empty. */
//    static final class DiscardingQueue extends AbstractQueue<Object> {
//
//        @Override
//        public boolean add(Object e) {
//            return true;
//        }
//
//        public boolean offer(Object e) {
//            return true;
//        }
//
//        public Object poll() {
//            return null;
//        }
//
//        public Object peek() {
//            return null;
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//        @Override
//        public Iterator<Object> iterator() {
//            return Collections.emptyList().iterator();
//        }
//    }


    /** An operation that can be lazily applied to the page replacement policy. */
    interface Task extends Runnable {

        /** The priority order. */
        int getOrder();

        /** If the task represents an add, modify, or remove operation. */
        boolean isWrite();

        /** Returns the next task on the link chain. */
        Task getNext();

        /** Sets the next task on the link chain. */
        void setNext(Task task);
    }

    /** A skeletal implementation of the <tt>Task</tt> interface. */
    abstract class AbstractTask implements Task {

        final int order;
        Task task;

        AbstractTask() {
            order = nextOrdering();
        }

        public int getOrder() {
            return order;
        }

        public Task getNext() {
            return task;
        }

        public void setNext(Task task) {
            this.task = task;
        }
    }

//    /* ---------------- Serialization Support -------------- */

    static final long serialVersionUID = 1;

    Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    /**
     * A proxy that is serialized instead of the map. The page-replacement algorithm's
     * data structures are not serialized so the deserialized instance contains only the
     * entries. This is acceptable as caches hold transient data that is recomputable and
     * serialization would tend to be used as a fast warm-up process.
     */
    static final class SerializationProxy<K, V> implements Serializable {

        final Weigher<? super V> weigher;
        final int concurrencyLevel;
        final Map<K, V> data;
        final int capacity;

        SerializationProxy(ConcurrentLinkedHashMap<K, V> map) {
            concurrencyLevel = map.concurrencyLevel;
            data = new HashMap<>(map);
            capacity = map.capacity;
            weigher = map.weigher;
        }

        Object readResolve() {
            ConcurrentLinkedHashMap<K, V> map = new Builder<K, V>()
                    .concurrencyLevel(concurrencyLevel)
                    .maximumWeightedCapacity(capacity)
                    .weigher(weigher)
                    .build();
            map.putAll(data);
            return map;
        }

        static final long serialVersionUID = 1;
    }

    /* ---------------- Builder -------------- */

    /**
     * A builder that creates {@link ConcurrentLinkedHashMap} instances. It provides a
     * flexible approach for constructing customized instances with a named parameter
     * syntax.
     *
     * @param <K>
     * @param <V>
     */
    public static final class Builder<K, V> {

        static final ExecutorService DEFAULT_EXECUTOR = new DisabledExecutorService();
        static final int DEFAULT_CONCURRENCY_LEVEL = 16;
        static final int DEFAULT_INITIAL_CAPACITY = 16;

        Weigher<? super V> weigher;

        ExecutorService executor;
//        TimeUnit unit;
//        long delay;

        int concurrencyLevel;
        int initialCapacity;
        int capacity;

        @SuppressWarnings("unchecked")
        public Builder() {
            capacity = -1;
            executor = DEFAULT_EXECUTOR;
            weigher = Weighers.singleton();
            initialCapacity = DEFAULT_INITIAL_CAPACITY;
            concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
        }

        /**
         * Specifies the initial capacity of the hash table (default <tt>16</tt>). This is
         * the number of key-value pairs that the hash table can hold before a resize
         * operation is required.
         *
         * @param initialCapacity the initial capacity used to size the hash table to
         *            accommodate this many entries.
         * @throws IllegalArgumentException if the initialCapacity is negative
         */
        public Builder<K, V> initialCapacity(int initialCapacity) {
            if (initialCapacity < 0) {
                throw new IllegalArgumentException();
            }
            this.initialCapacity = initialCapacity;
            return this;
        }

        /**
         * Specifies the maximum weighted capacity to coerce the map to and may exceed it
         * temporarily.
         *
         * @param capacity the weighted threshold to bound the map by
         * @throws IllegalArgumentException if the maximumWeightedCapacity is negative
         */
        public Builder<K, V> maximumWeightedCapacity(int capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException();
            }
            this.capacity = capacity;
            return this;
        }

        /**
         * Specifies the estimated number of concurrently updating threads. The
         * implementation performs internal sizing to try to accommodate this many threads
         * (default <tt>16</tt>).
         *
         * @param concurrencyLevel the estimated number of concurrently updating threads
         * @throws IllegalArgumentException if the concurrencyLevel is less than or equal
         *             to zero
         */
        public Builder<K, V> concurrencyLevel(int concurrencyLevel) {
            if (concurrencyLevel <= 0) {
                throw new IllegalArgumentException();
            }
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        /**
         * Specifies an algorithm to determine how many the units of capacity a value
         * consumes. The default algorithm bounds the map by the number of key-value pairs
         * by giving each entry a weight of <tt>1</tt>.
         *
         * @param weigher the algorithm to determine a value's weight
         * @throws NullPointerException if the weigher is null
         */
        public Builder<K, V> weigher(Weigher<? super V> weigher) {
            this.weigher = (weigher == Weighers.singleton())
                    ? Weighers.<V>singleton()
                    : new BoundedWeigher<>(weigher);
            return this;
        }

        /**
         * Creates a new {@link ConcurrentLinkedHashMap} instance.
         *
         * @throws IllegalStateException if the maximum weighted capacity was not set
         * @throws RejectedExecutionException if an executor was specified and the
         *             catch-up task cannot be scheduled for execution
         */
        public ConcurrentLinkedHashMap<K, V> build() {
            if (capacity < 0) {
                throw new IllegalStateException();
            }
            ConcurrentLinkedHashMap<K, V> map = new ConcurrentLinkedHashMap<>(this);
//            if (executor != DEFAULT_EXECUTOR) {
//                ScheduledExecutorService es = (ScheduledExecutorService) executor;
//                es.scheduleWithFixedDelay(new CatchUpTask(map), delay, delay, unit);
//            }
            return map;
        }
    }
}
