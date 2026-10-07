package com.chaos.chatplus.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.gif.OnlineGifs;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;

/**
 * GIFs de anime de nekos.best guardados en el PC de cada jugador (no en el servidor ni en el mod: sus términos no
 * permiten volver a alojarlos). Al entrar a un servidor que los permite se van descargando de poco en poco, uno cada
 * 2 segundos, hasta tener {@link #TARGETS} por categoría; quedan en .minecraft/chaoschatplus/cache y la próxima vez
 * salen al momento. Si nekos.best pide esperar (429), se respeta.
 */
public final class AnimePrefetch {

	/** Categoría de nekos.best → cuántos GIFs guardar (300 en total). */
	public static final Map<String, Integer> TARGETS = new LinkedHashMap<>();

	static {
		TARGETS.put("laugh", 30);
		TARGETS.put("dance", 30);
		TARGETS.put("happy", 24);
		TARGETS.put("smile", 24);
		TARGETS.put("hug", 24);
		TARGETS.put("wave", 24);
		TARGETS.put("thumbsup", 20);
		TARGETS.put("cry", 24);
		TARGETS.put("clap", 18);
		TARGETS.put("kiss", 18);
		TARGETS.put("angry", 18);
		TARGETS.put("shocked", 18);
		TARGETS.put("facepalm", 14);
		TARGETS.put("sleep", 14);
	}

	/** Un GIF ya guardado (o por guardar). */
	public record Entry(String category, String url, String anime, int width, int height) {

		public GifInfo info() {
			String file = url.substring(url.lastIndexOf('/') + 1);
			return new GifInfo(OnlineGifs.NEKOS + category + "/" + file, category + " · " + anime + " · nekos.best", url,
					width, height, url);
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final String USER_AGENT = "ChaosChatPlus/1.2 (Minecraft mod)";
	private static final long GAP_MS = 2000;

	/** Guardados, por categoría. */
	private static Map<String, List<Entry>> saved = new LinkedHashMap<>();
	/** Pendientes de descargar. */
	private static final Deque<Entry> PENDING = new ArrayDeque<>();
	/** Categorías a las que aún hay que pedir la lista. */
	private static final Deque<String> TO_LIST = new ArrayDeque<>();
	private static boolean loaded;
	private static boolean running;
	private static boolean busy;
	private static long last;
	private static long pausedUntil;

	private AnimePrefetch() {}

	private static Path dir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("chaoschatplus").resolve("cache").resolve("gifs");
	}

	private static Path indexFile() {
		return dir().resolve("nekos.json");
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		try {
			if (Files.exists(indexFile())) {
				Map<String, List<Entry>> read = GSON.fromJson(Files.readString(indexFile(), StandardCharsets.UTF_8),
						new TypeToken<LinkedHashMap<String, List<Entry>>>() {}.getType());
				if (read != null) {
					// Solo los que siguen en la caché (se pudieron borrar al pasar de 150 MB).
					read.values().forEach(list -> list.removeIf(e -> !Files.exists(file(e))));
					saved = read;
				}
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("No se pudo leer el índice de GIFs de anime", e);
		}
	}

	private static void save() {
		try {
			Files.createDirectories(dir());
			// Primero a un temporal y luego se reemplaza: si el juego se cierra a medias, el índice no se corrompe.
			Path tmp = indexFile().resolveSibling("nekos.json.tmp");
			Files.writeString(tmp, GSON.toJson(saved), StandardCharsets.UTF_8);
			Files.move(tmp, indexFile(), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
					java.nio.file.StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.debug("No se pudo guardar el índice de GIFs de anime", e);
		}
	}

	/** Mismo archivo que usa GifCache para esa URL (así el GIF sale de la caché al momento). */
	private static Path file(Entry e) {
		return dir().resolve(GifRegistry.sha1(e.url().getBytes(StandardCharsets.UTF_8)) + ".gif");
	}

	/** El servidor permite GIFs de anime: se empieza (o se sigue) completando la colección. */
	public static void start() {
		load();
		if (running) {
			return;
		}
		running = true;
		TO_LIST.clear();
		for (Map.Entry<String, Integer> t : TARGETS.entrySet()) {
			int have = saved.getOrDefault(t.getKey(), List.of()).size();
			if (have < t.getValue()) {
				TO_LIST.add(t.getKey());
			}
		}
	}

	public static void stop() {
		running = false;
		PENDING.clear();
		TO_LIST.clear();
	}

	/** Cada tick del cliente: como mucho una petición cada 2 segundos. */
	public static void tick() {
		long now = Util.getMillis();
		if (!running || busy || now < pausedUntil || now - last < GAP_MS) {
			return;
		}
		if (!PENDING.isEmpty()) {
			download(PENDING.pollFirst());
		} else if (!TO_LIST.isEmpty()) {
			list(TO_LIST.pollFirst());
		} else {
			running = false;
		}
	}

	private static void list(String category) {
		int need = TARGETS.get(category) - saved.getOrDefault(category, List.of()).size();
		if (need <= 0) {
			return;
		}
		busy = true;
		last = Util.getMillis();
		get("https://nekos.best/api/v2/" + category + "?amount=" + Math.min(20, need + 2), HttpResponse.BodyHandlers.ofString(),
				(status, body) -> {
					if (status == 429) {
						TO_LIST.addFirst(category);
						return;
					}
					if (status != 200 || body == null) {
						return;
					}
					List<String> known = new ArrayList<>();
					saved.getOrDefault(category, List.of()).forEach(e -> known.add(e.url()));
					int added = 0;
					for (JsonElement el : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("results")) {
						JsonObject r = el.getAsJsonObject();
						String url = r.has("url") ? r.get("url").getAsString() : "";
						if (!url.endsWith(".gif") || !OnlineGifs.allowedUrl(url) || known.contains(url) || added >= need) {
							continue;
						}
						int w = 498;
						int h = 280;
						if (r.has("dimensions") && r.get("dimensions").isJsonObject()) {
							JsonObject d = r.getAsJsonObject("dimensions");
							w = d.has("width") ? d.get("width").getAsInt() : w;
							h = d.has("height") ? d.get("height").getAsInt() : h;
						}
						String anime = r.has("anime_name") ? r.get("anime_name").getAsString() : "anime";
						PENDING.addLast(new Entry(category, url, anime, w, h));
						added++;
					}
				});
	}

	private static void download(Entry e) {
		if (Files.exists(file(e))) {
			keep(e);
			return;
		}
		busy = true;
		last = Util.getMillis();
		get(e.url(), HttpResponse.BodyHandlers.ofByteArray(), (status, body) -> {
			if (status == 429) {
				PENDING.addFirst(e);
				return;
			}
			if (status != 200 || body == null || body.length > 4 * 1024 * 1024) {
				return;
			}
			try {
				Files.createDirectories(dir());
				Files.write(file(e), body);
				keep(e);
			} catch (Exception ex) {
				ChaosChatPlus.LOGGER.debug("No se pudo guardar un GIF de anime", ex);
			}
		});
	}

	private static void keep(Entry e) {
		saved.computeIfAbsent(e.category(), k -> new ArrayList<>()).add(e);
		save();
	}

	private interface Done<T> {
		void accept(int status, T body);
	}

	private static <T> void get(String url, HttpResponse.BodyHandler<T> handler, Done<T> done) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
				.header("User-Agent", USER_AGENT).GET().build();
		HTTP.sendAsync(request, handler).whenComplete((response, error) -> Minecraft.getInstance().execute(() -> {
			busy = false;
			if (error != null) {
				done.accept(0, null);
				return;
			}
			if (response.statusCode() == 429) {
				long wait = response.headers().firstValueAsLong("Retry-After").orElse(120);
				pausedUntil = Util.getMillis() + Math.min(wait, 3600) * 1000;
			}
			done.accept(response.statusCode(), response.body());
		}));
	}

	/** Categorías con algún GIF guardado. */
	public static List<String> categories() {
		load();
		List<String> out = new ArrayList<>();
		for (String c : TARGETS.keySet()) {
			if (!saved.getOrDefault(c, List.of()).isEmpty()) {
				out.add(c);
			}
		}
		return out;
	}

	public static List<GifInfo> gifs(String category) {
		load();
		List<GifInfo> out = new ArrayList<>();
		saved.getOrDefault(category, List.of()).forEach(e -> out.add(e.info()));
		return out;
	}

	public static int count() {
		load();
		return saved.values().stream().mapToInt(List::size).sum();
	}

	public static int target() {
		return TARGETS.values().stream().mapToInt(Integer::intValue).sum();
	}

	public static boolean downloading() {
		return running;
	}
}
