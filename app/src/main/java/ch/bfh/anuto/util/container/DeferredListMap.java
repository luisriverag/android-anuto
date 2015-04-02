package ch.bfh.anuto.util.container;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ch.bfh.anuto.util.iterator.ComputingIterator;
import ch.bfh.anuto.util.iterator.StreamIterator;

public class DeferredListMap<K, V> {

    /*
    ------ Entry Class ------
     */

    private static class Entry<K, V> {
        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K key;
        public V value;
    }

    /*
    ------ Iterators ------
     */

    private abstract class ListMapIterator extends ComputingIterator<V> {
        public ListMapIterator() {
            mLock.readLock().lock();
        }

        @Override
        public void close() {
            mLock.readLock().unlock();
        }
    }

    private class ListMapAllIterator extends ListMapIterator {
        Iterator<List<V>> mListIterator;
        Iterator<V> mObjectIterator;

        public ListMapAllIterator() {
            mListIterator = mListMap.values().iterator();

            if (mListIterator.hasNext()) {
                mObjectIterator = mListIterator.next().iterator();
            }
        }

        @Override
        public V computeNext() {
            if (mObjectIterator == null) {
                return null;
            }

            synchronized (mObjectsToRemove) {
                while (true) {
                    while (!mObjectIterator.hasNext()) {
                        if (mListIterator.hasNext()) {
                            mObjectIterator = mListIterator.next().iterator();
                        } else {
                            return null;
                        }
                    }

                    V next = mObjectIterator.next();

                    if (!mObjectsToRemove.contains(next)) {
                        return next;
                    }
                }
            }
        }
    }

    private class ListMapKeyIterator extends ListMapIterator {
        Iterator<V> mObjectIterator;

        public ListMapKeyIterator(K key) {
            mObjectIterator = getList(key).iterator();
        }

        @Override
        public V computeNext() {
            synchronized (mObjectsToRemove) {
                while (true) {
                    if (!mObjectIterator.hasNext()) {
                        return null;
                    }

                    V next = mObjectIterator.next();

                    if (!mObjectsToRemove.contains(next)) {
                        return next;
                    }
                }
            }
        }
    }

    /*
    ------ Members ------
     */

    private final ReadWriteLock mLock = new ReentrantReadWriteLock();

    private final SortedMap<K, List<V>> mListMap = new TreeMap<>();

    private final Queue<Entry<K, V>> mObjectsToAdd = new ArrayDeque<>();
    private final Queue<Entry<K, V>> mObjectsToRemove = new ArrayDeque<>();

    /*
    ------ Methods ------
     */

    private List<V> getList(K key) {
        if (!mListMap.containsKey(key)) {
            mListMap.put(key, new ArrayList<V>());
        }

        return mListMap.get(key);
    }

    public void addDeferred(K key, V value) {
        synchronized (mObjectsToAdd) {
            mObjectsToAdd.add(new Entry<>(key, value));
        }
    }

    public void removeDeferred(K key, V value) {
        synchronized (mObjectsToRemove) {
            mObjectsToRemove.add(new Entry<>(key, value));
        }
    }

    public void applyChanges() {
        mLock.writeLock().lock();

        synchronized (mObjectsToAdd) {
            while (!mObjectsToAdd.isEmpty()) {
                Entry<K, V> e = mObjectsToAdd.remove();
                getList(e.key).add(e.value);
                onItemAdded(e.key, e.value);
            }
        }

        synchronized (mObjectsToRemove) {
            while (!mObjectsToRemove.isEmpty()) {
                Entry<K, V> e = mObjectsToRemove.remove();

                if (getList(e.key).remove(e.value)) {
                    onItemRemoved(e.key, e.value);
                }
            }
        }

        mLock.writeLock().unlock();
    }

    public StreamIterator<V> getAll() {
        return new ListMapAllIterator();
    }

    public StreamIterator<V> getByKey(K key) {
        return new ListMapKeyIterator(key);
    }

    protected void onItemAdded(K key, V value) {

    }

    protected void onItemRemoved(K key, V value) {

    }
}
