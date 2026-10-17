package com.chaos.chatplus.client.screen;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.GifCache;
import com.chaos.chatplus.client.ui.FlatButton;
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.gif.GifInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Un GIF del chat en grande. */
public class GifViewerScreen extends Screen {

	private final GifInfo gif;
	private final Screen parent;

	public GifViewerScreen(GifInfo gif, Screen parent) {
		super(Component.literal(gif.name()));
		this.gif = gif;
		this.parent = parent;
	}

	@Override
	protected void init() {
		addRenderableWidget(new FlatButton(width / 2 - 38, height - 26, 76, 16, Lang.tr("button.close"), false, b -> onClose()));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		int maxW = Math.min(width - 40, 320);
		int maxH = height - 70;
		int x = (width - maxW) / 2;
		int y = 24;
		Ui.round(g, x - 4, y - 4, x + maxW + 4, y + maxH + 4, 6, Ui.POPUP);
		GifCache.draw(g, gif, x, y, maxW, maxH, true);
		g.drawCenteredString(font, font.plainSubstrByWidth(gif.name(), width - 20), width / 2, 8, Ui.TEXT);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0xCC0B0C10);
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
