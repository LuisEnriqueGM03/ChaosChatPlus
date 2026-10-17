package com.chaos.chatplus.emoji;

import org.junit.jupiter.api.Test;
import com.chaos.chatplus.config.ChatConfig;
import static org.junit.jupiter.api.Assertions.*;

class UploadLimitsTest {
	@Test void pendingEntriesReserveCustomCapacity() {
		var cfg = new ChatConfig.Emojis(); cfg.maxCustom = 10;
		assertFalse(UploadLimits.hasCapacity(8, 2, 0, true, cfg));
		assertTrue(UploadLimits.hasCapacity(8, 1, 0, true, cfg));
	}
	@Test void pendingQueueHasGlobalAndPerPlayerLimits() {
		var cfg = new ChatConfig.Emojis(); cfg.maxPending = 4; cfg.maxPendingPerPlayer = 2;
		assertFalse(UploadLimits.hasCapacity(0, 4, 0, false, cfg));
		assertFalse(UploadLimits.hasCapacity(0, 2, 2, false, cfg));
		assertTrue(UploadLimits.hasCapacity(0, 2, 1, false, cfg));
	}
}
