package com.chaos.chatplus.client.screen;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ui.FlatButton;
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.net.ChatPayloads;
import com.mojang.blaze3d.platform.NativeImage;

import com.chaos.chatplus.client.ClientNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Subir un emoji propio (PNG soltado en el chat): vista previa, nombre y [Subir]. Lo aprueba un OP. */
public class EmojiUploadScreen extends Screen {

	private static final ResourceLocation PREVIEW = ChaosChatPlus.id("emoji_upload_preview");
	private static final int W = 220;
	private static final int H = 120;

	private final Path file;
	private final Screen parent;
	@Nullable private byte[] png;
	@Nullable private Component error;
	private EditBox name;
	private FlatButton upload;
	private int left;
	private int top;

	public EmojiUploadScreen(Path file, Screen parent) {
		super(Lang.tr("emoji.upload_title"));
		this.file = file;
		this.parent = parent;
		load();
	}

	private void load() {
		try {
			if (Files.size(file) > 8 * 1024 * 1024) throw new java.io.IOException("Image file too large");
			byte[] data = Files.readAllBytes(file);
			BufferedImage img = com.chaos.chatplus.image.SafeImages.read(data, 8 * 1024 * 1024, 4096, 16_777_216L, "png");
			if (img == null) {
				error = Lang.tr("emoji.error.bad_image", EmojiRegistry.MAX_UPLOAD_SIZE);
				return;
			}
			// Se manda ya reducido a 32 px: cabe de sobra en un paquete y es el tamaño con el que se guarda.
			int size = 32;
			double s = Math.min(1.0, (double) size / Math.max(img.getWidth(), img.getHeight()));
			int w = Math.max(1, (int) Math.round(img.getWidth() * s));
			int h = Math.max(1, (int) Math.round(img.getHeight() * s));
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(img, 0, 0, w, h, null);
			g.dispose();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ImageIO.write(out, "png", bytes);
			png = bytes.toByteArray();
			if (png.length > EmojiRegistry.MAX_UPLOAD_BYTES) {
				png = null;
				error = Lang.tr("emoji.error.too_big", EmojiRegistry.MAX_UPLOAD_SIZE);
			}
		} catch (Exception e) {
			error = Lang.tr("emoji.error.bad_image", EmojiRegistry.MAX_UPLOAD_SIZE);
		}
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		if (png != null && minecraft != null) {
			try {
				minecraft.getTextureManager().register(PREVIEW, new DynamicTexture(NativeImage.read(png)));
			} catch (Exception e) {
				png = null;
			}
		}
		String suggested = file.getFileName().toString().replaceFirst("\\.[^.]+$", "")
				.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
		if (suggested.length() > 32) {
			suggested = suggested.substring(0, 32);
		}
		name = new EditBox(font, left + 64, top + 36, W - 72, 16, Lang.tr("emoji.name"));
		name.setMaxLength(32);
		name.setValue(suggested);
		name.setFilter(s -> s.matches("[a-zA-Z0-9_]*"));
		addRenderableWidget(name);
		setInitialFocus(name);
		upload = addRenderableWidget(new FlatButton(left + W - 8 - 70, top + H - 24, 70, 16, Lang.tr("emoji.upload"), true,
				b -> send()));
		addRenderableWidget(new FlatButton(left + W - 8 - 70 - 6 - 70, top + H - 24, 70, 16, Lang.tr("button.cancel"), false,
				b -> onClose()));
	}

	@Override
	public void tick() {
		String n = name.getValue().toLowerCase(Locale.ROOT);
		upload.active = png != null && EmojiRegistry.NAME.matcher(n).matches();
	}

	private void send() {
		if (png == null || !ClientNetwork.canSend(ChatPayloads.UploadEmoji.TYPE)) {
			return;
		}
		ClientNetwork.send(new ChatPayloads.UploadEmoji(name.getValue().toLowerCase(Locale.ROOT), png));
		onClose();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		Ui.roundBordered(g, left, top, left + W, top + H, 6, Ui.POPUP, Ui.POPUP_BORDER);
		g.drawString(font, title, left + 8, top + 8, Ui.TEXT, false);
		Ui.round(g, left + 8, top + 24, left + 56, top + 72, 5, 0x22FFFFFF);
		if (png != null) {
			g.blit(PREVIEW, left + 16, top + 32, 32, 32, 0, 0, 1, 1, 1, 1);
		}
		g.drawString(font, Lang.tr("emoji.name"), left + 64, top + 25, Ui.MUTED, false);
		g.drawString(font, ":" + name.getValue().toLowerCase(Locale.ROOT) + ":", left + 64, top + 58, Ui.ACCENT, false);
		if (error != null) {
			g.drawString(font, error, left + 8, top + 80, Ui.ERROR, false);
		} else {
			g.drawWordWrap(font, Lang.tr("emoji.upload_note"), left + 8, top + 78, W - 16, 0xFF8A8F98);
		}
		super.render(g, mouseX, mouseY, partial);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0x88000000);
	}

	@Override
	public void removed() {
		if (minecraft != null) {
			minecraft.getTextureManager().release(PREVIEW);
		}
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
