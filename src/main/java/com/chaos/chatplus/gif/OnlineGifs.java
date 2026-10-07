package com.chaos.chatplus.gif;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GIFs de internet, buscados en vivo desde el servidor: <b>Klipy</b> (sucesor gratuito de Tenor, misma API v2), si el
 * dueño pone gifs.klipyKey. Se manda el enlace; no se guardan en el mod ni en la carpeta del servidor.
 */
public final class OnlineGifs {

	public static final String KLIPY = "klipy:";
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final String USER_AGENT = "ChaosChatPlus/1.1 (Minecraft mod)";
	/** Momentos de las últimas peticiones a Klipy (tope por hora) y de las búsquedas de cada jugador (tope por minuto). */
	private static final Deque<Long> CALLS = new ArrayDeque<>();
	private static final Map<UUID, Deque<Long>> PLAYER_CALLS = new ConcurrentHashMap<>();

	/** Resultado de una búsqueda: los GIFs y si se cortó por un tope ("" = no; "hour" o "player"). */
	public record Result(List<GifInfo> gifs, String limited) {}
	private static final int LIMIT = 24;

	private record Cached(long time, List<GifInfo> results) {}

	private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, GifInfo> KNOWN = new ConcurrentHashMap<>();

	private OnlineGifs() {}

	public static boolean enabled() {
		return !ChatConfig.get().gifs.klipyKey.isBlank();
	}

	/** Nombre de la fuente para el selector ("" = ninguna). */
	public static String sourceName() {
		return enabled() ? "Klipy" : "";
	}

	@Nullable
	public static GifInfo known(String id) {
		return KNOWN.get(id);
	}

	/** ¿Se puede descargar el GIF de esta URL? (solo de Klipy) */
	public static boolean allowedUrl(String url) {
		try {
			URI uri = URI.create(url);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			return "https".equals(uri.getScheme()) && (host.endsWith(".klipy.com") || host.equals("klipy.com"));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Busca en Klipy ("" = destacados) sin pasarse de la clave: lo ya buscado sale de la memoria (gifs.klipyCacheMinutes),
	 * cada jugador tiene unas búsquedas por minuto y el servidor entero unas por hora.
	 */
	public static CompletableFuture<Result> search(String query, UUID player, boolean unlimited) {
		if (!enabled()) {
			return CompletableFuture.completedFuture(new Result(List.of(), ""));
		}
		ChatConfig.Gifs cfg = ChatConfig.get().gifs;
		String q = query.strip().toLowerCase(Locale.ROOT);
		long now = System.currentTimeMillis();
		Cached cached = CACHE.get(q);
		if (cached != null && now - cached.time() < cfg.klipyCacheMinutes * 60_000L) {
			return CompletableFuture.completedFuture(new Result(cached.results(), ""));
		}
		String limited = take(player, unlimited, now);
		if (!limited.isEmpty()) {
			// Sin peticiones libres: lo último que se tenga de esa búsqueda (aunque sea viejo) o nada.
			return CompletableFuture.completedFuture(new Result(cached != null ? cached.results() : List.of(), limited));
		}
		return klipy(q).thenApply(results -> {
			results.forEach(g -> KNOWN.put(g.id(), g));
			if (CACHE.size() > 500) {
				CACHE.clear();
			}
			CACHE.put(q, new Cached(System.currentTimeMillis(), results));
			return new Result(results, "");
		}).exceptionally(e -> {
			ChaosChatPlus.LOGGER.warn("No se pudieron buscar GIFs en Klipy", e);
			return new Result(List.of(), "");
		});
	}

	/** Gasta una petición si quedan; si no, dice qué tope se alcanzó. */
	private static synchronized String take(UUID player, boolean unlimited, long now) {
		ChatConfig.Gifs cfg = ChatConfig.get().gifs;
		while (!CALLS.isEmpty() && now - CALLS.peekFirst() > 3_600_000L) {
			CALLS.pollFirst();
		}
		Deque<Long> mine = PLAYER_CALLS.computeIfAbsent(player, k -> new ArrayDeque<>());
		while (!mine.isEmpty() && now - mine.peekFirst() > 60_000L) {
			mine.pollFirst();
		}
		if (cfg.klipyMaxPerHour > 0 && CALLS.size() >= cfg.klipyMaxPerHour) {
			return "hour";
		}
		if (!unlimited && mine.size() >= cfg.klipyPerPlayerPerMinute) {
			return "player";
		}
		CALLS.addLast(now);
		mine.addLast(now);
		return "";
	}

	// ------------------------------------------------------------------ Klipy (API v2, compatible con Tenor)

	private static CompletableFuture<List<GifInfo>> klipy(String q) {
		ChatConfig.Gifs cfg = ChatConfig.get().gifs;
		String base = q.isEmpty() ? "https://api.klipy.com/v2/featured?" : "https://api.klipy.com/v2/search?q="
				+ URLEncoder.encode(q, StandardCharsets.UTF_8) + "&";
		String url = base + "key=" + URLEncoder.encode(cfg.klipyKey, StandardCharsets.UTF_8)
				+ "&client_key=chaoschatplus&limit=" + LIMIT + "&media_filter=tinygif&contentfilter="
				+ URLEncoder.encode(cfg.contentFilter, StandardCharsets.UTF_8);
		return get(url).thenApply(body -> {
			List<GifInfo> out = new ArrayList<>();
			if (body == null) {
				return out;
			}
			for (JsonElement el : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("results")) {
				JsonObject r = el.getAsJsonObject();
				JsonObject formats = r.getAsJsonObject("media_formats");
				JsonObject tiny = formats != null && formats.has("tinygif") ? formats.getAsJsonObject("tinygif") : null;
				if (tiny == null || !tiny.has("url") || !allowedUrl(tiny.get("url").getAsString())) {
					continue;
				}
				int[] dims = dims(tiny.has("dims") ? tiny.getAsJsonArray("dims") : null);
				String name = text(r, "title");
				if (name.isEmpty()) {
					name = text(r, "content_description");
				}
				if (name.isEmpty()) {
					name = "GIF";
				}
				String page = r.has("itemurl") ? r.get("itemurl").getAsString() : tiny.get("url").getAsString();
				out.add(new GifInfo(KLIPY + r.get("id").getAsString(), name, tiny.get("url").getAsString(), dims[0], dims[1], page));
			}
			return out;
		});
	}

	// ------------------------------------------------------------------ util

	private static CompletableFuture<String> get(String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
				.header("User-Agent", USER_AGENT).GET().build();
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			if (response.statusCode() != 200) {
				ChaosChatPlus.LOGGER.warn("GIFs: {} respondió {}", URI.create(url).getHost(), response.statusCode());
				return null;
			}
			return response.body();
		});
	}

	private static String text(JsonObject o, String key) {
		return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString().strip() : "";
	}

	private static int[] dims(@Nullable com.google.gson.JsonArray arr) {
		if (arr == null || arr.size() != 2) {
			return new int[] {220, 160};
		}
		return new int[] {Math.max(1, arr.get(0).getAsInt()), Math.max(1, arr.get(1).getAsInt())};
	}
}
