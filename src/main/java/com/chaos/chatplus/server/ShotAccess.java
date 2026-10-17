package com.chaos.chatplus.server;

import java.util.UUID;

/** Shared by viewing and reporting, so knowing a private image id grants no access. */
public final class ShotAccess {
	private ShotAccess() {}
	public static boolean canView(UUID owner, UUID target, UUID viewer, boolean moderator) {
		return moderator || target == null || owner.equals(viewer) || target.equals(viewer);
	}
	public static boolean canReport(UUID owner, UUID target, UUID viewer, boolean moderator) {
		return !owner.equals(viewer) && canView(owner, target, viewer, moderator);
	}
}
