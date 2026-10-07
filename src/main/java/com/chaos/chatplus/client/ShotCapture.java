package com.chaos.chatplus.client;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.screen.ShotSendScreen;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.server.Shots;
import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Capturas para el chat. Se hacen del propio juego: se oculta la interfaz un instante, se lee la imagen del frame y
 * se vuelve a mostrar. No hay forma de mandar un archivo cualquiera del PC (protección anti-NSFW).
 */
public final class ShotCapture {

	/** Comando local que pone el botón "Enviar al chat" tras una captura con F2 (no llega al servidor). */
	public static final String SEND_FILE_COMMAND = "/chatplus-local sendshot ";
	private static final int MAX_WIDTH = 960;

	private static int countdown = -1;
	private static boolean hadHiddenGui;

	private ShotCapture() {}

	/** Botón de la cámara: cierra el chat y hace la captura dentro de un par de ticks, sin la interfaz. */
	public static void start() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(null);
		hadHiddenGui = mc.options.hideGui;
		mc.options.hideGui = true;
		countdown = 3;
	}

	public static void tick(Minecraft mc) {
		if (countdown < 0) {
			return;
		}
		if (--countdown > 0) {
			return;
		}
		countdown = -1;
		NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
		mc.options.hideGui = hadHiddenGui;
		mc.setScreen(new ShotSendScreen(image));
	}

	/** Abre la pantalla de enviar con una captura hecha con F2 (solo de la carpeta screenshots del juego). */
	public static void openFile(String path) {
		Minecraft mc = Minecraft.getInstance();
		try {
			File file = new File(path).getCanonicalFile();
			File dir = new File(mc.gameDirectory, "screenshots").getCanonicalFile();
			if (!file.getParentFile().equals(dir) || !file.getName().toLowerCase().endsWith(".png")) {
				return;
			}
			try (var in = java.nio.file.Files.newInputStream(file.toPath())) {
				mc.setScreen(new ShotSendScreen(NativeImage.read(in)));
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("No se pudo abrir la captura {}", path, e);
		}
	}

	/** Al mensaje "Captura guardada como ..." de F2 le añade "[Enviar al chat]" si el servidor lo permite. */
	public static Component decorateScreenshotMessage(Component message) {
		if (!ChaosChatPlusClient.ACTIVE || !ClientState.shots()
				|| !(message.getContents() instanceof TranslatableContents t) || !t.getKey().equals("screenshot.success")
				|| t.getArgs().length == 0 || !(t.getArgs()[0] instanceof Component file)) {
			return message;
		}
		ClickEvent open = file.getStyle().getClickEvent();
		if (open == null || open.getAction() != ClickEvent.Action.OPEN_FILE) {
			return message;
		}
		return message.copy().append(" ").append(Lang.tr("shot.send_to_chat").withStyle(s -> s
				.withColor(0x7AD7FF)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, SEND_FILE_COMMAND + open.getValue()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.tr("shot.send_hover")))));
	}

	/** Reduce, codifica en JPEG y sube la captura en trozos. Libera la imagen. */
	public static void send(NativeImage image) {
		BufferedImage rgb = toBuffered(image);
		image.close();
		int maxBytes = ClientState.maxShotKb() * 1024;
		CompletableFuture.supplyAsync(() -> encode(rgb, maxBytes), Util.backgroundExecutor()).thenAccept(jpeg ->
				Minecraft.getInstance().execute(() -> upload(jpeg)));
	}

	@Nullable
	private static byte[] encode(BufferedImage img, int maxBytes) {
		try {
			int w = img.getWidth();
			int h = img.getHeight();
			if (w > MAX_WIDTH) {
				h = Math.round(h * (MAX_WIDTH / (float) w));
				w = MAX_WIDTH;
			}
			BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaled.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(img, 0, 0, w, h, null);
			g.dispose();
			for (float q : new float[] {0.82F, 0.65F, 0.45F}) {
				byte[] out = Shots.jpeg(scaled, q);
				if (out.length <= maxBytes) {
					return out;
				}
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("No se pudo codificar la captura", e);
		}
		return null;
	}

	private static void upload(@Nullable byte[] jpeg) {
		Minecraft mc = Minecraft.getInstance();
		if (jpeg == null) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Lang.tr("shot.too_big", ClientState.maxShotKb()).withStyle(ChatFormatting.RED), false);
			}
			return;
		}
		if (!ClientPlayNetworking.canSend(ChatPayloads.ShotPart.TYPE)) {
			return;
		}
		int parts = (jpeg.length + ChatPayloads.SHOT_PART - 1) / ChatPayloads.SHOT_PART;
		for (int i = 0; i < parts; i++) {
			int from = i * ChatPayloads.SHOT_PART;
			int to = Math.min(jpeg.length, from + ChatPayloads.SHOT_PART);
			byte[] chunk = java.util.Arrays.copyOfRange(jpeg, from, to);
			ClientPlayNetworking.send(new ChatPayloads.ShotPart(i, parts, chunk));
		}
		if (mc.player != null) {
			mc.player.displayClientMessage(Lang.tr("shot.sending").withStyle(ChatFormatting.GRAY), true);
		}
	}

	static BufferedImage toBuffered(NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int abgr = image.getPixelRGBA(x, y);
				int rgb = ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF);
				out.setRGB(x, y, rgb);
			}
		}
		return out;
	}

	public static Path screenshotsDir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
	}
}
