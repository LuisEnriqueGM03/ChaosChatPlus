package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.AnimePrefetch;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiTextures;
import com.chaos.chatplus.client.GifCache;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifServer;
import com.chaos.chatplus.net.ChatPayloads;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Pestaña GIFs del selector: buscador, GIFs del servidor y de internet (destacados o resultados), todos en celdas del
 * mismo tamaño. Clic = se manda al chat.
 */
public final class GifPanel {

	private static final int COLS = 4;
	private static final int CELL_H = 30;
	private static final int GAP = 3;
	private static final long DEBOUNCE_MS = 450;

	/** Último resultado recibido (lo rellena el receptor de GifResults). */
	private static String resultQuery = "";
	private static List<GifInfo> local = List.of();
	private static List<GifInfo> results = List.of();
	private static boolean received;

	/** Categorías de la barra de arriba: búsqueda, emoji que hace de icono y nombre. */
	private record Chip(String query, String icon, String label) {}

	/** Icono de cada carpeta conocida (las demás usan la estrella). */
	private static final Map<String, String> ICONS = Map.of(
			"texto", "chat", "efectos", "brillos", "troll", "diablo", "emojis", "sonrisa",
			"animados", "creeper", "servidor", "corona");

	/** Categorías que mandó el servidor (sus subcarpetas de GIFs). */
	private static List<String> categories = List.of();

	private static List<Chip> chips() {
		List<Chip> out = new ArrayList<>();
		out.add(new Chip("", "fuego", Lang.tr("gif.all").getString()));
		for (String c : categories) {
			String label = c.isEmpty() ? c : Character.toUpperCase(c.charAt(0)) + c.substring(1);
			out.add(new Chip(GIF_CATEGORY + c, ICONS.getOrDefault(c, "estrella"), label));
		}
		if (ClientState.anime()) {
			out.add(new Chip(ANIME, "gato_feliz", Lang.tr("gif.anime").getString()));
		}
		return out;
	}

	/** Icono de cada categoría de anime (nekos.best). */
	private static final Map<String, String> ANIME_ICONS = Map.ofEntries(
			Map.entry("laugh", "jaja"), Map.entry("dance", "musica"), Map.entry("happy", "feliz"),
			Map.entry("smile", "sonrisa"), Map.entry("hug", "corazon"), Map.entry("wave", "saludo"),
			Map.entry("thumbsup", "pulgar_arriba"), Map.entry("cry", "llanto"), Map.entry("clap", "aplausos"),
			Map.entry("kiss", "beso"), Map.entry("angry", "furioso"), Map.entry("shocked", "sorpresa"),
			Map.entry("facepalm", "inexpresivo"), Map.entry("sleep", "dormido"));

	/** Segunda fila, con la pestaña Anime elegida: una categoría por cada reacción. */
	private static List<Chip> animeChips() {
		List<Chip> out = new ArrayList<>();
		for (String c : AnimePrefetch.TARGETS.keySet()) {
			out.add(new Chip(ANIME + ":" + c, ANIME_ICONS.getOrDefault(c, "estrella"), Lang.tr("gif.anime." + c).getString()));
		}
		return out;
	}

	private static final String GIF_CATEGORY = "@cat:";
	/** Pestaña Anime (GIFs de nekos.best guardados en este PC; no se piden al servidor). */
	private static final String ANIME = "@anime";
	private static final int CHIP = 11;
	private static final int SUB_CHIP = 10;
	/** Altura del buscador + la barra de categorías (la rejilla va debajo); con Anime, una fila más. */
	private int header() {
		return animeMode() ? 43 : 30;
	}

	private boolean animeMode() {
		return query.isEmpty() && chip.startsWith(ANIME);
	}

	private final Runnable onSent;
	/** A quién va el GIF ("" = a todos; con el modo privado, al jugador elegido). */
	private final java.util.function.Supplier<String> target;
	private String query = "";
	private String chip = "";
	private String requested;
	private long editedAt;
	private int scroll;
	private int x;
	private int y;
	private int w;
	private int h;

	public GifPanel(Runnable onSent, java.util.function.Supplier<String> target) {
		this.onSent = onSent;
		this.target = target;
		requestNow();
	}

	public static void accept(byte[] data) {
		try {
			JsonObject o = JsonParser.parseString(ChatPayloads.gunzip(data)).getAsJsonObject();
			resultQuery = o.get("query").getAsString();
			if (o.has("categories")) {
				List<String> cats = new ArrayList<>();
				o.getAsJsonArray("categories").forEach(c -> cats.add(c.getAsString()));
				categories = List.copyOf(cats);
			}
			local = list(o, "local");
			results = list(o, "results");
			received = true;
		} catch (Exception e) {
			local = List.of();
			results = List.of();
		}
	}

	private static List<GifInfo> list(JsonObject o, String key) {
		List<GifInfo> out = new ArrayList<>();
		if (o.has(key)) {
			for (JsonElement el : o.getAsJsonArray(key)) {
				out.add(GifInfo.fromJson(el.getAsJsonObject()));
			}
		}
		return out;
	}

	/** Lo que se busca: lo escrito, o si no hay nada, la categoría elegida. */
	private String effective() {
		return query.isEmpty() ? chip : query;
	}

	private void requestNow() {
		requested = effective();
		received = false;
		scroll = 0;
		if (animeMode()) {
			return; // los de anime ya están en este PC
		}
		if (ClientPlayNetworking.canSend(ChatPayloads.GifSearch.TYPE)) {
			ClientPlayNetworking.send(new ChatPayloads.GifSearch(requested));
		}
	}

	// ------------------------------------------------------------------ teclado (el buscador)

	public boolean charTyped(char c) {
		if (query.length() < 48 && c >= ' ' && c != 127) {
			query += c;
			edited();
		}
		return true;
	}

	public boolean keyPressed(int key) {
		if (key == 259 && !query.isEmpty()) { // borrar
			query = query.substring(0, query.length() - 1);
			edited();
			return true;
		}
		if (key == 257 || key == 335) { // Enter: buscar ya
			requestNow();
			return true;
		}
		return key != 256; // Esc lo cierra el selector
	}

	private void edited() {
		editedAt = Util.getMillis();
		scroll = 0;
	}

	// ------------------------------------------------------------------ dibujo

	private List<GifInfo> items() {
		if (animeMode()) {
			String cat = chip.length() > ANIME.length() + 1 ? chip.substring(ANIME.length() + 1) : "";
			return AnimePrefetch.gifs(cat);
		}
		List<GifInfo> out = new ArrayList<>(local);
		out.addAll(results);
		return out;
	}

	public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		if (!effective().equals(requested) && Util.getMillis() - editedAt > DEBOUNCE_MS) {
			requestNow();
		}
		// Buscador.
		Ui.roundBordered(g, x, y, x + w, y + 13, 3, 0xFF15171C, 0xFF3A3E48);
		boolean caret = (Util.getMillis() / 500) % 2 == 0;
		if (query.isEmpty()) {
			g.drawString(font, Lang.tr("gif.search"), x + 4, y + 3, 0xFF6C717B, false);
		} else {
			String shown = font.plainSubstrByWidth(query, w - 10, true);
			g.drawString(font, shown + (caret ? "_" : ""), x + 4, y + 3, Ui.TEXT, false);
		}

		// Categorías.
		Chip hoveredChip = null;
		List<Chip> chipList = chips();
		for (int i = 0; i < chipList.size(); i++) {
			Chip c = chipList.get(i);
			int cx = x + i * (CHIP + 1);
			if (cx + CHIP > x + w) {
				break;
			}
			boolean sel = query.isEmpty() && (c.query().equals(chip) || (c.query().equals(ANIME) && chip.startsWith(ANIME)));
			boolean hover = Ui.inside(mouseX, mouseY, cx, y + 16, cx + CHIP, y + 16 + CHIP);
			if (sel) {
				Ui.round(g, cx, y + 16, cx + CHIP, y + 16 + CHIP, 3, Ui.SELECTED);
			} else if (hover) {
				Ui.round(g, cx, y + 16, cx + CHIP, y + 16 + CHIP, 3, Ui.HOVER);
			}
			EmojiTextures.draw(g, c.icon(), true, cx + 1, y + 17, 9);
			if (hover) {
				hoveredChip = c;
			}
		}
		// Con Anime: segunda fila con sus categorías.
		if (animeMode()) {
			List<Chip> sub = animeChips();
			for (int i = 0; i < sub.size(); i++) {
				Chip c = sub.get(i);
				int cx = x + i * (SUB_CHIP + 1);
				if (cx + SUB_CHIP > x + w) {
					break;
				}
				boolean sel = c.query().equals(chip);
				boolean hover = Ui.inside(mouseX, mouseY, cx, y + 29, cx + SUB_CHIP, y + 29 + SUB_CHIP);
				if (sel) {
					Ui.round(g, cx, y + 29, cx + SUB_CHIP, y + 29 + SUB_CHIP, 3, 0x55734A9E);
				} else if (hover) {
					Ui.round(g, cx, y + 29, cx + SUB_CHIP, y + 29 + SUB_CHIP, 3, Ui.HOVER);
				}
				EmojiTextures.draw(g, c.icon(), true, cx + 1, y + 30, 8);
				if (hover) {
					hoveredChip = c;
				}
			}
		}

		int gy = y + header();
		int gh = h - header();
		List<GifInfo> list = items();
		int rows = (list.size() + COLS - 1) / COLS;
		int visibleRows = Math.max(1, gh / (CELL_H + GAP));
		scroll = Mth.clamp(scroll, 0, Math.max(0, rows - visibleRows));
		int cellW = (w - GAP * (COLS - 1)) / COLS;
		GifInfo hovered = null;
		g.enableScissor(x, gy, x + w, gy + gh);
		for (int i = scroll * COLS; i < list.size() && i < (scroll + visibleRows) * COLS; i++) {
			int col = i % COLS;
			int row = i / COLS - scroll;
			int cx = x + col * (cellW + GAP);
			int cy = gy + row * (CELL_H + GAP);
			boolean hover = Ui.inside(mouseX, mouseY, cx, cy, cx + cellW, cy + CELL_H);
			Ui.round(g, cx, cy, cx + cellW, cy + CELL_H, 3, hover ? 0x33FFFFFF : 0x18FFFFFF);
			GifCache.draw(g, list.get(i), cx + 2, cy + 2, cellW - 4, CELL_H - 4, true);
			if (hover) {
				hovered = list.get(i);
			}
		}
		g.disableScissor();

		String note = null;
		if (animeMode()) {
			if (list.isEmpty()) {
				note = AnimePrefetch.downloading()
						? Lang.tr("gif.anime_downloading", AnimePrefetch.count(), AnimePrefetch.target()).getString()
						: Lang.tr("gif.none").getString();
			}
		} else if (!received || !resultQuery.equals(requested)) {
			note = Lang.tr("gif.loading").getString();
		} else if (list.isEmpty()) {
			note = Lang.tr("gif.none").getString();
		}
		if (note != null) {
			g.drawCenteredString(font, note, x + w / 2, gy + gh / 2 - 4, Ui.MUTED);
		}
		// Pie: nombre del GIF o aviso de Tenor.
		int fy = y + h + 2;
		if (hovered != null) {
			g.drawString(font, font.plainSubstrByWidth(hovered.name(), w, false), x, fy, Ui.TEXT, false);
		} else if (hoveredChip != null) {
			g.drawString(font, hoveredChip.label(), x, fy, Ui.TEXT, false);
		} else if (animeMode()) {
			String info = Lang.tr("gif.anime_footer", AnimePrefetch.count(), AnimePrefetch.target()).getString();
			g.drawString(font, font.plainSubstrByWidth(info, w, false), x, fy, 0xFFD6B4FF, false);
		} else if (GifCache.pausedSeconds() > 0) {
			String wait = Lang.tr("gif.busy", (GifCache.pausedSeconds() + 59) / 60).getString();
			g.drawString(font, font.plainSubstrByWidth(wait, w, false), x, fy, Ui.WARN, false);
		} else {
			String via = ClientState.gifSource().isEmpty() ? Lang.tr("gif.server_only").getString()
					: Lang.tr("gif.server_only").getString() + " + " + ClientState.gifSource();
			g.drawString(font, font.plainSubstrByWidth(via, w, false), x, fy, Ui.MUTED, false);
		}
	}

	@Nullable
	private GifInfo at(double mx, double my) {
		int gy = y + header();
		int cellW = (w - GAP * (COLS - 1)) / COLS;
		List<GifInfo> list = items();
		for (int i = scroll * COLS; i < list.size(); i++) {
			int col = i % COLS;
			int row = i / COLS - scroll;
			int cx = x + col * (cellW + GAP);
			int cy = gy + row * (CELL_H + GAP);
			if (cy > y + h) {
				break;
			}
			if (Ui.inside(mx, my, cx, cy, cx + cellW, cy + CELL_H)) {
				return list.get(i);
			}
		}
		return null;
	}

	public boolean mouseClicked(double mx, double my) {
		List<Chip> chipList = chips();
		for (int i = 0; i < chipList.size(); i++) {
			int cx = x + i * (CHIP + 1);
			if (cx + CHIP > x + w) {
				break;
			}
			if (Ui.inside(mx, my, cx, y + 16, cx + CHIP, y + 16 + CHIP)) {
				String q = chipList.get(i).query();
				chip = q.equals(ANIME) ? ANIME + ":" + AnimePrefetch.TARGETS.keySet().iterator().next() : q;
				query = "";
				requestNow();
				return true;
			}
		}
		if (animeMode()) {
			List<Chip> sub = animeChips();
			for (int i = 0; i < sub.size(); i++) {
				int cx = x + i * (SUB_CHIP + 1);
				if (Ui.inside(mx, my, cx, y + 29, cx + SUB_CHIP, y + 29 + SUB_CHIP)) {
					chip = sub.get(i).query();
					scroll = 0;
					requested = chip;
					return true;
				}
			}
		}
		GifInfo gif = at(mx, my);
		if (gif != null && ClientPlayNetworking.canSend(ChatPayloads.SendGif.TYPE)) {
			ClientPlayNetworking.send(new ChatPayloads.SendGif(gif.id(), target.get()));
			onSent.run();
			return true;
		}
		return Ui.inside(mx, my, x, y, x + w, y + h);
	}

	public boolean mouseScrolled(double amount) {
		scroll = Math.max(0, scroll - (int) Math.signum(amount));
		return true;
	}

	public static void reset() {
		local = List.of();
		results = List.of();
		received = false;
		resultQuery = "";
	}
}
