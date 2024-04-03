package com.finbourne;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Normally, we'd have two type arguments here for Key and Values. But since the
 * assignment explicitly states that we can have arbitrary types of key/value pairs AND
 * that this should be a singleton, we are just going to make it Object/Object.
 * We COULD, in theory, include a factory method that takes two Class arguments and getOrCreate's
 * an instance of LRUCache<K, V> with that specific type pair, but then it gets messy when it comes to subtypes
 * and overly heavy on the syntax.
 * Since this is just a test assignment, we are going to keep it simple and stick to Object/Object. But
 * we are going to manually craft the double-linked support list to give the exercise a bit of depth and for
 * optimization. Head is the LRU item, and Tail is the next to evict.
 *
 * Additionally, we are going to mark the methods as "synchronized" to obtain thread safety.
 *
 * Finally, we are going to make it implement the Map interface to see what methods are necessary to implement.
 */
public class LRUCache implements Map<Object, Object> {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LRUCache lruCache = (LRUCache) o;
        return capacity == lruCache.capacity && Objects.equals(map, lruCache.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, capacity);
    }

    private class Node {
        Object key;
        Object value;

        Node prev;

        Node next;

        EvictionCallback evictionCallback = null;

        Node(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        // No need to make these depend on prev/next pointers semantically.
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(key, node.key) && Objects.equals(value, node.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }

    private Map<Object, Node> map;

    //The default value here should be chosen appropriately.
    private int capacity = 99;
    private Node head = null;
    private Node tail = null;

    private synchronized Node removeTail() {
        if (tail != null) {
            Node toRemove = tail;
            if (toRemove.evictionCallback != null) {
                toRemove.evictionCallback.apply(toRemove.key, toRemove.value);
            }
            map.remove(toRemove.key);
            Node prev = toRemove.prev;
            if (prev != null) {
                prev.next = null;
                tail = prev;
            }
            else {
                tail = null;
                head = null;
            }
            return toRemove;
        }
        else {
            return null;
        }
    }

    private synchronized void moveToTail(Node n) {
        if (n != tail){
            if (n.prev != null) {
                n.prev.next = n.next;
            }
            if (n.next != null) {
                n.next.prev = n.prev;
                if (n.next.prev == null) {
                    head = n.next;
                }
            }
            if (tail != null) {
                tail.next = n;
                n.prev = tail;
            }
            tail = n;
        }
    }

    private synchronized void moveToHead(Node n) {
        if (n != head) {
            if (n.prev != null) {
                n.prev.next = n.next;
                if (n.prev.next == null) {
                    tail = n.prev;
                }
            }
            if (n.next != null) {
                n.next.prev = n.prev;
            }
            if (head != null) {
                head.prev = n;
                n.next = head;
            }
            head = n;
        }
    }


    private LRUCache() {
        this.map = new HashMap<>();
    }

    private static LRUCache instance = null;

    public static synchronized LRUCache getInstance() {
        if (LRUCache.instance == null) {
            LRUCache.instance = new LRUCache();
        }
        return LRUCache.instance;
    }


    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return map.values().stream().anyMatch(node -> node.value.equals(value));
    }

    /**
     * Getting an item means updating its position in the LRU list
     */
    @Override
    public synchronized Object get(Object key) {
        if (map.containsKey(key)) {
            Node n = map.get(key);
            moveToHead(n);
            return n.value;
        }
        else {
            return null;
        }
    }

    /**
     * Putting a k/v pair means adding it to the map, as well as fixing the pointers of the LRU list.
     * Can also register the eviction callback.
     */
    @Override
    public synchronized Object put(Object key, Object value) {
        return put(key, value, null);
    }

    public synchronized Object put(Object key, Object value, EvictionCallback ec) {
        Object previousValue = get(key);

        Node n = new Node(key, value);
        n.evictionCallback = ec;
        moveToHead(n);
        if (isEmpty()) {
            // If first item it's both head and tail
            moveToTail(n);
        }
        map.put(key, n);

        while(size() > capacity) {
            removeTail();
        }

        return previousValue;
    }

    @Override
    public synchronized Object remove(Object key) {
        if (map.containsKey(key)) {
            Node n = map.remove(key);
            moveToTail(n);
            removeTail();
            return n.value;
        }
        else {
           return null;
        }
    }

    @Override
    public synchronized void putAll(Map<?, ?> m) {
        for(Entry<?, ?> e: m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public synchronized void clear() {
        map.clear();
        tail = null;
        head = null;
    }

    @Override
    public synchronized Set<Object> keySet() {
        return map.keySet();
    }

    @Override
    public synchronized Collection<Object> values() {
        return map.values().stream().map(node -> node.value).toList();
    }

    @Override
    public synchronized Set<Entry<Object, Object>> entrySet() {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue().value)).entrySet();
    }

    /**
     * Can change the capacity at runtime and return what is evicted.
     */
    public synchronized Collection<Object> resize(int newCapacity) {
        if (newCapacity < 0 ){
            throw new IllegalArgumentException("Cache size can not be negative ("+newCapacity+")");
        }
        else {
            capacity = newCapacity;
            List<Object> removed = new LinkedList<>();
            while(size() > capacity) {
                removed.add(removeTail().value);
            }
            return removed;
        }
    }
}
