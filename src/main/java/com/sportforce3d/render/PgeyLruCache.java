package com.sportforce3d.render;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToLongFunction;

final class PgeyLruCache<K, V> {

    private final long pgeyMaxWeight;
    private final ToLongFunction<V> pgeyWeigher;
    private final LinkedHashMap<K, V> pgeyMap = new LinkedHashMap<>(64, 0.75f, true);
    private long pgeyWeight;

    PgeyLruCache(long pgeyMaxWeight, ToLongFunction<V> pgeyWeigher) {
        this.pgeyMaxWeight = pgeyMaxWeight;
        this.pgeyWeigher = pgeyWeigher;
    }

    synchronized V pgeyGet(K pgeyKey) {
        return pgeyMap.get(pgeyKey);
    }

    synchronized void pgeyPut(K pgeyKey, V pgeyValue) {
        V pgeyOld = pgeyMap.put(pgeyKey, pgeyValue);
        if (pgeyOld != null) {
            pgeyWeight -= pgeyWeigher.applyAsLong(pgeyOld);
        }
        pgeyWeight += pgeyWeigher.applyAsLong(pgeyValue);
        Iterator<Map.Entry<K, V>> pgeyIt = pgeyMap.entrySet().iterator();
        while (pgeyWeight > pgeyMaxWeight && pgeyMap.size() > 1 && pgeyIt.hasNext()) {
            Map.Entry<K, V> pgeyEldest = pgeyIt.next();
            if (pgeyEldest.getKey().equals(pgeyKey)) {
                continue;
            }
            pgeyWeight -= pgeyWeigher.applyAsLong(pgeyEldest.getValue());
            pgeyIt.remove();
        }
    }

    synchronized int pgeySize() {
        return pgeyMap.size();
    }

    synchronized long pgeyWeight() {
        return pgeyWeight;
    }

    synchronized boolean pgeyContainsKey(K pgeyKey) {
        return pgeyMap.containsKey(pgeyKey);
    }
}
