package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientPrefs;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiTextures;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Selector de emojis: pestañas (recientes, favoritos y categorías), rejilla con scroll. Clic = insertar en el chat,
 * clic derecho = favorito.
 */
public final class EmojiPicker {

	private static final int COLS = 9;
	private static final int ROWS = 6;
	private static final int CELL = 13;
	private static final int TAB = 12;
	private static final int PAD = 4;
	public static final int WIDTH = COLS * CELL + PAD * 2;
	public static final int HEIGHT = TAB + 4 + ROWS * CELL + 14 + PAD;

	/** Pestaña: id, emoji que hace de icono (o texto) y su nombre traducible. */
	private record Tab(String id, String icon, String key) {}

	private static final Map<String, String> CATEGORY_ICONS = new LinkedHashMap<>();

	static {
		CATEGORY_ICONS.put("caras", "sonrisa");
		CATEGORY_ICONS.put("corazones", "corazon");
		CATEGORY_ICONS.put("gestos", "pulgar_arriba");
		CATEGORY_ICONS.put("simbolos", "fuego");
		CATEGORY_ICONS.put("objetos", "regalo");
		CATEGORY_ICONS.put("minecraft", "diamante");
		CATEGORY_ICONS.put("servidor", "corona");
	}

	private final Consumer<ClientState.ClientEmoji> onPick;
	private final List<Tab> tabs = new ArrayList<>();
	private String tab;
	private int scroll;
	private int x;
	private int y;

	public EmojiPicker(Consumer<ClientState.ClientEmoji> onPick) {
		this.onPick = onPick;
		tabs.add(new Tab("recent", "◷", "picker.recent"));
		tabs.add(new Tab("favorites", "★", "picker.favorites"));
		for (Map.Entry<String, String> e : CATEGORY_ICONS.entrySet()) {
			boolean any = ClientState.emojis().stream().anyMatch(em -> em.category().equals(e.getKey()));
			if (any) {
				tabs.add(new Tab(e.getKey(), e.getValue(), "category." + e.getKey()));
			}
		}
		tab = ClientPrefs.get().recentEmojis.isEmpty() ? (tabs.size() > 2 ? tabs.get(2).id() : "recent") : "recent";
	}

	public void place(int right, int bottom) {
		x = right - WIDTH;
		y = bottom - HEIGHT;
	}

	public boolean contains(double mx, double my) {
		return Ui.inside(mx, my, x, y, x + WIDTH, y + HEIGHT);
	}

	private List<ClientState.ClientEmoji> current() {
		List<ClientState.ClientEmoji> out = new ArrayList<>();
		switch (tab) {
			case "recent" -> ClientPrefs.get().recentEmojis.forEach(n -> add(out, n));
			case "favorites" -> ClientPrefs.get().favoriteEmojis.forEach(n -> add(out, n));
			default -> ClientState.emojis().stream().filter(e -> e.category().equals(tab)).forEach(out::add);
		}
		return out;
	}

	private static void add(List<ClientState.ClientEmoji> out, String name) {
		ClientState.ClientEmoji e = ClientState.emoji(name);
		if (e != null) {
			out.add(e);
		}
	}

	private int maxScroll(int count) {
		return Math.max(0, (count + COLS - 1) / COLS - ROWS);
	}

	public void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
		Ui.roundBordered(g, x, y, x + WIDTH, y + HEIGHT, 5, Ui.POPUP, Ui.POPUP_BORDER);

		// Pestañas.
		int tx = x + PAD;
		Tab hoveredTab = null;
		for (Tab t : tabs) {
			boolean sel = t.id().equals(tab);
			boolean hover = Ui.inside(mouseX, mouseY, tx, y + PAD, tx + TAB, y + PAD + TAB);
			if (sel) {
				Ui.round(g, tx, y + PAD, tx + TAB, y + PAD + TAB, 3, Ui.SELECTED);
			} else if (hover) {
				Ui.round(g, tx, y + PAD, tx + TAB, y + PAD + TAB, 3, Ui.HOVER);
			}
			if (hover) {
				hoveredTab = t;
			}
			ClientState.ClientEmoji icon = ClientState.emoji(t.icon());
			if (icon != null) {
				EmojiTextures.draw(g, icon, tx + 2, y + PAD + 2, 8);
			} else {
				g.drawString(font, t.icon(), tx + (TAB - font.width(t.icon())) / 2 + 1, y + PAD + 2, sel ? Ui.MENTION : Ui.MUTED, false);
			}
			tx += TAB + 1;
		}
		g.fill(x + PAD, y + PAD + TAB + 2, x + WIDTH - PAD, y + PAD + TAB + 3, Ui.POPUP_BORDER);

		// Rejilla.
		List<ClientState.ClientEmoji> list = current();
		scroll = Mth.clamp(scroll, 0, maxScroll(list.size()));
		int gx = x + PAD;
		int gy = y + PAD + TAB + 4;
		ClientState.ClientEmoji hovered = null;
		for (int i = scroll * COLS; i < list.size() && i < (scroll + ROWS) * COLS; i++) {
			int col = i % COLS;
			int row = i / COLS - scroll;
			int cx = gx + col * CELL;
			int cy = gy + row * CELL;
			ClientState.ClientEmoji e = list.get(i);
			if (Ui.inside(mouseX, mouseY, cx, cy, cx + CELL, cy + CELL)) {
				Ui.round(g, cx, cy, cx + CELL, cy + CELL, 3, Ui.HOVER);
				hovered = e;
			}
			EmojiTextures.draw(g, e, cx + 2, cy + 2, 9);
			if (ClientPrefs.get().favoriteEmojis.contains(e.name()) && !tab.equals("favorites")) {
				g.fill(cx + CELL - 3, cy + 1, cx + CELL - 1, cy + 3, Ui.MENTION);
			}
		}
		if (list.isEmpty()) {
			Component empty = Lang.tr(tab.equals("favorites") ? "picker.no_favorites" : "picker.no_recent");
			g.drawCenteredString(font, empty, x + WIDTH / 2, gy + ROWS * CELL / 2 - 4, Ui.MUTED);
		}
		// Barra de scroll.
		int max = maxScroll(list.size());
		if (max > 0) {
			int trackH = ROWS * CELL;
			int barH = Math.max(6, trackH * ROWS / (ROWS + max));
			int barY = gy + (trackH - barH) * scroll / max;
			Ui.round(g, x + WIDTH - 3, barY, x + WIDTH - 1, barY + barH, 1, 0x66FFFFFF);
		}

		// Pie: nombre del emoji bajo el ratón o de la pestaña.
		int fy = y + HEIGHT - PAD - 9;
		if (hovered != null) {
			g.drawString(font, hovered.code(), x + PAD, fy, Ui.TEXT, false);
			Component hint = Lang.tr("picker.fav_hint");
			int hw = font.width(hint);
			if (font.width(hovered.code()) + hw + 8 < WIDTH - PAD * 2) {
				g.drawString(font, hint, x + WIDTH - PAD - hw, fy, Ui.MUTED, false);
			}
		} else {
			Tab t = hoveredTab;
			if (t == null) {
				t = tabs.stream().filter(tt -> tt.id().equals(tab)).findFirst().orElse(null);
			}
			if (t != null) {
				g.drawString(font, Lang.tr(t.key()), x + PAD, fy, Ui.MUTED, false);
			}
		}
	}

	/** true si el clic cayó dentro del selector. */
	public boolean mouseClicked(double mx, double my, int button) {
		if (!contains(mx, my)) {
			return false;
		}
		int tx = x + PAD;
		for (Tab t : tabs) {
			if (Ui.inside(mx, my, tx, y + PAD, tx + TAB, y + PAD + TAB)) {
				tab = t.id();
				scroll = 0;
				return true;
			}
			tx += TAB + 1;
		}
		ClientState.ClientEmoji e = at(mx, my);
		if (e != null) {
			if (button == 1) {
				ClientPrefs.toggleFavorite(e.name());
			} else {
				ClientPrefs.useEmoji(e.name());
				onPick.accept(e);
			}
		}
		return true;
	}

	@Nullable
	private ClientState.ClientEmoji at(double mx, double my) {
		List<ClientState.ClientEmoji> list = current();
		int gx = x + PAD;
		int gy = y + PAD + TAB + 4;
		if (!Ui.inside(mx, my, gx, gy, gx + COLS * CELL, gy + ROWS * CELL)) {
			return null;
		}
		int col = (int) ((mx - gx) / CELL);
		int row = (int) ((my - gy) / CELL);
		int i = (row + scroll) * COLS + col;
		return i >= 0 && i < list.size() ? list.get(i) : null;
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		if (!contains(mx, my)) {
			return false;
		}
		scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll(current().size()));
		return true;
	}
}
