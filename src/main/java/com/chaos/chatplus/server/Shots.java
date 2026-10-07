package com.chaos.chatplus.server;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.perm.Perms;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Capturas en el chat. Solo llegan capturas que el cliente hace del propio juego (no hay forma de elegir un archivo).
 * Aun así el servidor desconfía: vuelve a codificar la imagen (fuera metadatos y datos raros), aplica tamaño máximo,
 * cooldown y permisos, la puede pasar por un filtro NSFW externo, y los jugadores pueden reportarla.
 */
public final class Shots {

	/** Comando que abre una captura (el cliente con el mod lo intercepta y abre el visor). */
	public static final String VIEW_COMMAND = "/chatplus shot view ";
	/** Comando de los OPs para revisar una captura reportada (con el mod se abre la vista previa con [Borrar]). */
	public static final String REVIEW_COMMAND = "/chatplus shot review ";
	private static final int MAX_SIDE = 1280;
	private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public record Shot(String id, UUID owner, String ownerName, byte[] jpeg, long created, Set<UUID> reporters) {}

	private static final class Upload {
		final byte[][] parts;
		int received;
		final long started = System.currentTimeMillis();

		Upload(int count) {
			parts = new byte[count][];
		}
	}

	private static final Map<String, Shot> SHOTS = new ConcurrentHashMap<>();
	private static final Set<String> HIDDEN = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Upload> UPLOADS = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_SHOT = new ConcurrentHashMap<>();
	private static final Set<UUID> PROCESSING = ConcurrentHashMap.newKeySet();

	private Shots() {}

	public static Collection<Shot> all() {
		return SHOTS.values();
	}

	// ------------------------------------------------------------------ subida

	public static void onPart(ServerPlayer player, ChatPayloads.ShotPart part) {
		ChatConfig.Screenshots cfg = ChatConfig.get().screenshots;
		UUID id = player.getUUID();
		int maxParts = (cfg.maxKilobytes * 1024) / ChatPayloads.SHOT_PART + 1;
		if (part.parts() < 1 || part.parts() > maxParts || part.part() < 0 || part.part() >= part.parts()) {
			UPLOADS.remove(id);
			if (part.part() == 0) {
				player.sendSystemMessage(Lang.tr("shot.too_big", cfg.maxKilobytes).withStyle(ChatFormatting.RED));
			}
			return;
		}
		if (part.part() == 0) {
			if (!Perms.canShots(player)) {
				player.sendSystemMessage(Lang.tr("shot.no_permission").withStyle(ChatFormatting.RED));
				return;
			}
			long left = cooldownLeft(player);
			if (left > 0) {
				player.sendSystemMessage(Lang.tr("cooldown", left).withStyle(ChatFormatting.RED));
				return;
			}
			if (PROCESSING.contains(id)) {
				return;
			}
			UPLOADS.put(id, new Upload(part.parts()));
		}
		Upload up = UPLOADS.get(id);
		if (up == null || up.parts.length != part.parts() || up.parts[part.part()] != null) {
			return;
		}
		up.parts[part.part()] = part.data();
		up.received++;
		if (up.received < up.parts.length) {
			return;
		}
		UPLOADS.remove(id);
		ByteArrayOutputStream all = new ByteArrayOutputStream();
		for (byte[] p : up.parts) {
			all.writeBytes(p);
		}
		LAST_SHOT.put(id, System.currentTimeMillis());
		PROCESSING.add(id);
		MinecraftServer server = player.server;
		String name = player.getGameProfile().getName();
		byte[] raw = all.toByteArray();
		CompletableFuture.supplyAsync(() -> reencode(raw, cfg.maxKilobytes * 1024), Util.backgroundExecutor())
				.thenCompose(clean -> clean == null
						? CompletableFuture.completedFuture(new Verdict(null, "shot.invalid"))
						: Moderation.check(clean).thenApply(ok -> ok == null
								? new Verdict(null, "shot.unverified")
								: ok ? new Verdict(clean, null) : new Verdict(null, "shot.rejected")))
				.whenComplete((verdict, error) -> server.execute(() -> {
					PROCESSING.remove(id);
					ServerPlayer owner = server.getPlayerList().getPlayer(id);
					if (error != null || verdict == null || verdict.jpeg == null) {
						if (error != null) {
							ChaosChatPlus.LOGGER.warn("Captura de {} descartada", name, error);
						}
						LAST_SHOT.remove(id);
						if (owner != null) {
							String key = verdict != null && verdict.error != null ? verdict.error : "shot.invalid";
							owner.sendSystemMessage(Lang.tr(key).withStyle(ChatFormatting.RED));
						}
						if (verdict != null && "shot.rejected".equals(verdict.error)) {
							log("RECHAZADA por el filtro: captura de " + name);
						}
						return;
					}
					publish(server, id, name, verdict.jpeg);
				}));
	}

	private record Verdict(@Nullable byte[] jpeg, @Nullable String error) {}

	private static long cooldownLeft(ServerPlayer player) {
		if (player.hasPermissions(2)) {
			return 0;
		}
		Long last = LAST_SHOT.get(player.getUUID());
		long cooldown = ChatConfig.get().screenshots.cooldownSeconds * 1000L;
		if (last == null) {
			return 0;
		}
		long left = cooldown - (System.currentTimeMillis() - last);
		return left <= 0 ? 0 : (left + 999) / 1000;
	}

	/** Decodifica y vuelve a codificar como JPEG limpio. Null si no es una imagen válida o no cabe. */
	@Nullable
	static byte[] reencode(byte[] data, int maxBytes) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			if (img == null || img.getWidth() < 16 || img.getHeight() < 16 || img.getWidth() > 4096 || img.getHeight() > 4096) {
				return null;
			}
			int w = img.getWidth();
			int h = img.getHeight();
			if (w > MAX_SIDE || h > MAX_SIDE) {
				double s = (double) MAX_SIDE / Math.max(w, h);
				w = (int) Math.round(w * s);
				h = (int) Math.round(h * s);
			}
			BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = rgb.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(img, 0, 0, w, h, null);
			g.dispose();
			for (float q : new float[] {0.85F, 0.7F, 0.5F}) {
				byte[] out = jpeg(rgb, q);
				if (out.length <= maxBytes) {
					return out;
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static byte[] jpeg(BufferedImage img, float quality) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(out);
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(quality);
			writer.write(null, new IIOImage(img, null, null), param);
		} finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	private static void publish(MinecraftServer server, UUID owner, String ownerName, byte[] jpeg) {
		String id = newId();
		SHOTS.put(id, new Shot(id, owner, ownerName, jpeg, System.currentTimeMillis(), ConcurrentHashMap.newKeySet()));
		log("Captura " + id + " de " + ownerName + " (" + jpeg.length / 1024 + " KB)");
		ServerPlayer sender = server.getPlayerList().getPlayer(owner);
		Component name = sender != null ? sender.getDisplayName() : Component.literal(ownerName);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendSystemMessage(message(p, id, name));
		}
		ChaosChatPlus.LOGGER.info("[ChatPlus] {} envió una captura ({})", ownerName, id);
	}

	/** "Luis 📷 envió una captura [Ver]" (el icono es el emoji si quien lo recibe puede verlo). */
	private static Component message(ServerPlayer viewer, String id, Component name) {
		MutableComponent view = Lang.tr("shot.view").withStyle(s -> s
				.withColor(0x7AD7FF)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, VIEW_COMMAND + id))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						Lang.tr(ChatServer.isModded(viewer) ? "shot.hover" : "shot.hover_vanilla"))));
		return Component.empty().append(Lang.tr("shot.sent", name, ChatTransform.icon(viewer, "camara"))).append(" ").append(view);
	}

	private static String newId() {
		String chars = "abcdefghijkmnpqrstuvwxyz23456789";
		StringBuilder sb = new StringBuilder();
		java.util.Random r = new java.util.Random();
		do {
			sb.setLength(0);
			for (int i = 0; i < 8; i++) {
				sb.append(chars.charAt(r.nextInt(chars.length())));
			}
		} while (SHOTS.containsKey(sb.toString()));
		return sb.toString();
	}

	// ------------------------------------------------------------------ ver, reportar, borrar

	/** /chatplus shot view: con el mod manda la imagen; sin el mod, explica que hace falta. */
	public static void view(ServerPlayer player, String id) {
		if (ChatServer.isModded(player)) {
			send(player, id);
		} else {
			player.sendSystemMessage(Lang.tr("shot.need_mod").withStyle(ChatFormatting.YELLOW));
		}
	}

	public static void send(ServerPlayer player, String id) {
		Shot shot = SHOTS.get(id);
		if (shot == null || (HIDDEN.contains(id) && !player.hasPermissions(2))) {
			ServerPlayNetworking.send(player, new ChatPayloads.ShotData(id, "", new byte[0]));
			return;
		}
		ServerPlayNetworking.send(player, new ChatPayloads.ShotData(id, shot.ownerName(), shot.jpeg()));
	}

	/** /chatplus shot review (OP): sin el mod, texto con [Borrar]; con el mod lo intercepta el cliente. */
	public static void review(ServerPlayer player, String id) {
		Shot shot = SHOTS.get(id);
		if (shot == null) {
			player.sendSystemMessage(Lang.tr("shot.not_found", id).withStyle(ChatFormatting.RED));
			return;
		}
		if (ChatServer.isModded(player)) {
			send(player, id);
		}
		player.sendSystemMessage(Lang.tr("shot.review_text", id, shot.ownerName(), shot.reporters().size())
				.withStyle(ChatFormatting.GOLD).append(" ")
				.append(Lang.tr("shot.delete_button").withStyle(s -> s.withColor(ChatFormatting.RED)
						.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatplus shot delete " + id)))));
	}

	public static void report(ServerPlayer reporter, String id) {
		Shot shot = SHOTS.get(id);
		if (shot == null || shot.owner().equals(reporter.getUUID()) || !shot.reporters().add(reporter.getUUID())) {
			return;
		}
		String who = reporter.getGameProfile().getName();
		log("REPORTE de " + who + ": captura " + id + " de " + shot.ownerName() + " (" + shot.reporters().size() + " reportes)");
		reporter.sendSystemMessage(Lang.tr("shot.reported").withStyle(ChatFormatting.GREEN));
		MutableComponent note = Lang.tr("shot.report_staff", who, shot.ownerName(), id).withStyle(s -> s
				.withColor(ChatFormatting.GOLD)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, REVIEW_COMMAND + id)));
		for (ServerPlayer p : reporter.server.getPlayerList().getPlayers()) {
			if (p.hasPermissions(2)) {
				p.sendSystemMessage(note);
			}
		}
		int limit = ChatConfig.get().screenshots.autoHideReports;
		if (limit > 0 && shot.reporters().size() >= limit) {
			hide(reporter.server, id);
			log("Captura " + id + " ocultada automáticamente por reportes");
		}
	}

	/** Borra (o deja de servir) una captura y avisa a los clientes para que la quiten de su caché. */
	public static boolean delete(MinecraftServer server, String id) {
		if (SHOTS.remove(id) == null) {
			return false;
		}
		HIDDEN.remove(id);
		revoke(server, id);
		log("Captura " + id + " borrada");
		return true;
	}

	private static void hide(MinecraftServer server, String id) {
		HIDDEN.add(id);
		revoke(server, id);
	}

	private static void revoke(MinecraftServer server, String id) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (ChatServer.isModded(p)) {
				ServerPlayNetworking.send(p, new ChatPayloads.ShotData(id, "", new byte[0]));
			}
		}
	}

	// ------------------------------------------------------------------ mantenimiento

	public static void tick(MinecraftServer server) {
		long expire = ChatConfig.get().screenshots.expireMinutes * 60_000L;
		long now = System.currentTimeMillis();
		List<String> gone = new ArrayList<>();
		for (Iterator<Shot> it = SHOTS.values().iterator(); it.hasNext(); ) {
			Shot s = it.next();
			if (now - s.created() > expire) {
				it.remove();
				gone.add(s.id());
			}
		}
		gone.forEach(HIDDEN::remove);
		UPLOADS.entrySet().removeIf(e -> now - e.getValue().started > 60_000);
	}

	public static void forget(ServerPlayer player) {
		UPLOADS.remove(player.getUUID());
	}

	public static void clear() {
		SHOTS.clear();
		HIDDEN.clear();
		UPLOADS.clear();
		LAST_SHOT.clear();
		PROCESSING.clear();
	}

	public static Set<String> ids() {
		return new HashSet<>(SHOTS.keySet());
	}

	private static void log(String line) {
		try {
			Files.createDirectories(ChatConfig.dir());
			Files.writeString(ChatConfig.dir().resolve("reports.log"),
					"[" + LocalDateTime.now().format(LOG_TIME) + "] " + line + System.lineSeparator(),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.warn("No se pudo escribir reports.log", e);
		}
	}
}
