package com.chaos.chatplus.perm;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AccessRulesTest {
	@Test void disabledFeatureCannotBeBypassedByOpOrRank() {
		assertFalse(AccessRules.allowed(false, 4, 2, "vip", List.of("vip")));
	}
	@Test void opOrAllowedRankGrantsAccess() {
		assertTrue(AccessRules.allowed(true, 2, 2, null, List.of()));
		assertTrue(AccessRules.allowed(true, 0, 2, "VIP", List.of("vip")));
		assertFalse(AccessRules.allowed(true, 0, 2, "guest", List.of("vip")));
		assertFalse(AccessRules.allowed(true, 0, 2, null, List.of()));
		assertTrue(AccessRules.allowed(true, 0, 0, null, List.of()));
	}
}
