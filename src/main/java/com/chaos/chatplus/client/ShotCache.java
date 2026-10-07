package com.chaos.chatplus.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.ChatPayloads;
import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Capturas recibidas del servidor (se piden al pasar el ratón o al abrirlas, no antes). */
public final class ShotCache {

	public record Shot(String id, String owner, byte[] jpeg, ResourceLocation texture, ResourceLocation blurred,
			int width, int height) {}

	private static final Map<String, Shot> SHOTS = new HashMap<>();
	private static final Set<String> REQUESTED = new HashSet<>();
	private static final Set<String> GONE = new HashSet<>();

	private ShotCache() {}

	/** La captura si ya llegó; si no, la pide (una vez). */
	@Nullable
	public static Shot get(String id) {
		Shot s = SHOTS.get(id);
		if (s == null && !GONE.contains(id) && REQUESTED.add(id) && ClientPlayNetworking.canSend(ChatPayloads.RequestShot.TYPE)) {
			ClientPlayNetworking.send(new ChatPayloads.RequestShot(id));
		}
		return s;
	}

	/** Se borró, se ocultó o caducó. */
	public static boolean gone(String id) {
		return GONE.contains(id);
	}

	public static void accept(ChatPayloads.ShotData data) {
		String id = data.id();
		if (data.jpeg().length == 0) {
			Shot old = SHOTS.remove(id);
			if (old != null) {
				release(old);
			}
			GONE.add(id);
			return;
		}
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data.jpeg()));
			if (img == null) {
				return;
			}
			ResourceLocation tex = ChaosChatPlus.id("shot/" + id);
			ResourceLocation blur = ChaosChatPlus.id("shot/" + id + "_blur");
			Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(toNative(img)));
			Minecraft.getInstance().getTextureManager().register(blur, new DynamicTexture(toNative(blur(img))));
			Shot old = SHOTS.put(id, new Shot(id, data.owner(), data.jpeg(), tex, blur, img.getWidth(), img.getHeight()));
			if (old != null && !old.texture().equals(tex)) {
				release(old);
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("Captura {} no válida", id, e);
		}
	}

	public static void clear() {
		SHOTS.values().forEach(ShotCache::release);
		SHOTS.clear();
		REQUESTED.clear();
		GONE.clear();
	}

	private static void release(Shot s) {
		Minecraft.getInstance().getTextureManager().release(s.texture());
		Minecraft.getInstance().getTextureManager().release(s.blurred());
	}

	static NativeImage toNative(BufferedImage img) {
		NativeImage out = new NativeImage(img.getWidth(), img.getHeight(), false);
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
				out.setPixelRGBA(x, y, abgr | 0xFF000000);
			}
		}
		return out;
	}

	/** Versión muy pixelada para "difuminada hasta hacer clic". */
	private static BufferedImage blur(BufferedImage img) {
		int w = Math.max(1, img.getWidth() / 24);
		int h = Math.max(1, img.getHeight() / 24);
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		var g = small.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, w, h, null);
		g.dispose();
		return small;
	}
}
