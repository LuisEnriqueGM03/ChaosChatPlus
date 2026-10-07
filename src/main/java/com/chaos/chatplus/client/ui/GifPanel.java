package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.GifCache;
import com.chaos.chatplus.gif.GifInfo;
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
 * Pestaña GIFs del selector: buscador, GIFs del servidor y de Tenor (destacados o resultados), todos en celdas del
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

	private final Runnable onSent;
	private String query = "";
	private String requested;
	private long editedAt;
	private int scroll;
	private int x;
	private int y;
	private int w;
	private int h;

	public GifPanel(Runnable onSent) {
		this.onSent = onSent;
		requestNow();
	}

	public static void accept(byte[] data) {
		try {
			JsonObject o = JsonParser.parseString(ChatPayloads.gunzip(data)).getAsJsonObject();
			resultQuery = o.get("query").getAsString();
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

	private void requestNow() {
		requested = query;
		received = false;
		if (ClientPlayNetworking.canSend(ChatPayloads.GifSearch.TYPE)) {
			ClientPlayNetworking.send(new ChatPayloads.GifSearch(query));
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
		List<GifInfo> out = new ArrayList<>(local);
		out.addAll(results);
		return out;
	}

	public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		if (!query.equals(requested) && Util.getMillis() - editedAt > DEBOUNCE_MS) {
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

		int gy = y + 16;
		int gh = h - 16;
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
		if (!received || !resultQuery.equals(requested)) {
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
		} else if (!ClientState.tenor()) {
			g.drawString(font, font.plainSubstrByWidth(Lang.tr("gif.no_tenor").getString(), w, false), x, fy, Ui.MUTED, false);
		} else {
			g.drawString(font, "Tenor", x + w - font.width("Tenor"), fy, Ui.MUTED, false);
		}
	}

	@Nullable
	private GifInfo at(double mx, double my) {
		int gy = y + 16;
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
		GifInfo gif = at(mx, my);
		if (gif != null && ClientPlayNetworking.canSend(ChatPayloads.SendGif.TYPE)) {
			ClientPlayNetworking.send(new ChatPayloads.SendGif(gif.id()));
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
