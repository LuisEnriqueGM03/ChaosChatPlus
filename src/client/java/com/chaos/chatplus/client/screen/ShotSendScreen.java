package com.chaos.chatplus.client.screen;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.ui.FlatButton;
import com.chaos.chatplus.client.ui.Ui;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Vista previa de la captura antes de mandarla al chat: [Enviar] o [Cancelar]. */
public class ShotSendScreen extends Screen {

	private static final ResourceLocation PREVIEW = ChaosChatPlus.id("shot_preview");

	private NativeImage image;
	private final int imageW;
	private final int imageH;
	private boolean sent;
    private boolean previewRegistered;

	public ShotSendScreen(NativeImage image) {
		super(Lang.tr("shot.send_title"));
		this.image = image;
		this.imageW = image.getWidth();
		this.imageH = image.getHeight();
	}

	@Override
	protected void init() {
		if (minecraft != null && !previewRegistered && image != null) {
			// La textura se queda con una copia; la original se usa para codificar.
			NativeImage copy = new NativeImage(imageW, imageH, false);
			copy.copyFrom(image);
			minecraft.getTextureManager().register(PREVIEW, new DynamicTexture(copy));
            previewRegistered = true;
		}
		int bw = 80;
		int y = height - 30;
		addRenderableWidget(new FlatButton(width / 2 - bw - 4, y, bw, 18, Lang.tr("shot.send"), true, b -> send()));
		addRenderableWidget(new FlatButton(width / 2 + 4, y, bw, 18, Lang.tr("button.cancel"), false, b -> onClose()));
	}

	/** Para la autoprueba. */
	public void sendForTest() {
		send();
	}

	private void send() {
		if (sent || image == null) {
			return;
		}
		sent = true;
		NativeImage img = image;
		image = null;
		ShotCapture.send(img);
		onClose();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		int maxW = width - 40;
		int maxH = height - 70;
		double s = Math.min((double) maxW / imageW, (double) maxH / imageH);
		int w = (int) (imageW * s);
		int h = (int) (imageH * s);
		int x = (width - w) / 2;
		int y = 24;
		Ui.round(g, x - 3, y - 3, x + w + 3, y + h + 3, 4, Ui.POPUP);
		com.chaos.chatplus.client.ui.Ui.blit(g, PREVIEW, x, y, w, h, 0, 0, 1, 1, 1, 1);
		g.drawCenteredString(font, title, width / 2, 8, Ui.TEXT);
		g.drawCenteredString(font, Lang.tr("shot.send_note"), width / 2, height - 42, Ui.MUTED);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0xCC0B0C10);
	}

	@Override
	public void removed() {
		if (minecraft != null) {
			minecraft.getTextureManager().release(PREVIEW);
		}
		if (image != null && !sent) {
			image.close();
			image = null;
		}
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(null);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
