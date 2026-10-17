package com.chaos.chatplus.server;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ShotAccessTest {
	final UUID owner = UUID.randomUUID(), target = UUID.randomUUID(), outsider = UUID.randomUUID();
	@Test void privateImageIsRestrictedToParticipantsAndModerators() {
		assertTrue(ShotAccess.canView(owner, target, owner, false));
		assertTrue(ShotAccess.canView(owner, target, target, false));
		assertFalse(ShotAccess.canView(owner, target, outsider, false));
		assertTrue(ShotAccess.canView(owner, target, outsider, true));
	}
	@Test void outsiderCannotReportPrivateImageAndOwnerCannotSelfReport() {
		assertFalse(ShotAccess.canReport(owner, target, outsider, false));
		assertFalse(ShotAccess.canReport(owner, target, owner, true));
		assertTrue(ShotAccess.canReport(owner, target, target, false));
		assertTrue(ShotAccess.canReport(owner, null, outsider, false));
	}
}
