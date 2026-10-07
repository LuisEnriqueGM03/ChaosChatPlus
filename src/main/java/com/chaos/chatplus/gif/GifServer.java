package com.chaos.chatplus.gif;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.perm.Perms;
import com.chaos.chatplus.server.ChatServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * GIFs en el chat. A los clientes con el mod les llega una línea con una marca (inserción "chaoschatplus:gif:...")
 * y 3 líneas vacías que reservan el hueco donde el cliente dibuja el GIF animado. A los vanilla, un texto con enlace.
 */
public final class GifServer {

	public static final String MARK = "chaoschatplus:gif:";
	/** Líneas que ocupa el GIF en el chat (la de la marca + 3 vacías). */
	public static final int LINES = 4;

	private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

	private GifServer() {}

	public static boolean canUse(ServerPlayer player) {
		return Perms.can(player, ChatConfig.get().gifs.access);
	}

	public static void search(ServerPlayer player, String query) {
		if (!canUse(player)) {
			return;
		}
		List<GifInfo> local = GifRegistry.search(query);
		MinecraftServer server = player.server;
		UUID id = player.getUUID();
		OnlineGifs.search(query).thenAccept(online -> server.execute(() -> {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				return;
			}
			JsonObject o = new JsonObject();
			o.addProperty("query", query);
			o.addProperty("source", OnlineGifs.sourceName());
			o.add("local", array(local));
			o.add("results", array(online));
			ServerPlayNetworking.send(p, new ChatPayloads.GifResults(ChatPayloads.gzip(o.toString())));
		}));
	}

	private static JsonArray array(List<GifInfo> list) {
		JsonArray a = new JsonArray();
		list.forEach(g -> a.add(g.toJson()));
		return a;
	}

	public static void request(ServerPlayer player, String id) {
		byte[] data = GifRegistry.data(id);
		ServerPlayNetworking.send(player, new ChatPayloads.GifData(id, data == null ? new byte[0] : data));
	}

	public static void send(ServerPlayer player, String id) {
		if (!canUse(player)) {
			player.sendSystemMessage(Lang.tr("gif.no_permission").withStyle(ChatFormatting.RED));
			return;
		}
		long cooldown = ChatConfig.get().gifs.cooldownSeconds * 1000L;
		Long last = LAST.get(player.getUUID());
		if (last != null && !player.hasPermissions(2) && System.currentTimeMillis() - last < cooldown) {
			long left = (cooldown - (System.currentTimeMillis() - last) + 999) / 1000;
			player.sendSystemMessage(Lang.tr("cooldown", left).withStyle(ChatFormatting.RED));
			return;
		}
		GifInfo info = id.startsWith(GifInfo.LOCAL) ? GifRegistry.info(id) : OnlineGifs.known(id);
		if (info == null) {
			player.sendSystemMessage(Lang.tr("gif.not_found").withStyle(ChatFormatting.RED));
			return;
		}
		LAST.put(player.getUUID(), System.currentTimeMillis());
		ChaosChatPlus.LOGGER.info("[ChatPlus] {} envió el GIF {} ({})", player.getGameProfile().getName(), info.name(), info.id());
		for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
			viewer.sendSystemMessage(message(viewer, player, info));
		}
	}

	private static Component message(ServerPlayer viewer, ServerPlayer sender, GifInfo info) {
		if (ChatServer.isModded(viewer)) {
			MutableComponent m = Component.empty().append(Lang.tr("gif.sent", sender.getDisplayName()).withStyle(ChatFormatting.GRAY));
			m.append("\n").append(Component.literal(" ").withStyle(s -> s.withInsertion(MARK + info.encode())));
			for (int i = 1; i < LINES; i++) {
				m.append("\n ");
			}
			return m;
		}
		MutableComponent name = Component.literal(info.name()).withStyle(ChatFormatting.AQUA);
		if (!info.local() && !info.page().isEmpty()) {
			name = name.withStyle(s -> s.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, info.page())));
		}
		return Lang.tr("gif.sent_vanilla", sender.getDisplayName(), name).withStyle(ChatFormatting.GRAY);
	}

	public static void forget(ServerPlayer player) {
		LAST.remove(player.getUUID());
	}
}
