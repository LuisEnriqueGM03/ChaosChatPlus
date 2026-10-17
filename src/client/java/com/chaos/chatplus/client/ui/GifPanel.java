package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientPrefs;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiTextures;
import com.chaos.chatplus.client.GifCache;
import com.chaos.chatplus.client.KlipyClient;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.gif.OnlineGifs;
import com.chaos.chatplus.net.ChatPayloads;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.chaos.chatplus.client.ClientNetwork;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * Pestaña GIFs del selector: buscador y categorías. Primero «Custom» (Klipy en vivo, si el servidor tiene clave),
 * luego «Favoritos» (guardados en este PC) y los GIFs propios del servidor por carpeta. Clic = se manda al chat;
 * clic derecho = favorito.
 */
public final class GifPanel {

	private static final int COLS = 4;
	private static final int CELL_H = 30;
	private static final int GAP = 3;
	private static final long DEBOUNCE_MS = 450;
	private static final long KLIPY_DEBOUNCE_MS = 900;
	private static final int HEADER = 30;
	private static final int CHIP = 11;
	private static final int FAV_COLOR = 0xFFFFD54F;

	/** Último resultado recibido (lo rellena el receptor de GifResults). */
	private static String resultQuery = "";
	private static List<GifInfo> local = List.of();
	private static List<GifInfo> results = List.of();
	private static boolean received;
	/** Klipy cortado por un tope: "hour" (del servidor) o "player" (tuyo, por minuto). */
	private static String limited = "";

	/** Categorías de la barra de arriba: búsqueda, emoji que hace de icono y nombre. */
	private record Chip(String query, String icon, String label) {}

	/** Icono de cada carpeta conocida (las demás usan las estrellas). */
	private static final Map<String, String> ICONS = Map.of(
			"texto", "chat", "efectos", "brillos", "troll", "diablo", "emojis", "sonrisa",
			"animados", "creeper", "servidor", "corona", "anime", "gato_feliz");

	/** Categorías que mandó el servidor (sus subcarpetas de GIFs). */
	private static List<String> categories = List.of();

	private static final String GIF_CATEGORY = GifRegistry.CATEGORY_QUERY;
	/** Pestaña Custom: búsqueda en Klipy. */
	private static final String CUSTOM = "@klipy:";
	/** Pestaña Favoritos: sale de las preferencias de este PC, no se pide nada al servidor. */
	private static final String FAVORITES = "@fav";

	private static List<Chip> chips() {
		List<Chip> out = new ArrayList<>();
		out.add(new Chip(CUSTOM, "arcoiris", Lang.tr("gif.custom").getString()));
		out.add(new Chip(FAVORITES, "estrella", Lang.tr("gif.favorites").getString()));
		out.add(new Chip("", "fuego", Lang.tr("gif.all").getString()));
		for (String c : categories) {
			String label = c.isEmpty() ? c : Character.toUpperCase(c.charAt(0)) + c.substring(1);
			out.add(new Chip(GIF_CATEGORY + c, ICONS.getOrDefault(c, "estrellas"), label));
		}
		return out;
	}

	private final Runnable onSent;
	/** A quién va el GIF ("" = a todos; con el modo privado, al jugador elegido). */
	private final java.util.function.Supplier<String> target;
	private String query = "";
	/** Pestaña elegida; al abrir, «Todos». */
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
			limited = o.has("limited") ? o.get("limited").getAsString() : "";
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

	private boolean customMode() {
		return chip.equals(CUSTOM);
	}

	private boolean favoritesMode() {
		return chip.equals(FAVORITES);
	}

	/** ¿El servidor tiene Klipy configurado? */
	private static boolean klipy() {
		return !ClientState.klipyKey().isEmpty();
	}

	/** Lo que se pide al servidor: en Custom, «@klipy:» + lo escrito; si no, lo escrito o la categoría elegida. */
	private String effective() {
		if (customMode()) {
			return CUSTOM + query;
		}
		if (favoritesMode()) {
			return FAVORITES;
		}
		return query.isEmpty() ? chip : query;
	}

	/** Las pestañas que no preguntan al servidor (Favoritos, o Custom sin Klipy). */
	private boolean offline() {
		return favoritesMode() || (customMode() && !klipy());
	}

	private void requestNow() {
		requested = effective();
		received = false;
		scroll = 0;
		if (customMode()) {
			// Klipy se busca desde este juego, directo a Klipy (como pide Klipy), y se muestra tal cual llega.
			if (klipy()) {
				String asked = requested;
				KlipyClient.search(query, r -> {
					if (asked.equals(requested)) {
						local = List.of();
						results = r.gifs();
						limited = r.limited() ? "player" : "";
						resultQuery = asked;
						received = true;
					}
				});
			}
			return;
		}
		if (!offline() && ClientNetwork.canSend(ChatPayloads.GifSearch.TYPE)) {
			ClientNetwork.send(new ChatPayloads.GifSearch(requested));
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
		if (favoritesMode()) {
			String q = query.strip().toLowerCase(Locale.ROOT);
			List<GifInfo> out = new ArrayList<>();
			for (GifInfo g : ClientPrefs.get().favoriteGifs) {
				// Solo los propios: los de Klipy van en su pestaña, sin mezclarse con otros.
				if (!g.id().startsWith(OnlineGifs.KLIPY) && (q.isEmpty() || g.name().toLowerCase(Locale.ROOT).contains(q))) {
					out.add(g);
				}
			}
			return out;
		}
		if (customMode() && !klipy()) {
			return List.of();
		}
		List<GifInfo> out = new ArrayList<>(local);
		out.addAll(results);
		return out;
	}

	private boolean selected(Chip c) {
		if (!c.query().equals(chip)) {
			return false;
		}
		// En Custom y Favoritos el buscador busca dentro de la pestaña; en las demás, en todos los GIFs propios.
		return query.isEmpty() || customMode() || favoritesMode();
	}

	public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		long debounce = customMode() ? KLIPY_DEBOUNCE_MS : DEBOUNCE_MS;
		if (!effective().equals(requested) && Util.getMillis() - editedAt > debounce
				&& (!customMode() || query.isEmpty() || query.strip().length() >= 2)) {
			requestNow();
		}
		// Buscador.
		Ui.roundBordered(g, x, y, x + w, y + 13, 3, 0xFF15171C, customMode() ? 0xFF5A4A8E : 0xFF3A3E48);
		boolean caret = (Util.getMillis() / 500) % 2 == 0;
		if (query.isEmpty()) {
			g.drawString(font, Lang.tr(customMode() ? "gif.search_klipy" : "gif.search"), x + 4, y + 3, 0xFF6C717B, false);
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
			boolean hover = Ui.inside(mouseX, mouseY, cx, y + 16, cx + CHIP, y + 16 + CHIP);
			if (selected(c)) {
				Ui.round(g, cx, y + 16, cx + CHIP, y + 16 + CHIP, 3, Ui.SELECTED);
			} else if (hover) {
				Ui.round(g, cx, y + 16, cx + CHIP, y + 16 + CHIP, 3, Ui.HOVER);
			}
			EmojiTextures.draw(g, c.icon(), true, cx + 1, y + 17, 9);
			if (hover) {
				hoveredChip = c;
			}
			// Separador tras Custom y Favoritos (las pestañas que no son carpetas).
			if (i == 1) {
				g.fill(cx + CHIP + 1, y + 18, cx + CHIP + 2, y + 16 + CHIP - 2, 0xFF3A3E48);
			}
		}

		int gy = y + HEADER;
		int gh = h - HEADER;
		List<GifInfo> list = items();
		int rows = (list.size() + COLS - 1) / COLS;
		int visibleRows = Math.max(1, gh / (CELL_H + GAP));
		scroll = Mth.clamp(scroll, 0, Math.max(0, rows - visibleRows));
		int cellW = (w - GAP * (COLS - 1)) / COLS;
		GifInfo hovered = null;
		g.enableScissor(x, gy, x + w, gy + gh);
		for (int i = scroll * COLS; i < list.size() && i < (scroll + visibleRows) * COLS; i++) {
			GifInfo gif = list.get(i);
			int col = i % COLS;
			int row = i / COLS - scroll;
			int cx = x + col * (cellW + GAP);
			int cy = gy + row * (CELL_H + GAP);
			boolean hover = Ui.inside(mouseX, mouseY, cx, cy, cx + cellW, cy + CELL_H);
			Ui.round(g, cx, cy, cx + cellW, cy + CELL_H, 3, hover ? 0x33FFFFFF : 0x18FFFFFF);
			GifCache.draw(g, gif, cx + 2, cy + 2, cellW - 4, CELL_H - 4, true);
			if (ClientPrefs.isFavoriteGif(gif.id())) {
				g.pose().pushPose();
				g.pose().translate(0, 0, 200);
				g.drawString(font, "★", cx + cellW - 8, cy + 1, FAV_COLOR, true);
				g.pose().popPose();
			}
			if (hover) {
				hovered = gif;
			}
		}
		g.disableScissor();

		if (customMode() && !klipy()) {
			// Sin clave de Klipy: aviso y cómo activarlo.
			int cy = gy + gh / 2 - 12;
			g.drawCenteredString(font, Lang.tr("gif.klipy_off"), x + w / 2, cy, Ui.TEXT);
			List<FormattedCharSequence> hint = font.split(Lang.tr("gif.klipy_hint"), w - 8);
			for (int i = 0; i < hint.size() && i < 4; i++) {
				g.drawCenteredString(font, hint.get(i), x + w / 2, cy + 13 + i * 10, Ui.MUTED);
			}
		} else {
			String note = null;
			if (favoritesMode()) {
				if (list.isEmpty()) {
					note = Lang.tr(query.isEmpty() ? "gif.fav_empty" : "gif.none").getString();
				}
			} else if (!received || !resultQuery.equals(requested)) {
				note = Lang.tr("gif.loading").getString();
			} else if (customMode() && !limited.isEmpty() && list.isEmpty()) {
				note = Lang.tr("gif.limit_" + limited).getString();
			} else if (list.isEmpty()) {
				note = Lang.tr("gif.none").getString();
			}
			if (note != null) {
				List<FormattedCharSequence> lines = font.split(net.minecraft.network.chat.Component.literal(note), w - 8);
				for (int i = 0; i < lines.size(); i++) {
					g.drawCenteredString(font, lines.get(i), x + w / 2, gy + gh / 2 - 4 - (lines.size() - 1) * 5 + i * 10, Ui.MUTED);
				}
			}
		}
		// Pie: nombre del GIF (con la ayuda del favorito), de la pestaña, o de dónde salen.
		int fy = y + h + 2;
		if (hovered != null) {
			String hint = hovered.id().startsWith(OnlineGifs.KLIPY) ? "" : " · " + Lang.tr("gif.fav_hint").getString();
			String name = font.plainSubstrByWidth(hovered.name(), Math.max(20, w - font.width(hint)), false);
			g.drawString(font, name, x, fy, Ui.TEXT, false);
			if (font.width(name + hint) <= w) {
				g.drawString(font, hint, x + font.width(name), fy, Ui.MUTED, false);
			}
		} else if (hoveredChip != null) {
			g.drawString(font, hoveredChip.label(), x, fy, Ui.TEXT, false);
		} else if (GifCache.pausedSeconds() > 0) {
			String wait = Lang.tr("gif.busy", (GifCache.pausedSeconds() + 59) / 60).getString();
			g.drawString(font, font.plainSubstrByWidth(wait, w, false), x, fy, Ui.WARN, false);
		} else if (customMode() && !limited.isEmpty() && received) {
			String warn = Lang.tr("gif.limit_" + limited).getString();
			g.drawString(font, font.plainSubstrByWidth(warn, w, false), x, fy, Ui.WARN, false);
		} else if (customMode()) {
			if (klipy()) {
				g.drawString(font, Lang.tr("gif.powered_klipy"), x, fy, 0xFFB9A6FF, false);
			}
		} else {
			g.drawString(font, font.plainSubstrByWidth(Lang.tr("gif.server_only").getString(), w, false), x, fy, Ui.MUTED, false);
		}
	}

	@Nullable
	private GifInfo at(double mx, double my) {
		int gy = y + HEADER;
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

	public boolean mouseClicked(double mx, double my, int button) {
		List<Chip> chipList = chips();
		for (int i = 0; i < chipList.size(); i++) {
			int cx = x + i * (CHIP + 1);
			if (cx + CHIP > x + w) {
				break;
			}
			if (Ui.inside(mx, my, cx, y + 16, cx + CHIP, y + 16 + CHIP)) {
				chip = chipList.get(i).query();
				query = "";
				requestNow();
				return true;
			}
		}
		GifInfo gif = at(mx, my);
		if (gif != null && button == 1) {
			if (!gif.id().startsWith(OnlineGifs.KLIPY)) {
				ClientPrefs.toggleFavoriteGif(gif);
			}
			return true;
		}
		if (gif != null && button == 0 && ClientNetwork.canSend(ChatPayloads.SendGif.TYPE)) {
			String info = gif.id().startsWith(OnlineGifs.KLIPY) ? gif.encode() : "";
			ClientNetwork.send(new ChatPayloads.SendGif(gif.id(), info, target.get()));
			onSent.run();
			return true;
		}
		return Ui.inside(mx, my, x, y, x + w, y + h);
	}

	public boolean mouseScrolled(double amount) {
		scroll = Math.max(0, scroll - (int) Math.signum(amount));
		return true;
	}

	/** Autoprueba: "custom", "fav" (marca el primero visible y abre Favoritos) o "" (Todos). */
	public void tabForTest(String which) {
		if (which.equals("fav")) {
			List<GifInfo> list = items();
			if (!list.isEmpty() && !ClientPrefs.isFavoriteGif(list.get(0).id())) {
				ClientPrefs.toggleFavoriteGif(list.get(0));
			}
		}
		if (which.equals("send")) {
			List<GifInfo> list = items();
			if (!list.isEmpty()) {
				GifInfo gif = list.get(0);
				String info = gif.id().startsWith(OnlineGifs.KLIPY) ? gif.encode() : "";
				ClientNetwork.send(new ChatPayloads.SendGif(gif.id(), info, target.get()));
				onSent.run();
			}
			return;
		}
		query = "";
		if (which.startsWith("custom")) {
			chip = CUSTOM;
			query = which.startsWith("custom:") ? which.substring(7) : "";
		} else if (which.startsWith("cat:")) {
			chip = GIF_CATEGORY + which.substring(4);
		} else {
			chip = which.equals("fav") ? FAVORITES : "";
		}
		requestNow();
	}

	public static void reset() {
		local = List.of();
		results = List.of();
		received = false;
		resultQuery = "";
	}
}
