package com.chaos.chatplus.server;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.gif.GifServer;
import com.chaos.chatplus.gif.OnlineGifs;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.pack.FontSync;
import com.chaos.chatplus.perm.Perms;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.chaos.chatplus.platform.ServerNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Estado por jugador con el mod: lo que puede usar, su prefijo de rango y los paquetes que manda. */
public final class ChatServer {

	/** Lo último que se le mandó a cada jugador, para no repetir. */
	private record Sent(String permKey, int emojiVersion, String prefixKey) {}

	private static final Map<UUID, Sent> SENT = new ConcurrentHashMap<>();
	private static int ticks;
	private static final Map<UUID, Long> LAST_UPLOAD = new ConcurrentHashMap<>();

	private ChatServer() {}

	public static void registerReceivers() {
		ServerNetwork.registerGlobalReceiver(ChatPayloads.ShareItem.TYPE, (payload, context) ->
				context.server().execute(() -> ItemShare.shareSlot(context.player(), payload.slot(), payload.target())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.ShotPart.TYPE, (payload, context) ->
				context.server().execute(() -> Shots.onPart(context.player(), payload)));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.RequestShot.TYPE, (payload, context) ->
				context.server().execute(() -> Shots.send(context.player(), payload.id())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.ReportShot.TYPE, (payload, context) ->
				context.server().execute(() -> Shots.report(context.player(), payload.id())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.GifSearch.TYPE, (payload, context) ->
				context.server().execute(() -> GifServer.search(context.player(), payload.query())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.GifRequest.TYPE, (payload, context) ->
				context.server().execute(() -> GifServer.request(context.player(), payload.id())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.SendGif.TYPE, (payload, context) ->
				context.server().execute(() -> GifServer.send(context.player(), payload.id(), payload.info(), payload.target())));
		ServerNetwork.registerGlobalReceiver(ChatPayloads.UploadEmoji.TYPE, (payload, context) ->
				context.server().execute(() -> upload(context.player(), payload)));
	}

	/** ¿Tiene el cliente Chaos ChatPlus? */
	public static boolean isModded(ServerPlayer player) {
		return ServerNetwork.canSend(player, ChatPayloads.ServerInfo.TYPE);
	}

	public static void join(ServerPlayer player) {
		SENT.remove(player.getUUID());
		if (isModded(player)) {
			FontSync.send(player);
			sync(player, true);
		}
	}

	public static void leave(ServerPlayer player) {
		SENT.remove(player.getUUID());
		ItemShare.forget(player);
		Shots.forget(player);
		GifServer.forget(player);
	}

	public static void stop() {
		SENT.clear();
		ResourceReload.stop();
		LAST_UPLOAD.clear();
		Shots.clear();
	}

	public static void tick(MinecraftServer server) {
		if (++ticks % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isModded(player)) {
				sync(player, false);
			}
		}
		Shots.tick(server);
		// Las carpetas mandan: si se añadió, cambió o borró un emoji o GIF, se recarga solo.
		if (ticks % 100 == 0) ResourceReload.poll(server);
	}

	/** Tras recargar o cambiar emojis: vuelve a mandar todo y las fuentes. */
	public static void resyncAll(MinecraftServer server) {
		FontSync.rebuild();
		resyncPrepared(server);
	}

	public static void resyncPrepared(MinecraftServer server) {
		SENT.clear();
		FontSync.sendAll(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (isModded(player)) {
				sync(player, true);
			}
		}
	}

	/** Manda lo que haya cambiado: permisos/emojis (ServerInfo + imágenes) y el prefijo de rango. */
	private static void sync(ServerPlayer player, boolean force) {
		Sent last = force ? null : SENT.get(player.getUUID());
		String permKey = permKey(player);
		int version = EmojiRegistry.version();
		Component prefix = prefix(player);
		String prefixKey = prefix == null ? "" : prefix.toString();
		if (last == null || !last.permKey().equals(permKey) || last.emojiVersion() != version) {
			ServerNetwork.send(player, new ChatPayloads.ServerInfo(ChatPayloads.gzip(info(player).toString())));
			if (last == null || last.emojiVersion() != version) {
				sendImages(player);
			}
		}
		if (last == null || !last.prefixKey().equals(prefixKey)) {
			ServerNetwork.send(player, new ChatPayloads.SelfPrefix(Optional.ofNullable(prefix),
					Optional.ofNullable(prefixFallback(player))));
		}
		SENT.put(player.getUUID(), new Sent(permKey, version, prefixKey));
	}

	/** Lo que decide los permisos de un jugador (si cambia, se vuelve a mandar ServerInfo). */
	private static String permKey(ServerPlayer player) {
		return player.server.getProfilePermissions(player.getGameProfile()) + "|" + Objects.toString(ChaosRanksCompat.rankId(player));
	}

	private static JsonObject info(ServerPlayer player) {
		ChatConfig cfg = ChatConfig.get();
		JsonObject o = new JsonObject();
		o.addProperty("items", Perms.canItems(player));
		o.addProperty("shots", Perms.canShots(player));
		o.addProperty("upload", Perms.canUpload(player));
		o.addProperty("mentions", cfg.mentions.enabled);
		o.addProperty("mentionColor", cfg.mentionRgb());
		o.addProperty("blurShots", cfg.screenshots.blurUntilClick);
		o.addProperty("maxShotKb", cfg.screenshots.maxKilobytes);
		o.addProperty("pack", true);
		o.addProperty("gifs", GifServer.canUse(player));
		o.addProperty("gifSource", OnlineGifs.sourceName());
		// La clave de Klipy va al juego del jugador: Klipy pide que las búsquedas salgan del cliente.
		o.addProperty("klipyKey", GifServer.canUse(player) ? cfg.gifs.klipyKey : "");
		JsonArray special = new JsonArray();
		ChatDecorator.specialMentions(player).forEach(special::add);
		if (Perms.canMentionRanks(player)) {
			ChaosRanksCompat.ranks().forEach(r -> special.add(r.id()));
		}
		o.add("specialMentions", special);
		JsonArray emojis = new JsonArray();
		if (cfg.emojis.enabled) {
			for (Emoji e : EmojiRegistry.all()) {
				if (!Perms.inRanksOrAll(player, e.ranks())) {
					continue;
				}
				JsonObject j = new JsonObject();
				j.addProperty("n", e.name());
				JsonArray a = new JsonArray();
				e.aliases().forEach(a::add);
				j.add("a", a);
				j.addProperty("c", e.category());
				j.addProperty("p", e.codepoint());
				j.addProperty("b", e.builtin());
				emojis.add(j);
			}
		}
		o.add("emojis", emojis);
		return o;
	}

	private static void sendImages(ServerPlayer player) {
		ServerNetwork.send(player, new ChatPayloads.EmojiImage("*", new byte[0]));
		for (Map.Entry<String, byte[]> e : EmojiRegistry.customImages().entrySet()) {
			ServerNetwork.send(player, new ChatPayloads.EmojiImage(e.getKey(), e.getValue()));
		}
	}

	/** "ETIQUETA Nombre »" tal cual sale en el chat con Chaos Ranks (null sin Chaos Ranks o sin rango). */
	private static Component prefix(ServerPlayer player) {
		ChaosRanksCompat.Rank rank = ChaosRanksCompat.rankOf(player);
		if (rank == null) {
			return null;
		}
		MutableComponent out = Component.empty().append(rank.badge()).append(" ")
				.append(Component.literal(player.getGameProfile().getName()).setStyle(rank.nameStyle()));
		if (rank.separator() != null && !rank.separator().isEmpty()) {
			out.append(" ").append(Component.literal(rank.separator()).withColor(rank.separatorColor()));
		}
		return out;
	}

	/** Lo mismo sin las fuentes de Chaos Ranks: "[VIP] Nombre »". */
	private static Component prefixFallback(ServerPlayer player) {
		ChaosRanksCompat.Rank rank = ChaosRanksCompat.rankOf(player);
		if (rank == null) {
			return null;
		}
		MutableComponent out = Component.empty();
		if (rank.label() != null && !rank.label().isEmpty()) {
			out.append(Component.literal("[" + rank.label() + "] ").withStyle(s -> s.withColor(rank.bgColor()).withBold(true)));
		}
		out.append(Component.literal(player.getGameProfile().getName()).setStyle(rank.nameStyle()));
		if (rank.separator() != null && !rank.separator().isEmpty()) {
			out.append(" ").append(Component.literal(rank.separator()).withColor(rank.separatorColor()));
		}
		return out;
	}

	private static void upload(ServerPlayer player, ChatPayloads.UploadEmoji payload) {
		if (!Perms.canUpload(player)) {
			player.sendSystemMessage(Lang.tr("emoji.no_permission").withStyle(ChatFormatting.RED));
			return;
		}
		long now = System.currentTimeMillis();
		Long last = LAST_UPLOAD.get(player.getUUID());
		long cooldown = ChatConfig.get().emojis.uploadCooldownSeconds * 1000L;
		if (last != null && now - last < cooldown) {
			player.sendSystemMessage(Lang.tr("cooldown", (cooldown - (now - last) + 999) / 1000).withStyle(ChatFormatting.RED));
			return;
		}
		LAST_UPLOAD.put(player.getUUID(), now);
		if (!com.chaos.chatplus.emoji.EmojiImages.validUploadSize(payload.png())) {
			player.sendSystemMessage(Lang.tr("emoji.bad_image", EmojiRegistry.MAX_UPLOAD_SIZE).withStyle(ChatFormatting.RED));
			return;
		}
		boolean approved = !ChatConfig.get().emojis.requireApproval || player.hasPermissions(2);
		EmojiRegistry.Result result = EmojiRegistry.add(payload.name(), payload.png(), player.getGameProfile().getName(), approved);
		switch (result) {
			case OK -> {
				player.sendSystemMessage(Lang.tr("emoji.added", ":" + payload.name().toLowerCase() + ":").withStyle(ChatFormatting.GREEN));
				resyncAll(player.server);
			}
			case PENDING -> {
				player.sendSystemMessage(Lang.tr("emoji.pending").withStyle(ChatFormatting.YELLOW));
				Component note = Lang.tr("emoji.pending_staff", player.getGameProfile().getName(), payload.name().toLowerCase())
						.withStyle(ChatFormatting.GOLD);
				for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
					if (p.hasPermissions(2)) {
						p.sendSystemMessage(note);
					}
				}
				ChaosChatPlus.LOGGER.info("[ChatPlus] {} subió el emoji {} (pendiente de aprobar)",
						player.getGameProfile().getName(), payload.name());
			}
			default -> player.sendSystemMessage(Lang.tr("emoji.error." + result.name().toLowerCase(), EmojiRegistry.MAX_UPLOAD_SIZE)
					.withStyle(ChatFormatting.RED));
		}
	}
}
