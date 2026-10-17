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

	private static final int COLS = 10;
	private static final int ROWS = 6;
	private static final int CELL = 19;
	private static final int TAB = 12;
	private static final int PAD = 4;
	public static final int WIDTH = COLS * CELL + PAD * 2;
	private static final int CONTENT_H = TAB + 4 + ROWS * CELL + 14 + PAD;
	/** Barra de arriba con las dos pestañas grandes: Emojis y GIFs. */
	private static final int MODE = 16;
	public static final int HEIGHT = CONTENT_H + MODE;

	/** Pestaña: id, emoji que hace de icono (o texto) y su nombre traducible. */
	private record Tab(String id, String icon, String key) {}

	private static final Map<String, String> CATEGORY_ICONS = new LinkedHashMap<>();

	static {
		CATEGORY_ICONS.put("caras", "sonrisa");
		CATEGORY_ICONS.put("animales", "gato");
		CATEGORY_ICONS.put("comida", "hamburguesa");
		CATEGORY_ICONS.put("naturaleza", "arbol");
		CATEGORY_ICONS.put("corazones", "corazon");
		CATEGORY_ICONS.put("gestos", "pulgar_arriba");
		CATEGORY_ICONS.put("simbolos", "fuego");
		CATEGORY_ICONS.put("objetos", "regalo");
		CATEGORY_ICONS.put("minecraft", "diamante");
		CATEGORY_ICONS.put("servidor", "corona");
	}

	private final Consumer<ClientState.ClientEmoji> onPick;
	@Nullable private final GifPanel gifs;
	private boolean gifMode;
	private int top;
	private final List<Tab> tabs = new ArrayList<>();
	private String tab;
	private int scroll;
	private int tabOffset;
	private int x;
	private int y;

	public EmojiPicker(Consumer<ClientState.ClientEmoji> onPick, Runnable onGifSent) {
		this.onPick = onPick;
		this.gifs = ClientState.gifs() ? new GifPanel(onGifSent, ClientState::dmTargetOrEmpty) : null;
		tabs.add(new Tab("recent", "◷", "picker.recent"));
		tabs.add(new Tab("favorites", "★", "picker.favorites"));
		ClientState.emojis().stream().map(ClientState.ClientEmoji::category).distinct().forEach(category -> {
			tabs.add(new Tab(category, CATEGORY_ICONS.getOrDefault(category, "corona"),
					CATEGORY_ICONS.containsKey(category) ? "category." + category : ""));
		});
		tab = ClientPrefs.get().recentEmojis.isEmpty() ? (tabs.size() > 2 ? tabs.get(2).id() : "recent") : "recent";
	}

	public void place(int right, int bottom) {
		x = right - WIDTH;
		top = bottom - HEIGHT;
		y = top + MODE;
	}

	public boolean contains(double mx, double my) {
		return Ui.inside(mx, my, x, top, x + WIDTH, top + HEIGHT);
	}

	/** Para la autoprueba. */
	public void showGifs() {
		gifMode = gifs != null;
	}

	/** El buscador de GIFs se queda las teclas mientras esa pestaña está abierta. */
	public boolean charTyped(char c) {
		return gifMode && gifs != null && gifs.charTyped(c);
	}

	public boolean keyPressed(int key) {
		return gifMode && gifs != null && gifs.keyPressed(key);
	}

	private void renderModes(GuiGraphics g, Font font, int mouseX, int mouseY) {
		int half = (WIDTH - PAD * 2) / (gifs == null ? 1 : 2);
		String[] keys = gifs == null ? new String[] {"picker.tab_emojis"} : new String[] {"picker.tab_emojis", "picker.tab_gifs"};
		for (int i = 0; i < keys.length; i++) {
			int tx = x + PAD + i * half;
			boolean sel = (i == 1) == gifMode;
			boolean hover = Ui.inside(mouseX, mouseY, tx, top + PAD, tx + half - 2, top + PAD + 12);
			if (sel) {
				Ui.round(g, tx, top + PAD, tx + half - 2, top + PAD + 12, 3, Ui.SELECTED);
			} else if (hover) {
				Ui.round(g, tx, top + PAD, tx + half - 2, top + PAD + 12, 3, Ui.HOVER);
			}
			Component label = Lang.tr(keys[i]);
			g.drawString(font, label, tx + (half - 2 - font.width(label)) / 2, top + PAD + 2, sel ? 0xFFFFFFFF : Ui.MUTED, false);
		}
	}

	private boolean clickModes(double mx, double my) {
		if (gifs == null) {
			return false;
		}
		int half = (WIDTH - PAD * 2) / 2;
		for (int i = 0; i < 2; i++) {
			int tx = x + PAD + i * half;
			if (Ui.inside(mx, my, tx, top + PAD, tx + half - 2, top + PAD + 12)) {
				gifMode = i == 1;
				return true;
			}
		}
		return false;
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
		Ui.roundBordered(g, x, top, x + WIDTH, top + HEIGHT, 5, Ui.POPUP, Ui.POPUP_BORDER);
		renderModes(g, font, mouseX, mouseY);
		if (gifMode && gifs != null) {
			gifs.render(g, font, x + PAD, y + 2, WIDTH - PAD * 2, CONTENT_H - PAD - 14, mouseX, mouseY);
			return;
		}

		// Pestañas.
		int tx = x + PAD;
		Tab hoveredTab = null;
		for (Tab t : visibleTabs()) {
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
				EmojiTextures.draw(g, icon, tx + 1, y + PAD + 1, 10);
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
			EmojiTextures.draw(g, e, cx + 1, cy + 1, 17);
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
		int fy = top + HEIGHT - PAD - 9;
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
				g.drawString(font, t.key().isEmpty() ? Component.literal(t.id()) : Lang.tr(t.key()), x + PAD, fy, Ui.MUTED, false);
			}
		}
	}

	/** true si el clic cayó dentro del selector. */
	public boolean mouseClicked(double mx, double my, int button) {
		if (!contains(mx, my)) {
			return false;
		}
		if (clickModes(mx, my)) {
			return true;
		}
		if (gifMode && gifs != null) {
			gifs.mouseClicked(mx, my, button);
			return true;
		}
		int tx = x + PAD;
		for (Tab t : visibleTabs()) {
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

	private List<Tab> visibleTabs() {
		int capacity = Math.max(1, (WIDTH - PAD * 2) / (TAB + 1));
		tabOffset = Mth.clamp(tabOffset, 0, Math.max(0, tabs.size() - capacity));
		return tabs.subList(tabOffset, Math.min(tabs.size(), tabOffset + capacity));
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		if (!contains(mx, my)) {
			return false;
		}
		if (!gifMode && my < y + PAD + TAB + 4) {
			tabOffset += amount > 0 ? -1 : 1;
			return true;
		}
		if (gifMode && gifs != null) {
			return gifs.mouseScrolled(amount);
		}
		scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll(current().size()));
		return true;
	}

	@Nullable
	public GifPanel gifsForTest() {
		return gifs;
	}
}
