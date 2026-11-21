package com.nexusrpg.achievements;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Bounded, identity-keyed short-lived queue; never queries or loads a world. */
final class PendingDeaths<K, V> {
    static final int LIMIT = 512;
    static final long WAIT_TICKS = 2;
    private static final class Entry<V> {
        final V value;
        final long added;
        boolean confirmed;
        Entry(V value, long added) { this.value = value; this.added = added; }
    }
    private final Map<K, Entry<V>> entries = new IdentityHashMap<K, Entry<V>>();
    void offer(K key, V value, long now) {
        Entry<V> existing = entries.get(key);
        // Do not overwrite an already confirmed death with a later event.
        if (existing != null && existing.confirmed) return;
        if (existing == null && entries.size() >= LIMIT) return;
        entries.put(key, new Entry<V>(value, now));
    }
    V get(K key) { Entry<V> e = entries.get(key); return e == null ? null : e.value; }
    void confirm(K key) { Entry<V> e = entries.get(key); if (e != null) e.confirmed = true; }
    List<V> drain(long now) {
        List<V> result = new ArrayList<V>();
        Iterator<Entry<V>> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry<V> e = it.next();
            // Also discard on tick-counter reset rather than retaining stale references.
            if (now < e.added || now - e.added >= WAIT_TICKS) {
                if (now >= e.added && e.confirmed) result.add(e.value);
                it.remove();
            }
        }
        return result;
    }
    int size() { return entries.size(); }
}
