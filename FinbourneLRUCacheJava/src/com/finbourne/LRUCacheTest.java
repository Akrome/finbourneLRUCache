package com.finbourne;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {
    LRUCache cache = LRUCache.getInstance();

    @BeforeEach
    void beforeEach() {
        cache.clear();
        cache.resize(99);
    }

    @Test
    void isSingleton() {
        assertSame(cache, LRUCache.getInstance());
    }

    @Test
    void basicPutAndGet() {
        cache.put("x", "xx");
        cache.put(true, "false");
        assertEquals(cache.size(), 2);
        assertEquals(cache.get("x"), "xx");
        assertEquals(cache.get(true), "false");
    }

    @Test
    void basicPutAndRemove() {
        cache.put("x", "xx");
        cache.put(true, "false");
        assertEquals(cache.size(), 2);
        assertEquals(cache.remove(true), "false");
        assertNull(cache.remove(99));
        assertEquals(cache.size(), 1);
        assertNull(cache.get(true));
    }

    @Test
    void maxCapacity() {
        cache.resize(1);
        cache.put(1, 11);
        cache.put(2, 22);
        assertEquals(cache.size(), 1);
        assertNull(cache.get(1));
        assertEquals(cache.get(2), 22);
    }

    @Test
    void getShouldRefreshLRU() {
        cache.put(1, 11);
        cache.put(2, 22);
        cache.get(1);
        cache.resize(1);
        assertEquals(cache.size(), 1);
        assertNull(cache.get(2));
        assertEquals(cache.get(1), 11);
    }

    @Test
    void illegalCapacity() {
        assertThrows(IllegalArgumentException.class, () -> cache.resize(-2));
    }

    @Test
    void callBackOnEviction() {
        final int[] x = {0};
        cache.put(1, 11, new EvictionCallback() {
            @Override
            public void apply(Object key, Object value) {
                x[0] = (int) value;
            }
        });
        cache.remove(1);
        assertEquals(x[0], 11);
    }

    @Test
    void resizeReturnsListOfEvicted() {
        cache.put(1, 11);
        cache.put(2, 22);
        cache.put(3, 33);
        List<Object> expected = new LinkedList<>();
        expected.add(11);
        expected.add(22);
        assertEquals(expected, cache.resize(1));
    }
}