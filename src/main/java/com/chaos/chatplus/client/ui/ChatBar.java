package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.EmojiInput;
import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.screen.ItemPickerScreen;
import com.chaos.chatplus.mixin.client.EditBoxAccessor;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * La barra de escribir (tecla T) con el diseño moderno: barra redondeada, tu prefijo de rango a la izquierda
 * (con Chaos Ranks), botones en blanco y negro con marco gris (emojis, mostrar ítem, captura y enviar), aviso al
 * llegar al límite de letras, emojis dentro del texto, autocompletado y selector de emojis.
 */
public final class ChatBar {

	private static final int ICON = 8;
	private static final int BOX = 12;
	private static final int GAP = 3;
	private static final int MAX_LENGTH = 256;
	private static final long LIMIT_NOTICE_MS = 2500;

	private record Button(String icon, String tooltipKey, Runnable action) {}

	private final ChatScreen screen;
	private final EditBox input;
	private final Suggest suggest;
	@Nullable private EmojiPicker picker;
	private final List<Button> buttons = new ArrayList<>();
	private boolean converting;
	private long limitAt;

	private int barTop;
	private int barBottom;
	private int buttonsX;

	public ChatBar(ChatScreen screen, EditBox input) {
		this.screen = screen;
		this.input = input;
		this.suggest = new Suggest(input);
		if (ClientState.serverMod() && !ClientState.emojis().isEmpty()) {
			buttons.add(new Button("btn_emoji", "bar.emojis", this::togglePicker));
		}
		if (ClientState.items()) {
			buttons.add(new Button("btn_item", "bar.item", () ->
					Minecraft.getInstance().setScreen(new ItemPickerScreen(screen))));
		}
		if (ClientState.shots()) {
			buttons.add(new Button("btn_shot", "bar.shot", ShotCapture::start));
		}
		buttons.add(new Button("btn_send", "bar.send", this::send));
		input.setFormatter(EmojiInput.wrap(((EditBoxAccessor) input).chaoschatplus$formatter()));
		layout();
	}

	/** Coloca el campo de texto entre el prefijo y los botones. Se llama en cada frame (el prefijo cambia con "/"). */
	public void layout() {
		int h = screen.height;
		barTop = h - 16;
		barBottom = h - 2;
		Font font = Minecraft.getInstance().font;
		buttonsX = screen.width - 5 - buttons.size() * (BOX + GAP) + GAP;
		int x = 7 + prefixWidth(font);
		int width = Math.max(20, buttonsX - 6 - x);
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

	private int buttonX(int index) {
		return buttonsX + index * (BOX + GAP);
	}

	private int buttonY() {
		return barTop + (barBottom - barTop - BOX) / 2;
	}

	/** Fondo de la barra, prefijo y botones (va donde vanilla pintaba su franja negra). */
	public void renderBar(GuiGraphics g, int mouseX, int mouseY) {
		layout();
		Font font = Minecraft.getInstance().font;
		int bg = Minecraft.getInstance().options.getBackgroundColor(Integer.MIN_VALUE);
		Ui.round(g, 2, barTop, screen.width - 2, barBottom, 5, Ui.alpha(Ui.PANEL, Math.max(0x99, bg >>> 24)));
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

		Button hovered = null;
		int by = buttonY();
		for (int i = 0; i < buttons.size(); i++) {
			Button b = buttons.get(i);
			int x = buttonX(i);
			boolean hover = Ui.inside(mouseX, mouseY, x, by, x + BOX, by + BOX);
			boolean active = b.icon().equals("btn_emoji") && picker != null;
			int border = hover || active ? 0xFFA8ADB8 : 0xFF4A4F5A;
			int fill = active ? 0x447AA2FF : hover ? 0x33FFFFFF : 0x22000000;
			Ui.roundBordered(g, x, by, x + BOX, by + BOX, 3, fill, border);
			g.setColor(1, 1, 1, hover || active ? 1.0F : 0.75F);
			g.blit(icon(b.icon()), x + (BOX - ICON) / 2, by + (BOX - ICON) / 2, ICON, ICON, 0, 0, 16, 16, 16, 16);
			g.setColor(1, 1, 1, 1);
			if (hover) {
				hovered = b;
			}
		}

		if (hovered != null) {
			g.renderTooltip(font, Lang.tr(hovered.tooltipKey()), mouseX, mouseY);
		}
	}

	private static ResourceLocation icon(String name) {
		return ChaosChatPlus.id("textures/gui/" + name + ".png");
	}

	/** Popups encima de todo: aviso de límite, autocompletado y selector. */
	public void renderOverlays(GuiGraphics g, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		long since = Util.getMillis() - limitAt;
		if (since < LIMIT_NOTICE_MS) {
			float a = since > LIMIT_NOTICE_MS - 400 ? (LIMIT_NOTICE_MS - since) / 400F : 1F;
			Component text = Lang.tr("bar.limit", MAX_LENGTH);
			int w = font.width(text) + 12;
			int x = 4;
			int y = barTop - 16;
			Ui.round(g, x, y, x + w, y + 13, 4, Ui.alpha(0x3A1E22, (int) (230 * a)));
			g.drawString(font, text, x + 6, y + 3, Ui.alpha(0xFF8A8A, Math.max(4, (int) (255 * a))), false);
		}
		if (picker != null) {
			picker.place(screen.width - 4, barTop - 3);
			picker.render(g, font, mouseX, mouseY);
		} else {
			suggest.render(g, font, barTop, mouseX, mouseY);
		}
		g.pose().popPose();
	}

	public void onEdited() {
		if (converting) {
			return;
		}
		EmojiInput.Converted c = EmojiInput.toGlyphs(input.getValue(), input.getCursorPosition());
		if (c != null) {
			converting = true;
			input.setValue(c.text());
			input.setCursorPosition(c.cursor());
			input.setHighlightPos(c.cursor());
			converting = false;
		}
		// Los emojis ocupan 1 letra aquí pero ":nombre:" al enviar: el límite cuenta lo que se enviará.
		String value = input.getValue();
		int sent = EmojiInput.toCodes(value).length();
		input.setMaxLength(Math.max(value.length(), MAX_LENGTH - (sent - value.length())));
		if (sent >= MAX_LENGTH) {
			limitAt = Util.getMillis();
		}
		suggest.update();
	}

	/** Para la autoprueba: abre el selector en la pestaña GIFs. */
	public void openGifsForTest() {
		if (picker == null) {
			togglePicker();
		}
		picker.showGifs();
	}

	public void togglePicker() {
		picker = picker == null ? new EmojiPicker(this::insertEmoji, () -> Minecraft.getInstance().setScreen(null)) : null;
	}

	private void insertEmoji(ClientState.ClientEmoji e) {
		String value = input.getValue();
		int cursor = Math.min(input.getCursorPosition(), value.length());
		String before = cursor > 0 && !Character.isWhitespace(value.charAt(cursor - 1)) ? " " : "";
		input.insertText(before + e.code() + " ");
	}

	private void send() {
		screen.handleChatInput(input.getValue(), true);
		Minecraft.getInstance().setScreen(null);
	}

	public boolean keyPressed(int key) {
		if (picker != null && key == 256) {
			picker = null;
			return true;
		}
		if (picker != null && picker.keyPressed(key)) {
			return true;
		}
		if (picker == null && suggest.keyPressed(key)) {
			return true;
		}
		return false;
	}

	/** Letras para el buscador de GIFs (si está abierto). true = no van al chat. */
	public boolean charTypedPicker(char c) {
		return picker != null && picker.charTyped(c);
	}

	/** Al intentar escribir con el campo lleno, vuelve a enseñar el aviso. */
	public void charTyped() {
		if (EmojiInput.toCodes(input.getValue()).length() >= MAX_LENGTH
				|| input.getValue().length() >= ((EditBoxAccessor) input).chaoschatplus$maxLength()) {
			limitAt = Util.getMillis();
		}
	}

	public boolean mouseClicked(double mx, double my, int button) {
		if (picker != null && picker.mouseClicked(mx, my, button)) {
			return true;
		}
		if (picker == null && suggest.mouseClicked(mx, my)) {
			return true;
		}
		int by = buttonY();
		for (int i = 0; i < buttons.size(); i++) {
			int x = buttonX(i);
			if (Ui.inside(mx, my, x, by, x + BOX, by + BOX)) {
				Button b = buttons.get(i);
				if (!b.icon().equals("btn_emoji")) {
					picker = null;
				}
				b.action().run();
				return true;
			}
		}
		if (picker != null) {
			picker = null; // clic fuera: se cierra
		}
		return false;
	}

	/** ¿El ratón está sobre un popup (selector o autocompletado)? Entonces el chat de debajo no debe reaccionar. */
	public boolean blocksMouse(double mx, double my) {
		return (picker != null && picker.contains(mx, my)) || (picker == null && suggest.contains(mx, my));
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		return picker != null && picker.mouseScrolled(mx, my, amount);
	}
}
