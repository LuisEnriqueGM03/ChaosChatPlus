package com.chaos.chatplus.client;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.gif.GifDecoder;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.OnlineGifs;
import com.chaos.chatplus.net.ChatPayloads;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GIFs animados en el cliente: los propios se piden al servidor y los de internet se descargan de su web (nekos.best o Klipy).
 * Se decodifican en otro hilo y cada frame se sube como textura.
 */
public final class GifCache {

	private static final int MAX_SIZE = 160;
	private static final int MAX_BYTES = 4 * 1024 * 1024;
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();

	public static final class Gif {
		final List<ResourceLocation> frames = new ArrayList<>();
		int[] delays = new int[0];
		int total;
		int width;
		int height;
		boolean ready;
		boolean failed;
	}

	private static final Map<String, Gif> GIFS = new HashMap<>();
	private static int counter;

	private GifCache() {}

	/** El GIF (empieza a cargarse la primera vez que se pide). */
	public static Gif get(GifInfo info) {
		Gif gif = GIFS.get(info.id());
		if (gif != null) {
			return gif;
		}
		Gif created = new Gif();
		created.width = info.width();
		created.height = info.height();
		GIFS.put(info.id(), created);
		if (info.local()) {
			if (ClientPlayNetworking.canSend(ChatPayloads.GifRequest.TYPE)) {
				ClientPlayNetworking.send(new ChatPayloads.GifRequest(info.id()));
			} else {
				created.failed = true;
			}
		} else if (OnlineGifs.allowedUrl(info.url())) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(info.url())).timeout(Duration.ofSeconds(15))
					.header("User-Agent", "ChaosChatPlus/1.1 (Minecraft mod)").GET().build();
			HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).whenComplete((response, error) -> {
				if (error != null || response.statusCode() != 200 || response.body().length > MAX_BYTES) {
					Minecraft.getInstance().execute(() -> created.failed = true);
					return;
				}
				decode(created, response.body());
			});
		} else {
			created.failed = true;
		}
		return created;
	}

	/** Bytes de un GIF propio que llegan del servidor. */
	public static void accept(ChatPayloads.GifData data) {
		Gif gif = GIFS.get(data.id());
		if (gif == null) {
			return;
		}
		if (data.data().length == 0) {
			gif.failed = true;
			return;
		}
		decode(gif, data.data());
	}

	private static void decode(Gif gif, byte[] bytes) {
		CompletableFuture.supplyAsync(() -> {
			try {
				return GifDecoder.decode(bytes, MAX_SIZE);
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.warn("GIF no válido", e);
				return null;
			}
		}, Util.backgroundExecutor()).thenAccept(frames -> Minecraft.getInstance().execute(() -> upload(gif, frames)));
	}

	private static void upload(Gif gif, @Nullable GifDecoder.Frames frames) {
		if (frames == null || frames.images().isEmpty()) {
			gif.failed = true;
			return;
		}
		int id = counter++;
		for (int i = 0; i < frames.images().size(); i++) {
			BufferedImage img = frames.images().get(i);
			ResourceLocation loc = ChaosChatPlus.id("gif/" + id + "_" + i);
			Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(ShotCache.toNative(img, true)));
			gif.frames.add(loc);
		}
		gif.delays = frames.delays();
		int total = 0;
		for (int d : gif.delays) {
			total += d;
		}
		gif.total = Math.max(1, total);
		gif.width = frames.images().get(0).getWidth();
		gif.height = frames.images().get(0).getHeight();
		gif.ready = true;
	}

	/** Frame que toca ahora (animación con los retardos del GIF). */
	@Nullable
	public static ResourceLocation frame(Gif gif) {
		if (!gif.ready || gif.frames.isEmpty()) {
			return null;
		}
		long t = Util.getMillis() % gif.total;
		for (int i = 0; i < gif.delays.length; i++) {
			t -= gif.delays[i];
			if (t < 0) {
				return gif.frames.get(i);
			}
		}
		return gif.frames.get(0);
	}

	/** Dibuja el GIF dentro de la caja (sin deformar, centrado). Devuelve el ancho usado. */
	public static int draw(GuiGraphics g, GifInfo info, int x, int y, int maxW, int maxH, boolean center) {
		Gif gif = get(info);
		int w = Math.max(1, gif.width);
		int h = Math.max(1, gif.height);
		double s = Math.min((double) maxW / w, (double) maxH / h);
		int dw = Math.max(1, (int) (w * s));
		int dh = Math.max(1, (int) (h * s));
		int dx = center ? x + (maxW - dw) / 2 : x;
		int dy = y + (maxH - dh) / 2;
		ResourceLocation frame = frame(gif);
		if (frame != null) {
			g.blit(frame, dx, dy, dw, dh, 0, 0, 1, 1, 1, 1);
		} else {
			g.fill(dx, dy, dx + dw, dy + dh, gif.failed ? 0x44FF5555 : 0x22FFFFFF);
		}
		return dw;
	}

	public static void clear() {
		for (Gif gif : GIFS.values()) {
			gif.frames.forEach(Minecraft.getInstance().getTextureManager()::release);
		}
		GIFS.clear();
	}
}
