package com.chaos.chatplus.util;

import java.util.*;
import java.util.function.*;

/** Access-ordered cache with both an entry count and byte budget. Owned by the caller's thread. */
public final class BudgetCache<K, V> {
	private final LinkedHashMap<K, V> values = new LinkedHashMap<>(16, 0.75F, true);
	private final int maxEntries;
	private final long maxBytes;
	private final ToLongFunction<V> weight;
	private final Consumer<V> dispose;
	public BudgetCache(int maxEntries, long maxBytes, ToLongFunction<V> weight, Consumer<V> dispose) {
		this.maxEntries = maxEntries; this.maxBytes = maxBytes; this.weight = weight; this.dispose = dispose;
	}
	public V get(K key) { return values.get(key); }
	public boolean containsValue(V value) { return values.containsValue(value); }
	public void put(K key, V value) {
		V previous = values.put(key, value);
		if (previous != null && previous != value) dispose.accept(previous);
		trim();
	}
	public V remove(K key) {
		V value = values.remove(key);
		if (value != null) dispose.accept(value);
		return value;
	}
	public void trim() {
		long bytes = values.values().stream().mapToLong(weight).sum();
		var it = values.entrySet().iterator();
		while (it.hasNext() && (values.size() > maxEntries || bytes > maxBytes)) {
			V value = it.next().getValue();
			bytes -= weight.applyAsLong(value);
			it.remove(); dispose.accept(value);
		}
	}
	public int size() { return values.size(); }
	public void clear() {
		var old = List.copyOf(values.values()); values.clear(); old.forEach(dispose);
	}
}
