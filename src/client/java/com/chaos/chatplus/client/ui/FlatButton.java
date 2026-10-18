package com.chaos.chatplus.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botón plano y redondeado del estilo moderno. {@code primary} = azul de acento. */
public class FlatButton extends Button {

	private final boolean primary;
	private boolean danger;

	public FlatButton(int x, int y, int width, int height, Component message, boolean primary, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.primary = primary;
	}

	public FlatButton danger() {
		this.danger = true;
		return this;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		boolean hover = isHoveredOrFocused() && active;
		int fill;
		if (!active) {
			fill = 0xFF24262D;
		} else if (danger) {
			fill = hover ? 0xFFE05555 : 0xFF3A2328;
		} else if (primary) {
			fill = hover ? 0xFF93B4FF : 0xFF5F86E8;
		} else {
			fill = hover ? 0xFF353945 : 0xFF2A2D36;
		}
		Ui.round(g, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 4, fill);
		int color = !active ? 0xFF6C717B : danger && !hover ? 0xFFFF8A8A : 0xFFFFFFFF;
		var font = Minecraft.getInstance().font;
		g.text(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2,
				getY() + (getHeight() - 8) / 2, color, false);
	}
}
