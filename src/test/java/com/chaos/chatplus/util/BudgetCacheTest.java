package com.chaos.chatplus.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BudgetCacheTest {
	@Test void evictsLeastRecentlyUsedAndDisposesResources() {
		List<Integer> disposed = new ArrayList<>(); var cache = new BudgetCache<String,Integer>(2, 100, n -> n, disposed::add);
		cache.put("a", 1); cache.put("b", 2); cache.get("a"); cache.put("c", 3);
		assertNull(cache.get("b")); assertEquals(List.of(2), disposed);
		cache.clear(); assertEquals(0, cache.size()); assertEquals(3, disposed.size());
	}
	@Test void enforcesBytesAndRejectsOversizedEntry() {
		var cache = new BudgetCache<String,Integer>(10, 5, n -> n, n -> {});
		cache.put("a", 3); cache.put("b", 3); assertNull(cache.get("a"));
		cache.put("huge", 6); assertEquals(0, cache.size());
	}
}
