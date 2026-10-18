package com.chaos.chatplus.client.ui;

import java.util.*;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Recipient selection owns its search, geometry and keyboard state. */
public final class DmPicker {
	private boolean dmPicker;
	private int dmX, dmY;
	private List<String> otherPlayers() {
		List<String> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() != null && mc.player != null) {
			for (var info : mc.getConnection().getOnlinePlayers()) {
				String name = info.getProfile().name();
				if (!name.equals(mc.player.getGameProfile().name())) {
					out.add(name);
				}
			}
		}
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}
	private static final int DM_ROW = 12;
	private static final int DM_W = 120;

	/** Cuántos se enseñan sin buscar (al azar) y como mucho al buscar. */
	private static final int DM_RANDOM = 5;
	private static final int DM_MAX = 8;
	/** Alto de la cabecera (título + buscador). */
	private static final int DM_HEAD = 30;
	private String dmSearch = "";
	/** Orden al azar, fijo mientras la lista está abierta. */
	private List<String> dmShuffled = List.of();

	public void toggle() {
		dmPicker = !dmPicker;
		dmSearch = "";
		List<String> all = new ArrayList<>(otherPlayers());
		java.util.Collections.shuffle(all);
		dmShuffled = all;
	}

	/** Sin buscar: 5 al azar (el elegido, si lo hay, primero). Buscando: los que coinciden. */
	private List<String> visiblePlayers() {
		List<String> online = otherPlayers();
		List<String> out = new ArrayList<>();
		if (dmSearch.isEmpty()) {
			String target = ClientState.dmTarget();
			if (target != null && online.contains(target)) {
				out.add(target);
			}
			for (String name : dmShuffled) {
				if (out.size() >= DM_RANDOM) {
					break;
				}
				if (online.contains(name) && !out.contains(name)) {
					out.add(name);
				}
			}
			return out;
		}
		String q = dmSearch.toLowerCase(java.util.Locale.ROOT);
		for (String name : online) {
			if (name.toLowerCase(java.util.Locale.ROOT).contains(q) && out.size() < DM_MAX) {
				out.add(name);
			}
		}
		return out;
	}

	private int dmHeight(List<String> players) {
		int extra = dmSearch.isEmpty() && otherPlayers().size() > players.size() ? DM_ROW : 0;
		return DM_HEAD + Math.max(1, players.size()) * DM_ROW + extra + 4;
	}

	public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, int screenWidth, int bottom) {
		List<String> players = visiblePlayers();
		int h = dmHeight(players);
		dmX = screenWidth - 4 - DM_W;
		dmY = bottom - 3 - h;
		Ui.roundBordered(g, dmX, dmY, dmX + DM_W, dmY + h, 5, Ui.POPUP, Ui.POPUP_BORDER);
		g.text(font, Lang.tr("dm.pick"), dmX + 6, dmY + 5, Ui.MUTED, false);
		// Buscador.
		Ui.roundBordered(g, dmX + 4, dmY + 15, dmX + DM_W - 4, dmY + 27, 3, 0xFF15171C, 0xFF3A3E48);
		if (dmSearch.isEmpty()) {
			g.text(font, Lang.tr("dm.search"), dmX + 7, dmY + 17, 0xFF6C717B, false);
		} else {
			boolean caret = (Util.getMillis() / 500) % 2 == 0;
			g.text(font, font.plainSubstrByWidth(dmSearch, DM_W - 16, true) + (caret ? "_" : ""), dmX + 7, dmY + 17,
					Ui.TEXT, false);
		}
		if (players.isEmpty()) {
			g.text(font, Lang.tr("dm.none"), dmX + 6, dmY + DM_HEAD + 2, Ui.MUTED, false);
			return;
		}
		int more = otherPlayers().size() - players.size();
		if (dmSearch.isEmpty() && more > 0) {
			g.text(font, Lang.tr("dm.more", more), dmX + 6, dmY + DM_HEAD + players.size() * DM_ROW + 2, Ui.MUTED, false);
		}
		for (int i = 0; i < players.size(); i++) {
			int y = dmY + DM_HEAD + i * DM_ROW;
			String name = players.get(i);
			boolean sel = name.equals(ClientState.dmTarget());
			if (sel) {
				Ui.round(g, dmX + 2, y, dmX + DM_W - 2, y + DM_ROW, 3, 0x55734A9E);
			} else if (Ui.inside(mouseX, mouseY, dmX + 2, y, dmX + DM_W - 2, y + DM_ROW)) {
				Ui.round(g, dmX + 2, y, dmX + DM_W - 2, y + DM_ROW, 3, Ui.HOVER);
			}
			g.text(font, "✉ " + name, dmX + 6, y + 2, sel ? 0xFFFFFFFF : 0xFFD6B4FF, false);
		}
	}

	public boolean click(double mx, double my) {
		List<String> players = visiblePlayers();
		int h = dmHeight(players);
		if (!Ui.inside(mx, my, dmX, dmY, dmX + DM_W, dmY + h)) {
			return false;
		}
		for (int i = 0; i < players.size(); i++) {
			int y = dmY + DM_HEAD + i * DM_ROW;
			if (Ui.inside(mx, my, dmX + 2, y, dmX + DM_W - 2, y + DM_ROW)) {
				String name = players.get(i);
				ClientState.setDmTarget(name.equals(ClientState.dmTarget()) ? null : name);
				dmPicker = false;
			}
		}
		return true;
	}

	public boolean isOpen() { return dmPicker; }
	public void close() { dmPicker = false; }
	public boolean contains(double mx, double my) {
		return dmPicker && Ui.inside(mx, my, dmX, dmY, dmX + DM_W, dmY + dmHeight(visiblePlayers()));
	}
	public boolean keyPressed(int key) {
		if (!dmPicker) return false;
		if (key == 256) close();
		else if (key == 259 && !dmSearch.isEmpty()) dmSearch = dmSearch.substring(0, dmSearch.length() - 1);
		else if (key == 257 || key == 335) {
			List<String> players = visiblePlayers();
			if (!players.isEmpty()) { ClientState.setDmTarget(players.get(0)); close(); }
		}
		return true;
	}
	public boolean charTyped(char c) {
		if (!dmPicker) return false;
		if (dmSearch.length() < 16 && (Character.isLetterOrDigit(c) || c == '_')) dmSearch += c;
		return true;
	}
}
