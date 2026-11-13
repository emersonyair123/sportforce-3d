package com.sportforce3d.render;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToLongFunction;

final class LruCache<K, V> {

    private final long maxWeight;
    private final ToLongFunction<V> weigher;
    private final LinkedHashMap<K, V> map = new LinkedHashMap<>(64, 0.75f, true);
    private long weight;

    LruCache(long maxWeight, ToLongFunction<V> weigher) {
        this.maxWeight = maxWeight;
        this.weigher = weigher;
    }

    synchronized V get(K key) {
        return map.get(key);
    }

    synchronized void put(K key, V value) {
        V old = map.put(key, value);
        if (old != null) {
            weight -= weigher.applyAsLong(old);
        }
        weight += weigher.applyAsLong(value);
        Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
        while (weight > maxWeight && map.size() > 1 && it.hasNext()) {
            Map.Entry<K, V> eldest = it.next();
            if (eldest.getKey().equals(key)) {
                continue;
            }
            weight -= weigher.applyAsLong(eldest.getValue());
            it.remove();
        }
    }

    synchronized int size() {
        return map.size();
    }

    synchronized long weight() {
        return weight;
    }

    synchronized boolean containsKey(K key) {
        return map.containsKey(key);
    }
}
