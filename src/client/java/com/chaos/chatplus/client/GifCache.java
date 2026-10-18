package com.chaos.chatplus.client;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.gif.GifDecoder;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.gif.OnlineGifs;
import com.chaos.chatplus.net.ChatPayloads;

import com.chaos.chatplus.client.ClientNetwork;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * GIFs animados en el cliente. De dónde salen, en este orden:
 * <ol>
 * <li>"jar:id": los de serie, del propio jar del mod (no se descarga nada).</li>
 * <li>"sha1:...": los que puso el servidor; se guardan en .minecraft/chaoschatplus/cache/gifs/ y solo se piden al
 * servidor la primera vez (uno a uno, para no saturar la conexión).</li>
 * <li>URL de Klipy: solo en memoria, como mucho 2 descargas por segundo y respetando
 * el "Retry-After" si la web pide esperar.</li>
 * </ol>
 */
public final class GifCache {

	private static final int MAX_SIZE = 128;
	private static final int MAX_BYTES = 4 * 1024 * 1024;
	private static final long CACHE_LIMIT = 150L * 1024 * 1024;
	private static final long MIN_GAP_MS = 500;
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NEVER).build();

	public static final class Gif {
		final List<Identifier> frames = new ArrayList<>();
		int[] delays = new int[0];
		int total;
		int width;
		int height;
		boolean ready;
		boolean failed;
		boolean queued;
		boolean loading;
		long memoryBytes;
		CompletableFuture<?> task;
		String id;
		int generation;
	}

	private static int generation;
	private static final com.chaos.chatplus.util.BudgetCache<String, Gif> GIFS =
			new com.chaos.chatplus.util.BudgetCache<>(64, 96L * 1024 * 1024, gif -> gif.memoryBytes, GifCache::dispose);
	/** GIFs esperando su turno (servidor o internet), del más reciente al más antiguo: primero lo que se ve ahora. */
	private static final Deque<GifInfo> QUEUE = new ArrayDeque<>();
	private static boolean busy;
	private static String pendingId;
	private static long busySince;
	private static long lastRequest;
	private static long pausedUntil;
	private static int counter;

	private GifCache() {}

	private static Path cacheDir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("chaoschatplus").resolve("cache").resolve("gifs");
	}

	/** El GIF (empieza a cargarse la primera vez que se pide). */
	public static Gif get(GifInfo info) {
		Gif gif = GIFS.get(info.id());
		if (gif != null) {
			if (gif.queued && !gif.ready && !gif.loading) {
				// Se está viendo: pasa al principio de la cola.
				QUEUE.removeIf(queued -> queued.id().equals(info.id()));
				QUEUE.addFirst(info);
			}
			return gif;
		}
		Gif created = new Gif();
		created.id = info.id();
		created.generation = generation;
		created.width = info.width();
		created.height = info.height();
		GIFS.put(info.id(), created);
		String url = info.url();
		if (url.startsWith("jar:")) {
			byte[] data = jar(url.substring(4));
			if (data != null) {
				decode(created, data);
				return created;
			}
		}
		Path cached = cacheFile(info);
		if (cached != null && Files.exists(cached)) {
			try {
				if (Files.size(cached) > MAX_BYTES) throw new java.io.IOException("Cached GIF too large");
				decode(created, Files.readAllBytes(cached));
				return created;
			} catch (Exception ignored) {
				// se vuelve a descargar
			}
		}
		created.queued = true;
		QUEUE.addFirst(info);
		return created;
	}

	@Nullable
	private static byte[] jar(String id) {
		try (InputStream in = GifCache.class.getResourceAsStream("/assets/chaoschatplus/gifs/" + id + ".gif")) {
			return in == null ? null : in.readAllBytes();
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	private static Path cacheFile(GifInfo info) {
		String url = info.url();
		if (url.matches("sha1:[a-f0-9]{40}")) {
			return cacheDir().resolve(url.substring(5) + ".gif");
		}
		// Los de Klipy no se guardan en disco (sus términos no lo permiten): solo en memoria mientras se ven.
		return null;
	}

	/** Cada tick: lanza la siguiente descarga si toca. */
	public static void tick() {
		long now = Util.getMillis();
		if (busy && now - busySince > 15_000) {
			busy = false; // el servidor no contestó: se sigue con el siguiente
		}
		if (busy || QUEUE.isEmpty() || now < pausedUntil || now - lastRequest < MIN_GAP_MS) {
			return;
		}
		GifInfo info = QUEUE.pollFirst();
		Gif gif = GIFS.get(info.id());
		if (gif == null || gif.ready || gif.failed || gif.loading) {
			return;
		}
		lastRequest = now;
		if (info.local()) {
			if (!ClientNetwork.canSend(ChatPayloads.GifRequest.TYPE)) {
				gif.failed = true;
				return;
			}
			busy = true;
			pendingId = info.id();
			busySince = now;
			ClientNetwork.send(new ChatPayloads.GifRequest(info.id()));
		} else if (OnlineGifs.allowedUrl(info.url())) {
			busy = true;
			pendingId = info.id();
			busySince = now;
			HttpRequest request = HttpRequest.newBuilder(URI.create(info.url())).timeout(Duration.ofSeconds(15))
					.header("User-Agent", "ChaosChatPlus/1.2 (Minecraft mod)").GET().build();
			gif.loading = true;
			gif.task = HTTP.sendAsync(request, com.chaos.chatplus.net.BoundedBody.handler(MAX_BYTES)).whenComplete((response, error) ->
					Minecraft.getInstance().execute(() -> {
						if (!current(gif)) return;
						gif.loading = false;
						busy = false;
						if (error == null && response.statusCode() == 429) {
							// La web pide esperar: se pausa y el GIF vuelve a la cola (no se marca como fallido).
							long wait;
							try { wait = response.headers().firstValueAsLong("Retry-After").orElse(60); }
							catch (NumberFormatException e) { wait = 60; }
							wait = Math.max(1, wait);
							pausedUntil = Util.getMillis() + Math.min(wait, 3600) * 1000;
							QUEUE.addLast(info);
							return;
						}
						if (error != null || response.statusCode() != 200 || response.body().length > MAX_BYTES) {
							gif.failed = true;
							return;
						}
						save(info, response.body());
						decode(gif, response.body());
					}));
		} else {
			gif.failed = true;
		}
	}

	/** Bytes de un GIF del servidor (respuesta a GifRequest). */
	public static void accept(ChatPayloads.GifData data) {
		if (data.id().equals(pendingId)) { busy = false; pendingId = null; }
		Gif gif = GIFS.get(data.id());
		if (gif == null) {
			return;
		}
		if (gif.ready || gif.loading) return;
		if (data.data().length == 0 || data.data().length > MAX_BYTES) {
			gif.failed = true;
			return;
		}
		saveBySha(data.data());
		decode(gif, data.data());
	}

	private static void save(GifInfo info, byte[] data) {
		Path file = cacheFile(info);
		if (file == null) {
			return;
		}
		write(file, data);
	}

	private static void saveBySha(byte[] data) {
		write(cacheDir().resolve(GifRegistry.sha1(data) + ".gif"), data);
	}

	private static void write(Path file, byte[] data) {
		CompletableFuture.runAsync(() -> {
			try {
				Files.createDirectories(file.getParent());
				Files.write(file, data);
				trim();
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.debug("No se pudo guardar el GIF en caché", e);
			}
		}, Util.ioPool());
	}

	/** Si la caché pasa de 150 MB, se borran los más antiguos. */
	private static void trim() throws Exception {
		List<Path> files;
		try (var walk = Files.list(cacheDir())) {
			files = new ArrayList<>(walk.toList());
		}
		long total = 0;
		for (Path p : files) {
			total += Files.size(p);
		}
		if (total <= CACHE_LIMIT) {
			return;
		}
		files.sort((a, b) -> {
			try {
				return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
			} catch (Exception e) {
				return 0;
			}
		});
		for (Path p : files) {
			if (total <= CACHE_LIMIT * 8 / 10) {
				break;
			}
			total -= Files.size(p);
			Files.deleteIfExists(p);
		}
	}

	private static void decode(Gif gif, byte[] bytes) {
		if (!current(gif) || gif.loading || gif.ready) return;
		gif.loading = true;
		gif.task = CompletableFuture.supplyAsync(() -> {
			try {
				return GifDecoder.decode(bytes, MAX_SIZE);
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.warn("GIF no válido", e);
				return null;
			}
		}, Util.backgroundExecutor()).thenAccept(frames -> Minecraft.getInstance().execute(() -> upload(gif, frames)));
	}

	private static void upload(Gif gif, @Nullable GifDecoder.Frames frames) {
		if (!current(gif)) return;
		gif.loading = false;
		gif.queued = false;
		if (frames == null || frames.images().isEmpty()) {
			gif.failed = true;
			return;
		}
		int id = counter++;
		for (int i = 0; i < frames.images().size(); i++) {
			BufferedImage img = frames.images().get(i);
			Identifier loc = ChaosChatPlus.id("gif/" + id + "_" + i);
			Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(() -> "Chaos ChatPlus", ShotCache.toNative(img, true)));
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
		gif.memoryBytes = (long) gif.width * gif.height * 4 * gif.frames.size();
		GIFS.trim();
	}

	/** ¿Hay una web pidiendo esperar? (segundos que faltan, o 0) */
	public static long pausedSeconds() {
		return Math.max(0, (pausedUntil - Util.getMillis()) / 1000);
	}

	/** Frame que toca ahora (animación con los retardos del GIF). */
	@Nullable
	public static Identifier frame(Gif gif) {
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
	public static int draw(GuiGraphicsExtractor g, GifInfo info, int x, int y, int maxW, int maxH, boolean center) {
        return draw(g, info, x, y, maxW, maxH, center, 0xFFFFFFFF);
    }
    public static int draw(GuiGraphicsExtractor g, GifInfo info, int x, int y, int maxW, int maxH, boolean center, int color) {
		Gif gif = get(info);
		int w = Math.max(1, gif.width);
		int h = Math.max(1, gif.height);
		double s = Math.min((double) maxW / w, (double) maxH / h);
		int dw = Math.max(1, (int) (w * s));
		int dh = Math.max(1, (int) (h * s));
		int dx = center ? x + (maxW - dw) / 2 : x;
		int dy = y + (maxH - dh) / 2;
		Identifier frame = frame(gif);
		if (frame != null) {
			com.chaos.chatplus.client.ui.Ui.blit(g, frame, dx, dy, dw, dh, 0, 0, 1, 1, 1, 1, color);
		} else {
			g.fill(dx, dy, dx + dw, dy + dh, gif.failed ? 0x44FF5555 : 0x22FFFFFF);
		}
		return dw;
	}

	private static boolean current(Gif gif) {
		return gif.generation == generation && GIFS.containsValue(gif);
	}

	private static void dispose(Gif gif) {
		if (gif.id.equals(pendingId)) { busy = false; pendingId = null; }
		if (gif.task != null) gif.task.cancel(true);
		gif.frames.forEach(Minecraft.getInstance().getTextureManager()::release);
		gif.frames.clear();
		QUEUE.removeIf(info -> info.id().equals(gif.id));
		gif.ready = false;
		gif.memoryBytes = 0;
	}

	public static void clear() {
		generation++;
		GIFS.clear();
		QUEUE.clear();
		busy = false;
		pendingId = null;
		lastRequest = pausedUntil = 0;
	}
}
