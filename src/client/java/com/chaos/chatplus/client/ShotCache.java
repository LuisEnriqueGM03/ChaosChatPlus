package com.chaos.chatplus.client;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.ChatPayloads;
import com.mojang.blaze3d.platform.NativeImage;

import com.chaos.chatplus.client.ClientNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Capturas recibidas del servidor (se piden al pasar el ratón o al abrirlas, no antes). */
public final class ShotCache {

	public record Shot(String id, String owner, byte[] jpeg, ResourceLocation texture, ResourceLocation blurred,
			int width, int height) {}

	private static final com.chaos.chatplus.util.BudgetCache<String, Shot> SHOTS =
			new com.chaos.chatplus.util.BudgetCache<>(24, 64L * 1024 * 1024,
					shot -> (long) shot.width() * shot.height() * 4 + shot.jpeg().length, ShotCache::release);
	private static final Set<String> REQUESTED = new HashSet<>();
	private static final Set<String> GONE = new HashSet<>();

	private ShotCache() {}

	/** La captura si ya llegó; si no, la pide (una vez). */
	@Nullable
	public static Shot get(String id) {
		Shot s = SHOTS.get(id);
		if (s == null && !GONE.contains(id) && REQUESTED.add(id) && ClientNetwork.canSend(ChatPayloads.RequestShot.TYPE)) {
			ClientNetwork.send(new ChatPayloads.RequestShot(id));
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
			SHOTS.remove(id);
			GONE.add(id);
			return;
		}
		try {
			BufferedImage img = com.chaos.chatplus.image.SafeImages.read(data.jpeg(), 4_000_000, 1280, 1280L * 1280, "jpeg");
			if (img == null) {
				return;
			}
			SHOTS.remove(id);
			GONE.remove(id);
			ResourceLocation tex = ChaosChatPlus.id("shot/" + id);
			ResourceLocation blur = ChaosChatPlus.id("shot/" + id + "_blur");
			Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(toNative(img)));
			Minecraft.getInstance().getTextureManager().register(blur, new DynamicTexture(toNative(blur(img))));
			SHOTS.put(id, new Shot(id, data.owner(), data.jpeg(), tex, blur, img.getWidth(), img.getHeight()));

		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("Captura {} no válida", id, e);
		}
	}

	public static void clear() {
				SHOTS.clear();
		REQUESTED.clear();
		GONE.clear();
	}

	private static void release(Shot s) {
		REQUESTED.remove(s.id());
		Minecraft.getInstance().getTextureManager().release(s.texture());
		Minecraft.getInstance().getTextureManager().release(s.blurred());
	}

	static NativeImage toNative(BufferedImage img) {
		return toNative(img, false);
	}

	/** BufferedImage (ARGB) → NativeImage (ABGR). Con {@code alpha} se respeta la transparencia. */
	static NativeImage toNative(BufferedImage img, boolean alpha) {
		NativeImage out = new NativeImage(img.getWidth(), img.getHeight(), false);
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				int abgr = (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
				out.setPixelRGBA(x, y, alpha ? abgr : abgr | 0xFF000000);
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
