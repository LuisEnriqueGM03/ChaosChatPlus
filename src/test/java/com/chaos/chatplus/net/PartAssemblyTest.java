package com.chaos.chatplus.net;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartAssemblyTest {
	@Test void reassemblesOutOfOrderAndRefusesIncompleteTransfer() {
		var parts = new PartAssembly(2, 2, 4, 6);
		parts.accept(1, 2, new byte[]{3,4});
		assertFalse(parts.complete()); assertThrows(IllegalStateException.class, parts::join);
		parts.accept(0, 2, new byte[]{1,2}); assertArrayEquals(new byte[]{1,2,3,4}, parts.join());
	}
	@Test void rejectsExcessiveCountsBeforeAllocation() {
		assertThrows(IllegalArgumentException.class, () -> new PartAssembly(Integer.MAX_VALUE, 16, 4, 64));
		assertThrows(IllegalArgumentException.class, () -> new PartAssembly(0, 16, 4, 64));
	}
	@Test void rejectsDuplicatesInconsistentCountsAndIndices() {
		var parts = new PartAssembly(2, 2, 4, 8); parts.accept(0, 2, new byte[]{1});
		assertThrows(IllegalArgumentException.class, () -> parts.accept(0, 2, new byte[]{1}));
		assertThrows(IllegalArgumentException.class, () -> parts.accept(1, 3, new byte[]{1}));
		assertThrows(IllegalArgumentException.class, () -> parts.accept(-1, 2, new byte[]{1}));
	}
	@Test void rejectsPartAndAggregateOverflow() {
		var parts = new PartAssembly(2, 2, 4, 5); parts.accept(0, 2, new byte[]{1,2,3,4});
		assertThrows(IllegalArgumentException.class, () -> parts.accept(1, 2, new byte[]{5,6}));
		assertThrows(IllegalArgumentException.class, () -> parts.accept(1, 2, new byte[5]));
	}
}
