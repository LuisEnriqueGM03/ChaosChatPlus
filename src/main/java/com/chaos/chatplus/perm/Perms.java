package com.chaos.chatplus.perm;

import java.util.List;
import java.util.Locale;

import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.config.ChatConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

/** Único punto para decidir si un jugador puede usar algo: nivel de OP o, con Chaos Ranks, su rango. */
public final class Perms {

	private Perms() {}

	public static boolean can(ServerPlayer player, ChatConfig.Access access) {
		if (access == null || !access.enabled) {
			return false;
		}
		if (player.hasPermissions(access.opLevel)) {
			return true;
		}
		return inRanks(player, access.allowedRanks);
	}

	/** Lista vacía = cualquiera. */
	public static boolean inRanksOrAll(ServerPlayer player, List<String> ranks) {
		return ranks == null || ranks.isEmpty() || inRanks(player, ranks);
	}

	private static boolean inRanks(ServerPlayer player, List<String> ranks) {
		if (ranks == null || ranks.isEmpty() || !ChaosRanksCompat.present()) {
			return false;
		}
		String id = ChaosRanksCompat.rankId(player);
		return id != null && ranks.contains(id.toLowerCase(Locale.ROOT));
	}

	public static boolean canItems(ServerPlayer player) {
		return can(player, ChatConfig.get().items.access);
	}

	public static boolean canShots(ServerPlayer player) {
		ChatConfig.Screenshots cfg = ChatConfig.get().screenshots;
		if (!can(player, cfg.access)) {
			return false;
		}
		return cfg.minPlayMinutes <= 0 || player.hasPermissions(2) || playMinutes(player) >= cfg.minPlayMinutes;
	}

	public static boolean canUpload(ServerPlayer player) {
		return ChatConfig.get().emojis.enabled && can(player, ChatConfig.get().emojis.upload);
	}

	public static boolean canMentionEveryone(ServerPlayer player) {
		return ChatConfig.get().mentions.enabled && can(player, ChatConfig.get().mentions.everyone);
	}

	public static boolean canMentionRanks(ServerPlayer player) {
		return ChatConfig.get().mentions.enabled && ChaosRanksCompat.present() && can(player, ChatConfig.get().mentions.ranks);
	}

	public static int playMinutes(ServerPlayer player) {
		int ticks = player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME);
		return ticks / 20 / 60;
	}
}
