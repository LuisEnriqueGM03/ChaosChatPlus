package com.chaos.chatplus.emoji;

import com.chaos.chatplus.config.ChatConfig;

public final class UploadLimits {
	private UploadLimits() {}
	public static boolean hasCapacity(int approvedCount, int pendingCount, long playerPending,
			boolean approved, ChatConfig.Emojis cfg) {
		return approvedCount + pendingCount < cfg.maxCustom && (approved ||
				(pendingCount < cfg.maxPending && playerPending < cfg.maxPendingPerPlayer));
	}
}
