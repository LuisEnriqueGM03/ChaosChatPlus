package com.chaos.chatplus.client.ui;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.client.ClientPrefs;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Autocompletado mientras escribes: ":fu" → emojis, "@lu" → jugadores conectados (y @everyone/@here/@rango si el
 * servidor te deja). Tab o Enter para aceptar, flechas para elegir, Esc para cerrar.
 */
public final class Suggest {

	private static final int MAX = 8;
	private static final int ROW = 12;

	private record Entry(String insert, String label, @Nullable ClientState.ClientEmoji emoji) {}

	private final EditBox input;
	private final List<Entry> entries = new ArrayList<>();
	private int start = -1;
	private int selected;
	private String dismissedFor = null;

	private int boxX;
	private int boxY;
	private int boxW;

	public Suggest(EditBox input) {
		this.input = input;
	}

	public boolean visible() {
		return !entries.isEmpty();
	}

	/** Recalcula las sugerencias según el texto antes del cursor. */
	public void update() {
		entries.clear();
		start = -1;
		String value = input.getValue();
		if (value.startsWith("/")) {
			return;
		}
		int cursor = Math.min(input.getCursorPosition(), value.length());
		int i = cursor - 1;
		while (i >= 0 && isNameChar(value.charAt(i))) {
			i--;
		}
		if (i < 0 || (value.charAt(i) != ':' && value.charAt(i) != '@')) {
			return;
		}
		if (i > 0 && !Character.isWhitespace(value.charAt(i - 1)) && value.charAt(i) == '@') {
			return; // correo@algo: no es una mención
		}
		char kind = value.charAt(i);
		String query = value.substring(i + 1, cursor).toLowerCase(Locale.ROOT);
		String key = value + "|" + cursor;
		if (key.equals(dismissedFor)) {
			return;
		}
		if (kind == ':') {
			if (query.length() < 2) {
				return;
			}
			emojis(query);
		} else if (ClientState.mentions()) {
			players(query);
		}
		if (!entries.isEmpty()) {
			start = i;
			selected = Math.min(selected, entries.size() - 1);
		}
	}

	private void emojis(String query) {
		List<ClientState.ClientEmoji> starts = new ArrayList<>();
		List<ClientState.ClientEmoji> contains = new ArrayList<>();
		for (ClientState.ClientEmoji e : ClientState.emojis()) {
			if (e.matches(query)) {
				starts.add(e);
			} else if (e.name().contains(query)) {
				contains.add(e);
			}
		}
		// Primero los usados hace poco.
		List<String> recent = ClientPrefs.get().recentEmojis;
		starts.sort((a, b) -> Integer.compare(rank(recent, a.name()), rank(recent, b.name())));
		starts.addAll(contains);
		for (ClientState.ClientEmoji e : starts) {
			if (entries.size() >= MAX) {
				break;
			}
			entries.add(new Entry(e.code() + " ", e.code(), e));
		}
	}

	private static int rank(List<String> recent, String name) {
		int i = recent.indexOf(name);
		return i < 0 ? Integer.MAX_VALUE : i;
	}

	private void players(String query) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		List<String> names = new ArrayList<>();
		if (connection != null) {
			for (PlayerInfo info : connection.getOnlinePlayers()) {
				names.add(info.getProfile().name());
			}
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		names.addAll(ClientState.specialMentions());
		for (String name : names) {
			if (entries.size() >= MAX) {
				break;
			}
			if (name.toLowerCase(Locale.ROOT).startsWith(query)) {
				entries.add(new Entry("@" + name + " ", "@" + name, null));
			}
		}
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '+' || c == '-';
	}

	public boolean keyPressed(int key) {
		if (!visible()) {
			return false;
		}
		switch (key) {
			case InputConstants.KEY_TAB, InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> { // Tab, Enter
				accept(selected);
				return true;
			}
			case InputConstants.KEY_UP -> { // arriba
				selected = (selected + 1) % entries.size();
				return true;
			}
			case InputConstants.KEY_DOWN -> { // abajo
				selected = (selected - 1 + entries.size()) % entries.size();
				return true;
			}
			case InputConstants.KEY_ESCAPE -> { // Esc
				dismissedFor = input.getValue() + "|" + input.getCursorPosition();
				entries.clear();
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	public boolean contains(double mx, double my) {
		if (!visible()) {
			return false;
		}
		int h = entries.size() * ROW + 4;
		return Ui.inside(mx, my, boxX - 1, boxY - 3, boxX + boxW + 1, boxY + h - 1);
	}

	public boolean mouseClicked(double mx, double my) {
		if (!visible()) {
			return false;
		}
		for (int i = 0; i < entries.size(); i++) {
			int y = rowY(i);
			if (Ui.inside(mx, my, boxX, y, boxX + boxW, y + ROW)) {
				accept(i);
				return true;
			}
		}
		return false;
	}

	private void accept(int index) {
		Entry e = entries.get(index);
		String value = input.getValue();
		int cursor = Math.min(input.getCursorPosition(), value.length());
		String next = value.substring(0, start) + e.insert() + value.substring(cursor);
		input.setValue(next);
		input.setCursorPosition(start + e.insert().length());
		input.setHighlightPos(input.getCursorPosition());
		if (e.emoji() != null) {
			ClientPrefs.useEmoji(e.emoji().name());
		}
		entries.clear();
		selected = 0;
	}

	private int rowY(int i) {
		// La fila 0 (la elegida por defecto) es la de abajo, pegada a la barra.
		return boxY + (entries.size() - 1 - i) * ROW;
	}

	public void render(GuiGraphicsExtractor g, Font font, int barTop, int mouseX, int mouseY) {
		if (!visible()) {
			return;
		}
		int w = 0;
		for (Entry e : entries) {
			w = Math.max(w, font.width(e.label()) + (e.emoji() != null ? 14 : 0));
		}
		boxW = w + 10;
		boxX = Math.max(2, Math.min(input.getScreenX(start) - 6, g.guiWidth() - boxW - 2));
		int h = entries.size() * ROW + 4;
		boxY = barTop - h - 2 + 2;
		Ui.roundBordered(g, boxX - 1, boxY - 3, boxX + boxW + 1, boxY + h - 1, 4, Ui.POPUP, Ui.POPUP_BORDER);
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			int y = rowY(i);
			boolean hover = Ui.inside(mouseX, mouseY, boxX, y, boxX + boxW, y + ROW);
			if (i == selected) {
				Ui.round(g, boxX + 1, y, boxX + boxW - 1, y + ROW, 3, Ui.SELECTED);
			} else if (hover) {
				Ui.round(g, boxX + 1, y, boxX + boxW - 1, y + ROW, 3, Ui.HOVER);
			}
			int x = boxX + 4;
			if (e.emoji() != null) {
				EmojiTextures.draw(g, e.emoji(), x, y + 2, 8);
				x += 12;
			}
			int color = e.emoji() == null ? (0xFF000000 | ClientState.mentionColor()) : Ui.TEXT;
			g.text(font, e.label(), x, y + 2, i == selected ? 0xFFFFFFFF : color, false);
		}
	}
}
