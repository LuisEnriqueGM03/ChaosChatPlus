package com.chaos.chatplus.perm;

import java.util.List;
import java.util.Locale;

public final class AccessRules {
	private AccessRules() {}
	public static boolean allowed(boolean enabled, int playerOp, int requiredOp, String rank, List<String> ranks) {
		return enabled && (playerOp >= requiredOp || (rank != null && ranks != null && ranks.contains(rank.toLowerCase(Locale.ROOT))));
	}
}
