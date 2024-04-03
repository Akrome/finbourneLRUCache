package com.finbourne;

/**
 * This is the signature of the handler called when the item gets evicted
 */
@FunctionalInterface
public interface EvictionCallback {
    void apply(Object key, Object value);
}
