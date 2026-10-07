package com.chaos.chatplus.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Piezas de dibujo del estilo moderno: rectángulos redondeados y la paleta. */
public final class Ui {

	/** Fondo de paneles (sin alfa). */
	public static final int PANEL = 0x111318;
	/** Fondo de paneles flotantes (popups, selector), con alfa. */
	public static final int POPUP = 0xF0181A20;
	public static final int POPUP_BORDER = 0xFF2A2D36;
	public static final int HOVER = 0x22FFFFFF;
	public static final int SELECTED = 0x337AA2FF;
	public static final int ACCENT = 0xFF7AA2FF;
	public static final int TEXT = 0xFFE6E8EE;
	public static final int MUTED = 0xFF8A8F98;
	public static final int MENTION = 0xFFFFD54A;
	public static final int WARN = 0xFFFFB74D;
	public static final int ERROR = 0xFFFF6B6B;

	private Ui() {}

	/** Rectángulo con las esquinas redondeadas (radio en px), hecho con filas de fill. */
	public static void round(GuiGraphics g, int x0, int y0, int x1, int y1, int radius, int argb) {
		int h = y1 - y0;
		int w = x1 - x0;
		int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		if ((argb >>> 24) == 0 || w <= 0 || h <= 0) {
			return;
		}
		for (int i = 0; i < r; i++) {
			int inset = inset(r, i);
			g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, argb);
			g.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, argb);
		}
		g.fill(x0, y0 + r, x1, y1 - r, argb);
	}

	/** Lo mismo con un borde de 1 px de otro color. */
	public static void roundBordered(GuiGraphics g, int x0, int y0, int x1, int y1, int radius, int fill, int border) {
		round(g, x0, y0, x1, y1, radius, border);
		round(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(0, radius - 1), fill);
	}

	private static int inset(int r, int row) {
		double dy = r - row - 0.5;
		return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
	}

	public static int alpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	public static int scaleAlpha(int argb, double factor) {
		int a = (int) ((argb >>> 24) * factor);
		return alpha(argb, a);
	}

	public static boolean inside(double mx, double my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}
}
