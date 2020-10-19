package org.ballerinalang.stdlib.cache.nativeimpl;

import org.ballerinalang.jvm.api.values.BMap;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.jvm.api.values.BString;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class LinkedList {

    private static class Node {

        BMap<BString, Object> value = null;
        final AtomicReference<Node> next = new AtomicReference<>(null);

        Node(BMap<BString, Object> item, Node next) {
            this.value = item;
            this.next.set(next);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static AtomicReference<Node> head = new AtomicReference<>(new Node(null, null));
    private static AtomicReference<Node> tail = new AtomicReference<>(head.get());

    @SuppressWarnings("unchecked")
    public static void externIni(BObject lruEvictionPolicy) {
        lruEvictionPolicy.addNativeData(Constant.HEAD, head);
        lruEvictionPolicy.addNativeData(Constant.TAIL, tail);
    }

    @SuppressWarnings("unchecked")
    public static void externAddLast(BObject lruEvictionPolicy, BMap<BString, Object> node) {
        tail = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.TAIL);
        Node currentTail = tail.get();
        BMap<BString, Object> value = (BMap<BString, Object>) node.getMapValue(Constant.VALUE);
        Node newNode = new Node(value, null);
        currentTail.next.compareAndSet(null, newNode);
        tail.set(currentTail);
        lruEvictionPolicy.addNativeData(Constant.TAIL, tail);
    }

    @SuppressWarnings("unchecked")
    public static void externAddFirst(BObject lruEvictionPolicy, BMap<BString, Object> node) {
        head = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.HEAD);
        BMap<BString, Object> value = (BMap<BString, Object>) node.getMapValue(Constant.VALUE);
        Node newNode = new Node(value, null);
        Node currentHead = head.get();
        newNode.next.compareAndSet(null, currentHead);
        head.set(newNode);
        lruEvictionPolicy.addNativeData(Constant.HEAD, head);
    }

    @SuppressWarnings("unchecked")
    public static BMap<BString, Object> externRemoveLast(BObject lruEvictionPolicy) {
        PrintStream asd = System.out;
        asd.println("!!!!!!!!!!!!!!!!!!!!!!");
        head = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.HEAD);
        BMap<BString, Object> value = null;
        Node currentHead = head.get();
        if (currentHead == null) {
            asd.println("``````");
            return null;
        }

        if (currentHead.next.get() == null) {
            asd.println("~~~~~~~~#################");
            value = head.get().value;
            asd.println(value);
            head.set(null);
            lruEvictionPolicy.addNativeData(Constant.HEAD, head);
            return value;
        }

        while (currentHead.next.get().next.get().next.get() != null) {
            asd.println("~~~~~~~~~~~~~~~~~~");
            currentHead = currentHead.next.get();
        }

        value = currentHead.next.get().value;
        asd.println(value);
        currentHead.next.set(null);
        head.set(currentHead);
        lruEvictionPolicy.addNativeData(Constant.HEAD, head);
        return value;
    }

    @SuppressWarnings("unchecked")
    public static void externRemove(BObject lruEvictionPolicy, BMap<BString, Object> value) {
        head = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.HEAD);
        Node currentHead = head.get();
        PrintStream sdf = System.out;
        sdf.println("######################");
        sdf.println(value.get(Constant.VALUE));
        //in case list is empty then return
        if (currentHead != null) {
        if (head.get().value.equals(value.get(Constant.VALUE))) {
                head.set(head.get().next.get());
            } else {
                while (currentHead.next.get().next.get().next.get() != null) {
                    sdf.println("~~~~~~~~~~~~~~~~~~~~~~~~~");
                    sdf.println(currentHead.next.get().value);
                    if (currentHead.next.get().next.get().value != null && currentHead.next.get().next.get().
                            value.equals(value.get(Constant.VALUE))) {
                        sdf.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
                        if (currentHead.next.get().next.get().next.get() != null) {
                            sdf.println("%%%%");
                            currentHead.next.get().next.set(currentHead.next.get().next.get().next.get());
                            sdf.println(currentHead.value);
                            sdf.println(currentHead.next.get().value);
                            break;
                        } else {
                            sdf.println("****");
                            currentHead.next.set(null);
                            sdf.println(currentHead.next.get().value);
                            break;
                        }
                    }
                    currentHead = currentHead.next.get();
                }
            }
        }
        head.set(currentHead);
        lruEvictionPolicy.addNativeData(Constant.HEAD, head);
    }

    public static void externClear(BObject lruEvictionPolicy) {
        head = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.HEAD);
        tail = (AtomicReference<Node>) lruEvictionPolicy.getNativeData(Constant.TAIL);
        head.set(null);
        tail.set(null);
        lruEvictionPolicy.addNativeData(Constant.TAIL, tail);
        lruEvictionPolicy.addNativeData(Constant.HEAD, head);
    }
}
