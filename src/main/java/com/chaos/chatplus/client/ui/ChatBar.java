package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiTextures;
import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.screen.ItemPickerScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

/**
 * La barra de escribir (tecla T) con el diseño moderno: barra redondeada, tu prefijo de rango a la izquierda
 * (con Chaos Ranks), botones de emojis, mostrar ítem y captura a la derecha, contador de letras, texto de ayuda,
 * autocompletado y selector de emojis.
 */
public final class ChatBar {

	private static final int ICON = 8;
	private static final int ICON_GAP = 4;
	private static final int MAX_LENGTH = 256;

	private record Button(String id, String emoji, String tooltipKey, Runnable action) {}

	private final ChatScreen screen;
	private final EditBox input;
	private final Suggest suggest;
	@Nullable private EmojiPicker picker;
	private final List<Button> buttons = new ArrayList<>();

	private int barTop;
	private int barBottom;
	private int iconsX;
	private int counterX;

	public ChatBar(ChatScreen screen, EditBox input) {
		this.screen = screen;
		this.input = input;
		this.suggest = new Suggest(input);
		if (ClientState.serverMod() && !ClientState.emojis().isEmpty()) {
			buttons.add(new Button("emoji", "sonrisa", "bar.emojis", this::togglePicker));
		}
		if (ClientState.items()) {
			buttons.add(new Button("item", "espada", "bar.item", () ->
					Minecraft.getInstance().setScreen(new ItemPickerScreen(screen))));
		}
		if (ClientState.shots()) {
			buttons.add(new Button("shot", "camara", "bar.shot", ShotCapture::start));
		}
		layout();
	}

	/** Coloca el campo de texto entre el prefijo y los botones. Se llama en cada frame (el prefijo cambia con "/"). */
	public void layout() {
		int h = screen.height;
		barTop = h - 16;
		barBottom = h - 2;
		Font font = Minecraft.getInstance().font;
		int counterW = font.width(MAX_LENGTH + "/" + MAX_LENGTH);
		counterX = screen.width - 6 - counterW;
		iconsX = counterX - 6 - buttons.size() * (ICON + ICON_GAP);
		int left = 7 + prefixWidth(font);
		int x = left;
		int width = Math.max(20, iconsX - 4 - x);
		if (input.getX() != x) {
			input.setX(x);
		}
		if (input.getWidth() != width) {
			input.setWidth(width);
		}
		if (input.getY() != h - 13) {
			input.setY(h - 13);
		}
	}

	private boolean commandMode() {
		return input.getValue().startsWith("/");
	}

	@Nullable
	private Component prefix() {
		if (commandMode()) {
			return null;
		}
		Component p = ClientState.prefix();
		if (p == null && Minecraft.getInstance().player != null) {
			p = Component.literal(Minecraft.getInstance().player.getGameProfile().getName()).withColor(0xC9CDD6);
		}
		return p;
	}

	private int prefixWidth(Font font) {
		if (commandMode()) {
			return font.width("/") + 8;
		}
		Component p = prefix();
		return p == null ? 0 : font.width(p) + 5;
	}

	/** Fondo de la barra, prefijo, botones y contador (va donde vanilla pintaba su franja negra). */
	public void renderBar(GuiGraphics g, int mouseX, int mouseY) {
		layout();
		Font font = Minecraft.getInstance().font;
		int bg = Minecraft.getInstance().options.getBackgroundColor(Integer.MIN_VALUE);
		int alpha = Math.max(0x99, bg >>> 24);
		Ui.round(g, 2, barTop, screen.width - 2, barBottom, 5, Ui.alpha(Ui.PANEL, alpha));
		int textY = input.getY() + 2;

		if (commandMode()) {
			int w = font.width("/") + 6;
			Ui.round(g, 5, barTop + 2, 5 + w, barBottom - 2, 3, 0x447AA2FF);
			g.drawString(font, "/", 8, textY, Ui.ACCENT, false);
		} else {
			Component p = prefix();
			if (p != null) {
				g.drawString(font, p, 7, textY, 0xFFFFFFFF, true);
			}
		}

		if (input.getValue().isEmpty()) {
			g.drawString(font, Lang.tr("bar.placeholder"), input.getX(), textY, 0xFF6C717B, false);
		}

		// Botones.
		Button hovered = null;
		int x = iconsX;
		for (Button b : buttons) {
			int iy = barTop + (barBottom - barTop - ICON) / 2;
			boolean hover = Ui.inside(mouseX, mouseY, x - 2, iy - 2, x + ICON + 2, iy + ICON + 2);
			boolean active = b.id().equals("emoji") && picker != null;
			if (hover || active) {
				Ui.round(g, x - 2, iy - 2, x + ICON + 2, iy + ICON + 2, 3, active ? Ui.SELECTED : Ui.HOVER);
			}
			EmojiTextures.draw(g, b.emoji(), true, x, iy, ICON);
			if (hover) {
				hovered = b;
			}
			x += ICON + ICON_GAP;
		}

		// Contador de letras.
		int len = input.getValue().length();
		int color = len >= 240 ? Ui.ERROR : len >= 200 ? Ui.WARN : 0xFF5C616B;
		String counter = len + "/" + MAX_LENGTH;
		g.drawString(font, counter, screen.width - 6 - font.width(counter), textY, color, false);

		if (hovered != null) {
			g.renderTooltip(font, Lang.tr(hovered.tooltipKey()), mouseX, mouseY);
		}
	}

	/** Popups encima de todo: autocompletado y selector. */
	public void renderOverlays(GuiGraphics g, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		if (picker != null) {
			picker.place(screen.width - 4, barTop - 3);
			picker.render(g, font, mouseX, mouseY);
		} else {
			suggest.render(g, font, barTop, mouseX, mouseY);
		}
		g.pose().popPose();
	}

	public void onEdited() {
		suggest.update();
	}

	public void togglePicker() {
		picker = picker == null ? new EmojiPicker(this::insertEmoji) : null;
	}

	private void insertEmoji(ClientState.ClientEmoji e) {
		String value = input.getValue();
		int cursor = Math.min(input.getCursorPosition(), value.length());
		String before = cursor > 0 && !Character.isWhitespace(value.charAt(cursor - 1)) ? " " : "";
		input.insertText(before + e.code() + " ");
	}

	public boolean keyPressed(int key) {
		if (picker != null && key == 256) {
			picker = null;
			return true;
		}
		if (picker == null && suggest.keyPressed(key)) {
			return true;
		}
		return false;
	}

	public boolean mouseClicked(double mx, double my, int button) {
		if (picker != null && picker.mouseClicked(mx, my, button)) {
			return true;
		}
		if (picker == null && suggest.mouseClicked(mx, my)) {
			return true;
		}
		int x = iconsX;
		for (Button b : buttons) {
			int iy = barTop + (barBottom - barTop - ICON) / 2;
			if (Ui.inside(mx, my, x - 2, iy - 2, x + ICON + 2, iy + ICON + 2)) {
				if (!b.id().equals("emoji")) {
					picker = null;
				}
				b.action().run();
				return true;
			}
			x += ICON + ICON_GAP;
		}
		if (picker != null) {
			picker = null; // clic fuera: se cierra
		}
		return false;
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		return picker != null && picker.mouseScrolled(mx, my, amount);
	}
}
