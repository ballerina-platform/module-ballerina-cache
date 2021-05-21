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

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *  This class provides a doubly-linked list that is optimized for the virtual
 *  machine. The first and last elements are manipulated instead of a slightly
 *  more convenient sentinel element to avoid the insertion of null checks with
 *  NullPointerException throws in the byte code. The links to a removed
 *  element are cleared to help a generational garbage collector if the
 *  discarded elements inhabit more than one generation.
 *
 * @param <E> the type of elements held in this collection
 */
public class LinkedDeque<E extends Linked<E>> extends AbstractCollection<E> {

    /**
     * Pointer to first node. Invariant: (first == null && last == null) || (first.prev ==
     * null)
     */
    E first = null;

    /**
     * Pointer to last node. Invariant: (first == null && last == null) || (last.next ==
     * null)
     */
    E last = null;

    /**
     * Links the element to the front of the deque so that it becomes the first element.
     *
     * @param e the unlinked element
     */
    void linkFirst(final E e) {
        final E f = first;
        first = e;

        if (f == null) {
            last = e;
        } else {
            f.setPrevious(e);
            e.setNext(f);
        }
    }

    public void addFirst(E e) {
        if (!offerFirst(e)) {
            throw new IllegalArgumentException();
        }
    }

    public boolean offerFirst(E e) {
        if (contains(e)) {
            return false;
        }
        linkFirst(e);
        return true;
    }
    /**
     * Links the element to the back of the deque so that it becomes the last element.
     *
     * @param e the unlinked element
     */
    private void linkLast(final E e) {
        final E l = last;
        last = e;

        if (l == null) {
            first = e;
        } else {
            l.setNext(e);
            e.setPrevious(l);
        }
    }

    /** Unlinks the non-null first element. */
    E unlinkFirst() {
        final E f = first;
        final E next = f.getNext();
        f.setNext(null);

        first = next;
        if (next == null) {
            last = null;
        } else {
            next.setPrevious(null);
        }
        return f;
    }

    /** Unlinks the non-null element. */
    private void unlink(E e) {
        final E prev = e.getPrevious();
        final E next = e.getNext();

        if (prev == null) {
            first = next;
        } else {
            prev.setNext(next);
            e.setPrevious(null);
        }

        if (next == null) {
            last = prev;
        } else {
            next.setPrevious(prev);
            e.setNext(null);
        }
    }

    public boolean isEmpty() {
        return (first == null);
    }

    public boolean contains(Object o) {
        return (o instanceof Linked<?>) && contains((Linked<?>) o);
    }

    // A fast-path containment check
    boolean contains(Linked<?> e) {
        return (e.getPrevious() != null) || (e.getNext() != null) || (e == first);
    }

    /**
     * Moves the element to the back of the deque so that it becomes the last element.
     *
     * @param e the linked element
     */
    public void moveToBack(E e) {
        if (e != last) {
            unlink(e);
            linkLast(e);
        }
    }

    public boolean offerLast(E e) {
        if (contains(e)) {
            return false;
        }
        linkLast(e);
        return true;
    }

    public boolean add(E e) {
        return offerLast(e);
    }

    public E poll() {
        return pollFirst();
    }

    public E pollFirst() {
        if (isEmpty()) {
            return null;
        }
        return unlinkFirst();
    }

    public E pollLast() {
        if (isEmpty()) {
            return null;
        }
        return unlinkLast();
    }

    /** Unlinks the non-null last element. */
    E unlinkLast() {
        final E l = last;
        final E prev = l.getPrevious();
        l.setPrevious(null);
        last = prev;
        if (prev == null) {
            first = null;
        } else {
            prev.setNext(null);
        }
        return l;
    }

    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        if (contains(o)) {
            unlink((E) o);
            return true;
        }
        return false;
    }

    public E removeLast() {
        checkNotEmpty();
        return pollLast();
    }

    void checkNotEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new AbstractLinkedIterator(first) {

            @Override
            E computeNext() {
                return cursor.getNext();
            }
        };
    }

    @Override
    public int size() {
        return 0;
    }

    public Iterator<E> descendingIterator() {
        return new AbstractLinkedIterator(last) {

            @Override
            E computeNext() {
                return cursor.getPrevious();
            }
        };
    }

    abstract class AbstractLinkedIterator implements Iterator<E> {

        E cursor;

        /**
         * Creates an iterator that can can traverse the deque.
         *
         * @param start the initial element to begin traversal from
         */
        AbstractLinkedIterator(E start) {
            cursor = start;
        }

        public boolean hasNext() {
            return (cursor != null);
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E e = cursor;
            cursor = computeNext();
            return e;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Retrieves the next element to traverse to or <tt>null</tt> if there are no more
         * elements.
         */
        abstract E computeNext();
    }
}
