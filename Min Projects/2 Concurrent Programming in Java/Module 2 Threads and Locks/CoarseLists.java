package edu.coursera.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Wrapper class for two lock-based concurrent list implementations.
 * CoarseList typically refers to a data structure used in concurrent programming, specifically a type of linked list
 * that allows multiple threads to access it simultaneously while maintaining thread safety.
 * Coarse-Grained Locking means using a single lock (a mutex or monitor) to control access to the entire data structure.
 * In the context of a CoarseList, this means that any operation (such as insertion, deletion, or traversal) on the list will lock the entire list,
 * preventing other threads from accessing it until the operation is complete.
 */
public final class CoarseLists {
    /**
     * An implementation of the ListSet interface that uses Java locks to
     * protect against concurrent accesses.
     * TODO Implement the add, remove, and contains methods below to support
     * correct, concurrent access to this list. Use a Java ReentrantLock object
     * to protect against those concurrent accesses. You may refer to
     * SyncList.java for help understanding the list management logic, and for
     * guidance in understanding where to place lock-based synchronization.
     */
    public static final class CoarseList extends ListSet {

        private final Lock lock = new ReentrantLock();

        /**
         * Default constructor.
         */
        public CoarseList() {
            super();
        }

        @Override
        boolean add(final Integer object) {
            lock.lock();
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }

                if (object.equals(curr.object)) {
                    return false;
                } else {
                    final Entry entry = new Entry(object);
                    entry.next = curr;
                    pred.next = entry;
                    return true;
                }
            } finally {
                lock.unlock(); // Ensure the lock is released
            }
        }

        @Override
        boolean remove(final Integer object) {
            lock.lock();
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }

                if (object.equals(curr.object)) {
                    pred.next = curr.next;
                    return true;
                } else {
                    return false;
                }
            } finally {
                lock.unlock(); // Ensure the lock is released
            }
        }

        @Override
        boolean contains(final Integer object) {
            lock.lock();
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }
                return object.equals(curr.object);
            } finally {
                lock.unlock(); // Ensure the lock is released
            }
        }
    }

    /**
     * An implementation of the ListSet interface that uses Java read-write
     * locks to protect against concurrent accesses.
     * TODO Implement the add, remove, and contains methods below to support
     * correct, concurrent access to this list. Use a Java
     * ReentrantReadWriteLock object to protect against those concurrent
     * accesses. You may refer to SyncList.java for help understanding the list
     * management logic, and for guidance in understanding where to place
     * lock-based synchronization.
     */
    public static final class RWCoarseList extends ListSet {

        /**
         Read Lock: Allows multiple threads to acquire the read lock simultaneously, as long as no thread holds the write lock.
         Write Lock: Allows only one thread to acquire the write lock at a time (no other write or read), and provides exclusive access to the resource.
         It will have better performance than ReentrantLock if read operations are intensive.
         */
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        /**
         * Default constructor.
         */
        public RWCoarseList() {
            super();
        }

        /**
         * {@inheritDoc}
         *
         * TODO Use a read-write lock to protect against concurrent access.
         */
        @Override
        boolean add(final Integer object) {
            rwLock.writeLock().lock(); // Acquire write lock
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }

                if (object.equals(curr.object)) {
                    return false;
                } else {
                    final Entry entry = new Entry(object);
                    entry.next = curr;
                    pred.next = entry;
                    return true;
                }
            } finally {
                rwLock.writeLock().unlock(); // Release write lock
            }
        }

        @Override
        boolean remove(final Integer object) {
            rwLock.writeLock().lock(); // Acquire write lock
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }

                if (object.equals(curr.object)) {
                    pred.next = curr.next;
                    return true;
                } else {
                    return false;
                }
            } finally {
                rwLock.writeLock().unlock(); // Release write lock
            }
        }

        @Override
        boolean contains(final Integer object) {
            rwLock.readLock().lock(); // Acquire read lock
            try
            {
                Entry pred = this.head;
                Entry curr = pred.next;

                while (curr.object.compareTo(object) < 0) {
                    pred = curr;
                    curr = curr.next;
                }
                return object.equals(curr.object);
            } finally {
                rwLock.readLock().unlock(); // Release read lock
            }
        }
    }
}
