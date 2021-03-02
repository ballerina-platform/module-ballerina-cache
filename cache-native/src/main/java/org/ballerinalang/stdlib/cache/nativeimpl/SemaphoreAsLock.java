package org.ballerinalang.stdlib.cache.nativeimpl;

import java.util.concurrent.Semaphore;

/**
 * Class to handle concurrency behaviour of `LinkedList` using compare and swap technique.
 */
public class SemaphoreAsLock {

    private static Semaphore semaphore = null;


    public static void init() {
        semaphore = new Semaphore(1);
    }

    public static void acquire() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            //
        }
    }

    public static void release() {
        semaphore.release();
    }
}
