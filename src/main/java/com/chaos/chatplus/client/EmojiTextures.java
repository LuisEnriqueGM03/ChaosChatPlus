package com.chaos.chatplus.client;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Imágenes de los emojis para el selector y el autocompletado: los de serie salen del jar del mod y los propios del
 * servidor llegan por paquete (así se ven aunque el jugador rechace el resource pack).
 */
public final class EmojiTextures {

	private record Texture(ResourceLocation id, int width, int height) {}

	private static final Map<String, Texture> CUSTOM = new HashMap<>();

	private EmojiTextures() {}

	public static void accept(String name, byte[] png) {
		if (name.equals("*")) {
			clear();
			return;
		}
		try {
			NativeImage image = NativeImage.read(png);
			ResourceLocation id = ChaosChatPlus.id("custom_emoji/" + name);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
			CUSTOM.put(name, new Texture(id, image.getWidth(), image.getHeight()));
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("Emoji propio no válido: {}", name, e);
		}
	}

	public static void clear() {
		for (Texture t : CUSTOM.values()) {
			Minecraft.getInstance().getTextureManager().release(t.id());
		}
		CUSTOM.clear();
	}

	/** Dibuja el emoji en un cuadrado de {@code size} px (centrado y sin deformar). */
	public static void draw(GuiGraphics g, ClientState.ClientEmoji emoji, int x, int y, int size) {
		draw(g, emoji.name(), emoji.builtin(), x, y, size);
	}

	public static void draw(GuiGraphics g, String name, boolean builtin, int x, int y, int size) {
		Texture t = builtin ? new Texture(builtin(name), 32, 32) : CUSTOM.get(name);
		if (t == null) {
			return;
		}
		int w = size;
		int h = size;
		if (t.width() != t.height()) {
			double s = (double) size / Math.max(t.width(), t.height());
			w = Math.max(1, (int) Math.round(t.width() * s));
			h = Math.max(1, (int) Math.round(t.height() * s));
		}
		g.blit(t.id(), x + (size - w) / 2, y + (size - h) / 2, w, h, 0, 0, t.width(), t.height(), t.width(), t.height());
	}

	public static ResourceLocation builtin(String name) {
		return ChaosChatPlus.id("textures/emoji/" + name + ".png");
	}

	@Nullable
	public static ResourceLocation custom(String name) {
		Texture t = CUSTOM.get(name);
		return t == null ? null : t.id();
	}
}
