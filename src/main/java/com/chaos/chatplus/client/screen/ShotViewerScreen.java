package com.chaos.chatplus.client.screen;

import java.nio.file.Files;
import java.nio.file.Path;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.ShotCache;
import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.ui.FlatButton;
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.net.ChatPayloads;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/** Visor de una captura del chat: [Guardar], [Reportar], [Cerrar]. Si el servidor lo pide, empieza difuminada. */
public class ShotViewerScreen extends Screen {

	private final String id;
	private final Screen parent;
	private boolean revealed;
	private boolean reported;
	private FlatButton save;
	private FlatButton report;

	public ShotViewerScreen(String id, Screen parent) {
		super(Lang.tr("shot.viewer_title"));
		this.id = id;
		this.parent = parent;
		this.revealed = !ClientState.blurShots();
	}

	@Override
	protected void init() {
		int y = height - 26;
		int bw = 76;
		save = addRenderableWidget(new FlatButton(width / 2 - bw * 3 / 2 - 6, y, bw, 16, Lang.tr("shot.save"), true, b -> save()));
		report = addRenderableWidget(new FlatButton(width / 2 - bw / 2, y, bw, 16, Lang.tr("shot.report"), false, b -> report()).danger());
		addRenderableWidget(new FlatButton(width / 2 + bw / 2 + 6, y, bw, 16, Lang.tr("button.close"), false, b -> onClose()));
	}

	@Override
	public void tick() {
		ShotCache.Shot shot = ShotCache.get(id);
		save.active = shot != null;
		boolean mine = shot != null && minecraft != null && minecraft.player != null
				&& shot.owner().equals(minecraft.player.getGameProfile().getName());
		report.active = shot != null && !reported && !mine;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		ShotCache.Shot shot = ShotCache.get(id);
		if (shot == null) {
			String key = ShotCache.gone(id) ? "shot.gone" : "shot.loading";
			g.drawCenteredString(font, Lang.tr(key), width / 2, height / 2 - 4, Ui.MUTED);
			return;
		}
		int maxW = width - 40;
		int maxH = height - 60;
		double s = Math.min((double) maxW / shot.width(), (double) maxH / shot.height());
		s = Math.min(s, 2.0);
		int w = (int) (shot.width() * s);
		int h = (int) (shot.height() * s);
		int x = (width - w) / 2;
		int y = 20 + (maxH - h) / 2;
		Ui.round(g, x - 3, y - 3, x + w + 3, y + h + 3, 4, Ui.POPUP);
		g.blit(revealed ? shot.texture() : shot.blurred(), x, y, w, h, 0, 0, 1, 1, 1, 1);
		if (!revealed) {
			Component show = Lang.tr("shot.reveal");
			int tw = font.width(show) + 16;
			Ui.round(g, width / 2 - tw / 2, y + h / 2 - 9, width / 2 + tw / 2, y + h / 2 + 9, 5, 0xCC111318);
			g.drawCenteredString(font, show, width / 2, y + h / 2 - 4, Ui.TEXT);
		}
		g.drawCenteredString(font, Lang.tr("shot.by", shot.owner()), width / 2, 6, Ui.TEXT);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!revealed && my < height - 30) {
			revealed = true;
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	private void save() {
		ShotCache.Shot shot = ShotCache.get(id);
		if (shot == null || minecraft == null || minecraft.player == null) {
			return;
		}
		try {
			Path dir = ShotCapture.screenshotsDir().resolve("chaoschatplus");
			Files.createDirectories(dir);
			String owner = shot.owner().replaceAll("[^A-Za-z0-9_]", "_");
			Path file = dir.resolve(owner + "_" + id + ".jpg");
			Files.write(file, shot.jpeg());
			Component name = Component.literal(file.getFileName().toString()).withStyle(s -> s
					.withUnderlined(true)
					.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.toAbsolutePath().toString())));
			minecraft.player.displayClientMessage(Lang.tr("shot.saved", name).withStyle(ChatFormatting.GREEN), false);
			save.active = false;
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("No se pudo guardar la captura {}", id, e);
		}
	}

	private void report() {
		if (reported || !ClientPlayNetworking.canSend(ChatPayloads.ReportShot.TYPE)) {
			return;
		}
		reported = true;
		report.active = false;
		ClientPlayNetworking.send(new ChatPayloads.ReportShot(id));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0xE00B0C10);
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
