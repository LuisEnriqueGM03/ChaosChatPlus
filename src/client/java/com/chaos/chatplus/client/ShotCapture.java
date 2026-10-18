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
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.server.Shots;
import com.mojang.blaze3d.platform.NativeImage;

import com.chaos.chatplus.client.ClientNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Capturas para el chat. Se hacen del propio juego: se oculta la interfaz un instante, se lee la imagen del frame y
 * se vuelve a mostrar. La interfaz no ofrece archivos arbitrarios; la moderación del contenido corresponde al servidor.
 */
public final class ShotCapture {

	/** Comando local que pone el botón "Enviar al chat" tras una captura con F2 (no llega al servidor). */
	public static final String SEND_FILE_COMMAND = "/chatplus-local sendshot ";
	private static final int MAX_WIDTH = 960;

	private static boolean cameraMode;
	private static double zoom = 1.0;
	private static int countdown = -1;
	private static int hudFrames;

	private ShotCapture() {}

	/**
	 * Botón de la cámara: cierra el chat y entra en modo cámara. Sin interfaz, con un visor; el jugador se puede mover
	 * y girar. Clic izquierdo o Enter captura, Esc cancela.
	 */
	public static void start() {
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setScreen(null);
		cameraMode = true;
		hudFrames = 0;
		countdown = -1;
		zoom = 1.0;
	}

	public static double zoom() {
		return zoom;
	}

	/** Rueda del ratón en modo cámara: acercar o alejar (x1 a x8). */
	public static void scrollZoom(double amount) {
		zoom = Math.max(1.0, Math.min(8.0, zoom * (amount > 0 ? 1.15 : 1 / 1.15)));
	}

	public static boolean cameraMode() {
		return cameraMode;
	}

	/** El HUD de vanilla no se pinta mientras dura el modo cámara. */
	public static boolean hideHud() {
		return cameraMode;
	}

	/** Pulsar capturar: el visor se oculta y se captura un par de frames después. */
	public static void shoot() {
		if (cameraMode && countdown < 0) {
			countdown = 2;
		}
	}

	public static void cancel() {
		cameraMode = false;
		countdown = -1;
		zoom = 1.0;
	}

	/** Inicio del tick: los clics del modo cámara no pegan ni rompen bloques. */
	public static void startTick(Minecraft mc) {
		if (!cameraMode) {
			return;
		}
		if (mc.gui.screen() != null || mc.player == null) {
			cancel();
			return;
		}
		boolean clicked = false;
		while (mc.options.keyAttack.consumeClick()) {
			clicked = true;
		}
		while (mc.options.keyUse.consumeClick()) {
			// nada: con la cámara no se usan ítems
		}
		if (clicked) {
			shoot();
		}
	}

	public static void tick(Minecraft mc) {
		if (!cameraMode || countdown < 0) {
			return;
		}
		if (--countdown > 0) {
			return;
		}
        countdown = -2;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> mc.execute(() -> {
            boolean active = cameraMode;
            cancel();
            if (active && mc.player != null) mc.gui.setScreen(new ShotSendScreen(image));
            else image.close();
        }));
	}

	/** Visor del modo cámara (esquinas, título y ayuda). No se pinta en el frame que se captura. */
	public static void renderViewfinder(GuiGraphicsExtractor g) {
		if (cameraMode) hudFrames++;
		if (!cameraMode || countdown != -1) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int w = g.guiWidth();
		int h = g.guiHeight();
		int m = 12;
		int len = 14;
		int c = 0xEEFFFFFF;
		int sh = 0x66000000;
		for (int[] corner : new int[][] {{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}}) {
			int x = corner[0];
			int y = corner[1];
			int dx = corner[2];
			int dy = corner[3];
			fillLine(g, x + 1, y + 1, x + dx * len + 1, y + dy * 2 + 1, sh);
			fillLine(g, x + 1, y + 1, x + dx * 2 + 1, y + dy * len + 1, sh);
			fillLine(g, x, y, x + dx * len, y + dy * 2, c);
			fillLine(g, x, y, x + dx * 2, y + dy * len, c);
		}
		// Punto central.
		g.fill(w / 2 - 1, h / 2 - 1, w / 2 + 1, h / 2 + 1, 0x99FFFFFF);
		Component title = zoom > 1.01
				? Lang.tr("camera.title").append(String.format(" · x%.1f", zoom))
				: Lang.tr("camera.title");
		int tw = mc.font.width(title) + 22;
		Ui.round(g, w / 2 - tw / 2, m + 2, w / 2 + tw / 2, m + 16, 5, 0xAA111318);
		com.chaos.chatplus.client.ui.Ui.smooth(ChaosChatPlus.id("textures/gui/btn_shot.png"));
		com.chaos.chatplus.client.ui.Ui.blit(g, ChaosChatPlus.id("textures/gui/btn_shot.png"), w / 2 - tw / 2 + 5, m + 5, 8, 8, 0, 0, 32, 32, 32, 32);
		g.text(mc.font, title, w / 2 - tw / 2 + 16, m + 5, 0xFFFFFFFF, false);
		Component hint = Lang.tr("camera.hint");
		int hw = mc.font.width(hint) + 16;
		Ui.round(g, w / 2 - hw / 2, h - m - 18, w / 2 + hw / 2, h - m - 4, 5, 0xAA111318);
		g.text(mc.font, hint, w / 2 - hw / 2 + 8, h - m - 15, 0xFFE6E8EE, false);
	}

	private static void fillLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
		g.fill(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1), color);
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
				mc.gui.setScreen(new ShotSendScreen(NativeImage.read(in)));
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
		if (!(open instanceof ClickEvent.OpenFile fileEvent)) {
			return message;
		}
		return message.copy().append(" ").append(Lang.tr("shot.send_to_chat").withStyle(s -> s
				.withColor(0x7AD7FF)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand(SEND_FILE_COMMAND + fileEvent.path()))
				.withHoverEvent(new HoverEvent.ShowText(Lang.tr("shot.send_hover")))));
	}

	/** Reduce, codifica en JPEG y sube la captura en trozos. Libera la imagen. */
	public static void send(NativeImage image) {
		BufferedImage rgb = toBuffered(image);
		image.close();
		int maxBytes = ClientState.maxShotKb() * 1024;
		var connection = Minecraft.getInstance().getConnection();
		String target = ClientState.dmTargetOrEmpty();
		CompletableFuture.supplyAsync(() -> encode(rgb, maxBytes), Util.backgroundExecutor()).thenAccept(jpeg ->
				Minecraft.getInstance().execute(() -> {
					if (Minecraft.getInstance().getConnection() == connection) upload(jpeg, target);
				}));
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

	private static void upload(@Nullable byte[] jpeg, String target) {
		Minecraft mc = Minecraft.getInstance();
		if (jpeg == null) {
			if (mc.player != null) {
				Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Lang.tr("shot.too_big", ClientState.maxShotKb()).withStyle(ChatFormatting.RED));
			}
			return;
		}
		if (!ClientNetwork.canSend(ChatPayloads.ShotPart.TYPE)) {
			return;
		}
		int parts = (jpeg.length + ChatPayloads.SHOT_PART - 1) / ChatPayloads.SHOT_PART;
		for (int i = 0; i < parts; i++) {
			int from = i * ChatPayloads.SHOT_PART;
			int to = Math.min(jpeg.length, from + ChatPayloads.SHOT_PART);
			byte[] chunk = java.util.Arrays.copyOfRange(jpeg, from, to);
			ClientNetwork.send(new ChatPayloads.ShotPart(i, parts, chunk, target));
		}
		if (mc.player != null) {
			Minecraft.getInstance().gui.hud.setOverlayMessage(Lang.tr("shot.sending").withStyle(ChatFormatting.GRAY), false);
		}
	}

	static BufferedImage toBuffered(NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = image.getPixel(x, y) & 0xFFFFFF;
				out.setRGB(x, y, rgb);
			}
		}
		return out;
	}

	public static Path screenshotsDir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
	}
}
