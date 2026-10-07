package com.chaos.chatplus.gif;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Búsqueda en Tenor (API v2 de Google) desde el servidor, con la clave de gifs.tenorKey y el filtro de contenido
 * (por defecto "high", anti-NSFW). Los resultados se guardan 10 minutos.
 */
public final class Tenor {

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static final long CACHE_MS = 10 * 60_000L;
	private static final int LIMIT = 24;

	private record Cached(long time, List<GifInfo> results) {}

	private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
	/** Todos los GIFs de Tenor vistos, para poder mandarlos por id. */
	private static final Map<String, GifInfo> KNOWN = new ConcurrentHashMap<>();

	private Tenor() {}

	public static boolean enabled() {
		return !ChatConfig.get().gifs.tenorKey.isBlank();
	}

	@Nullable
	public static GifInfo known(String id) {
		return KNOWN.get(id);
	}

	/** Búsqueda (consulta vacía = destacados). */
	public static CompletableFuture<List<GifInfo>> search(String query) {
		if (!enabled()) {
			return CompletableFuture.completedFuture(List.of());
		}
		String q = query.strip().toLowerCase(Locale.ROOT);
		Cached cached = CACHE.get(q);
		if (cached != null && System.currentTimeMillis() - cached.time() < CACHE_MS) {
			return CompletableFuture.completedFuture(cached.results());
		}
		ChatConfig.Gifs cfg = ChatConfig.get().gifs;
		String base = q.isEmpty() ? "https://tenor.googleapis.com/v2/featured?" : "https://tenor.googleapis.com/v2/search?q="
				+ URLEncoder.encode(q, StandardCharsets.UTF_8) + "&";
		String url = base + "key=" + URLEncoder.encode(cfg.tenorKey, StandardCharsets.UTF_8)
				+ "&client_key=chaoschatplus&limit=" + LIMIT + "&media_filter=tinygif,gif"
				+ "&contentfilter=" + URLEncoder.encode(cfg.contentFilter, StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
			if (response.statusCode() != 200) {
				ChaosChatPlus.LOGGER.warn("Tenor respondió {}: {}", response.statusCode(), response.body());
				return List.<GifInfo>of();
			}
			List<GifInfo> out = new ArrayList<>();
			for (JsonElement el : JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("results")) {
				JsonObject r = el.getAsJsonObject();
				JsonObject formats = r.getAsJsonObject("media_formats");
				JsonObject tiny = formats != null && formats.has("tinygif") ? formats.getAsJsonObject("tinygif") : null;
				if (tiny == null || !tiny.has("url")) {
					continue;
				}
				String mediaUrl = tiny.get("url").getAsString();
				if (!mediaUrl.startsWith("https://media.tenor.com/")) {
					continue;
				}
				int w = 0;
				int h = 0;
				if (tiny.has("dims") && tiny.getAsJsonArray("dims").size() == 2) {
					w = tiny.getAsJsonArray("dims").get(0).getAsInt();
					h = tiny.getAsJsonArray("dims").get(1).getAsInt();
				}
				String name = r.has("content_description") ? r.get("content_description").getAsString() : "GIF";
				String page = r.has("itemurl") ? r.get("itemurl").getAsString() : mediaUrl;
				GifInfo info = new GifInfo(GifInfo.TENOR + r.get("id").getAsString(), name, mediaUrl, Math.max(1, w),
						Math.max(1, h), page);
				KNOWN.put(info.id(), info);
				out.add(info);
			}
			List<GifInfo> results = List.copyOf(out);
			CACHE.put(q, new Cached(System.currentTimeMillis(), results));
			return results;
		}).exceptionally(e -> {
			ChaosChatPlus.LOGGER.warn("No se pudo buscar en Tenor", e);
			return List.of();
		});
	}
}
