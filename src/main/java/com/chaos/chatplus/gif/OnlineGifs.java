package com.chaos.chatplus.gif;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GIFs de internet, buscados desde el servidor:
 * <ul>
 * <li><b>Klipy</b> (sucesor gratuito de Tenor, misma API v2) si el dueño pone gifs.klipyKey.</li>
 * <li><b>Wikimedia Commons</b>: sin clave. GIFs reales con licencia libre (animales, gente, humor, animaciones...).
 * La búsqueda en español se traduce a inglés y se excluye contenido adulto.</li>
 * <li><b>nekos.best</b>: sin clave ni registro. GIFs de reacciones de anime (risa, baile, abrazo, aplausos...). La
 * búsqueda se traduce a sus categorías (en español o inglés).</li>
 * </ul>
 * Tenor (Google) cerró su API el 30 de junio de 2026, por eso ya no se usa.
 */
public final class OnlineGifs {

	public static final String KLIPY = "klipy:";
	public static final String NEKOS = "nekos:";
	public static final String COMMONS = "commons:";

	/** Para excluir contenido adulto de Wikimedia (en la búsqueda y en los títulos). */
	private static final List<String> BLOCKED = List.of("nude", "naked", "sex", "porn", "nsfw", "erotic", "breast",
			"penis", "vagina", "genital", "masturb", "intercourse", "topless", "nipple", "fetish", "xxx", "hentai",
			"gore", "blood", "corpse", "dead body", "execution", "torture");
	private static final List<String> COMMONS_FEATURED = List.of("funny", "cat", "dog", "dance", "fireworks", "party",
			"laughing", "thumbs up", "explosion", "minecraft", "pixel art", "cartoon");

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final String USER_AGENT = "ChaosChatPlus/1.1 (Minecraft mod)";
	private static final long CACHE_MS = 2 * 60_000L;
	private static final int LIMIT = 24;

	/** Palabras (sin tildes, en minúsculas) → categoría de nekos.best. */
	private static final Map<String, String> WORDS = new HashMap<>();
	private static final List<String> FEATURED = List.of("laugh", "dance", "wave", "clap", "thumbsup", "hug", "happy", "cry");

	static {
		word("laugh", "risa", "reir", "jaja", "jajaja", "lol", "xd", "carcajada", "gracioso");
		word("dance", "baile", "bailar", "fiesta", "party");
		word("hug", "abrazo", "abrazar");
		word("cuddle", "acurrucar", "mimos");
		word("clap", "aplauso", "aplausos", "aplaudir", "bravo");
		word("wave", "hola", "saludo", "saludar", "adios", "chao", "bye", "hi", "hello");
		word("thumbsup", "like", "ok", "bien", "genial", "perfecto", "good");
		word("cry", "llorar", "llanto", "triste", "sad");
		word("kiss", "beso", "besar");
		word("blowkiss", "besito");
		word("angry", "enojado", "enfadado", "ira", "furia", "mad");
		word("happy", "feliz", "alegre", "contento", "yay");
		word("smile", "sonrisa", "sonreir");
		word("sleep", "dormir", "sueno", "zzz");
		word("yawn", "bostezo", "cansado");
		word("facepalm", "verguenza", "enserio");
		word("think", "pensar", "pensando", "hmm");
		word("shocked", "sorpresa", "wow", "omg", "shock", "asombro");
		word("confused", "confundido", "que", "huh");
		word("bored", "aburrido", "aburrimiento");
		word("shrug", "nose", "ni_idea", "whatever");
		word("wink", "guino");
		word("blush", "sonrojo", "timido", "verguenza_ajena");
		word("highfive", "chocala", "choca", "high5");
		word("handshake", "trato", "acuerdo", "deal");
		word("nod", "si", "yes", "asentir");
		word("nope", "no", "nah", "negar");
		word("salute", "saludo_militar", "a_sus_ordenes");
		word("run", "correr", "huir", "escapar");
		word("yeet", "lanzar", "tirar");
		word("slap", "cachetada", "bofetada");
		word("punch", "punetazo", "golpe");
		word("kick", "patada");
		word("bonk", "bonk", "carcel");
		word("pat", "caricia", "palmadita");
		word("poke", "toque", "molestar");
		word("tickle", "cosquillas");
		word("nom", "comer", "comida", "hambre");
		word("feed", "alimentar", "dar_comida");
		word("sip", "te", "cafe", "beber");
		word("stare", "mirar", "mirada", "observar");
		word("lurk", "espiar", "acechar");
		word("smug", "creido", "presumido");
		word("pout", "puchero", "berrinche");
		word("tableflip", "mesa", "rage", "furioso");
		word("spin", "girar", "vuelta");
		word("shake", "sacudir", "temblar");
		word("teehee", "jiji", "risita");
		word("bleh", "lengua", "bleh");
		word("baka", "tonto", "idiota");
		word("handhold", "mano", "manos");
		word("carry", "cargar", "llevar");
		word("wag", "cola", "mover_cola");
		word("bite", "morder", "mordisco");
		word("peck", "besito_rapido");
		word("shoot", "disparar", "pium");
		word("kabedon", "kabedon");
		word("lappillow", "regazo");
		word("nya", "nya", "gato", "miau");
	}

	private static void word(String category, String... words) {
		WORDS.put(category, category);
		for (String w : words) {
			WORDS.put(w, category);
		}
	}

	private record Cached(long time, List<GifInfo> results) {}

	private static final Map<String, Cached> CACHE = new ConcurrentHashMap<>();
	private static final Map<String, GifInfo> KNOWN = new ConcurrentHashMap<>();

	private OnlineGifs() {}

	public static boolean enabled() {
		return klipy() || ChatConfig.get().gifs.nekosBest || ChatConfig.get().gifs.wikimedia;
	}

	private static boolean klipy() {
		return !ChatConfig.get().gifs.klipyKey.isBlank();
	}

	/** Nombre de la fuente para el pie del selector ("" = ninguna). */
	public static String sourceName() {
		if (klipy()) {
			return "Klipy";
		}
		ChatConfig.Gifs cfg = ChatConfig.get().gifs;
		if (cfg.wikimedia && cfg.nekosBest) {
			return "Wikimedia + nekos.best";
		}
		return cfg.wikimedia ? "Wikimedia" : cfg.nekosBest ? "nekos.best" : "";
	}

	@Nullable
	public static GifInfo known(String id) {
		return KNOWN.get(id);
	}

	/** ¿Se puede descargar el GIF de esta URL? (solo de las fuentes conocidas) */
	public static boolean allowedUrl(String url) {
		try {
			URI uri = URI.create(url);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			return "https".equals(uri.getScheme())
					&& (host.equals("nekos.best") || host.endsWith(".klipy.com") || host.equals("klipy.com")
							|| host.equals("upload.wikimedia.org") || host.equals("thumb.wikimedia.org"));
		} catch (Exception e) {
			return false;
		}
	}

	public static CompletableFuture<List<GifInfo>> search(String query) {
		if (!enabled()) {
			return CompletableFuture.completedFuture(List.of());
		}
		String q = query.strip().toLowerCase(Locale.ROOT);
		Cached cached = CACHE.get(q);
		if (cached != null && System.currentTimeMillis() - cached.time() < CACHE_MS) {
			return CompletableFuture.completedFuture(cached.results());
		}
		CompletableFuture<List<GifInfo>> future;
		if (klipy()) {
			future = klipy(q);
		} else {
			ChatConfig.Gifs cfg = ChatConfig.get().gifs;
			CompletableFuture<List<GifInfo>> real = cfg.wikimedia ? commons(q) : CompletableFuture.completedFuture(List.of());
			CompletableFuture<List<GifInfo>> anime = cfg.nekosBest ? nekos(q) : CompletableFuture.completedFuture(List.of());
			// Primero los reales (Wikimedia) y luego los de anime.
			future = real.exceptionally(e -> List.of()).thenCombine(anime.exceptionally(e -> List.of()), (a, b) -> {
				List<GifInfo> all = new ArrayList<>(a);
				all.addAll(b);
				return all;
			});
		}
		return future.thenApply(results -> {
			results.forEach(g -> KNOWN.put(g.id(), g));
			CACHE.put(q, new Cached(System.currentTimeMillis(), results));
			return results;
		}).exceptionally(e -> {
			ChaosChatPlus.LOGGER.warn("No se pudieron buscar GIFs", e);
			return List.of();
		});
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
				String name = r.has("content_description") ? r.get("content_description").getAsString() : "GIF";
				String page = r.has("itemurl") ? r.get("itemurl").getAsString() : tiny.get("url").getAsString();
				out.add(new GifInfo(KLIPY + r.get("id").getAsString(), name, tiny.get("url").getAsString(), dims[0], dims[1], page));
			}
			return out;
		});
	}

	// ------------------------------------------------------------------ Wikimedia Commons (sin clave)

	private static CompletableFuture<List<GifInfo>> commons(String q) {
		String terms;
		if (q.isEmpty()) {
			List<String> pick = new ArrayList<>(COMMONS_FEATURED);
			java.util.Collections.shuffle(pick);
			terms = "\"" + pick.get(0) + "\" OR \"" + pick.get(1) + "\" OR \"" + pick.get(2) + "\"";
		} else {
			terms = english(q);
		}
		StringBuilder search = new StringBuilder("filemime:image/gif ").append(terms);
		for (String b : BLOCKED) {
			if (!b.contains(" ")) {
				search.append(" -").append(b);
			}
		}
		String url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search&gsrnamespace=6"
				+ "&gsrlimit=24&prop=imageinfo&iiprop=url%7Csize&iiurlwidth=220&gsrsearch="
				+ URLEncoder.encode(search.toString(), StandardCharsets.UTF_8);
		return get(url).thenApply(body -> {
			List<GifInfo> out = new ArrayList<>();
			if (body == null) {
				return out;
			}
			JsonObject root = JsonParser.parseString(body).getAsJsonObject();
			if (!root.has("query")) {
				return out;
			}
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("query").getAsJsonObject("pages").entrySet()) {
				JsonObject page = e.getValue().getAsJsonObject();
				String title = page.has("title") ? page.get("title").getAsString() : "";
				String lower = title.toLowerCase(Locale.ROOT);
				if (!lower.endsWith(".gif") || BLOCKED.stream().anyMatch(lower::contains) || !page.has("imageinfo")) {
					continue;
				}
				JsonObject info = page.getAsJsonArray("imageinfo").get(0).getAsJsonObject();
				String thumb = info.has("thumburl") ? info.get("thumburl").getAsString() : info.get("url").getAsString();
				int q2 = thumb.indexOf('?');
				if (q2 > 0) {
					thumb = thumb.substring(0, q2);
				}
				if (!allowedUrl(thumb) || info.get("width").getAsInt() < 32) {
					continue;
				}
				int w = info.has("thumbwidth") ? info.get("thumbwidth").getAsInt() : info.get("width").getAsInt();
				int h = info.has("thumbheight") ? info.get("thumbheight").getAsInt() : info.get("height").getAsInt();
				String name = title.replaceFirst("^File:", "").replaceFirst("(?i)\\.gif$", "").replace('_', ' ') + " · Wikimedia";
				String page2 = info.has("descriptionurl") ? info.get("descriptionurl").getAsString() : thumb;
				out.add(new GifInfo(COMMONS + e.getKey(), name, thumb, Math.max(1, w), Math.max(1, h), page2));
			}
			return out;
		});
	}

	/** La búsqueda en inglés para Wikimedia (las palabras conocidas en español se traducen). */
	private static String english(String q) {
		String plain = Normalizer.normalize(q, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		StringBuilder out = new StringBuilder();
		for (String w : plain.split("[^a-z0-9]+")) {
			if (w.isEmpty()) {
				continue;
			}
			String category = WORDS.get(w);
			String term = category == null ? w : switch (category) {
				case "thumbsup" -> "thumbs up";
				case "highfive" -> "high five";
				case "tableflip" -> "table flip";
				case "laugh" -> "laughing";
				case "cry" -> "crying";
				case "dance" -> "dancing";
				case "clap" -> "applause";
				case "nya" -> "cat";
				default -> category;
			};
			out.append(out.isEmpty() ? "" : " ").append(term);
		}
		return out.isEmpty() ? q : out.toString();
	}

	// ------------------------------------------------------------------ nekos.best (sin clave)

	private static CompletableFuture<List<GifInfo>> nekos(String q) {
		if (q.isEmpty()) {
			List<CompletableFuture<List<GifInfo>>> parts = new ArrayList<>();
			for (String category : FEATURED) {
				parts.add(nekosCategory(category, 3));
			}
			return CompletableFuture.allOf(parts.toArray(CompletableFuture[]::new)).thenApply(v -> {
				List<GifInfo> out = new ArrayList<>();
				parts.forEach(p -> out.addAll(p.join()));
				return out;
			});
		}
		Set<String> categories = new LinkedHashSet<>();
		String plain = Normalizer.normalize(q, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String joined = plain.replace(' ', '_');
		if (WORDS.containsKey(joined)) {
			categories.add(WORDS.get(joined));
		}
		for (String w : plain.split("[^a-z0-9]+")) {
			if (WORDS.containsKey(w)) {
				categories.add(WORDS.get(w));
			}
		}
		if (categories.isEmpty()) {
			// Sin categoría: se busca por nombre de anime/artista.
			String url = "https://nekos.best/api/v2/search?type=2&amount=20&query=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
			return get(url).thenApply(OnlineGifs::parseNekos);
		}
		int each = Math.max(4, 20 / categories.size());
		List<CompletableFuture<List<GifInfo>>> parts = new ArrayList<>();
		for (String c : categories) {
			parts.add(nekosCategory(c, each));
		}
		return CompletableFuture.allOf(parts.toArray(CompletableFuture[]::new)).thenApply(v -> {
			List<GifInfo> out = new ArrayList<>();
			parts.forEach(p -> out.addAll(p.join()));
			return out;
		});
	}

	private static CompletableFuture<List<GifInfo>> nekosCategory(String category, int amount) {
		return get("https://nekos.best/api/v2/" + category + "?amount=" + Math.min(20, amount)).thenApply(OnlineGifs::parseNekos);
	}

	private static List<GifInfo> parseNekos(@Nullable String body) {
		List<GifInfo> out = new ArrayList<>();
		if (body == null) {
			return out;
		}
		for (JsonElement el : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("results")) {
			JsonObject r = el.getAsJsonObject();
			if (!r.has("url")) {
				continue;
			}
			String url = r.get("url").getAsString();
			if (!url.endsWith(".gif") || !allowedUrl(url)) {
				continue;
			}
			String path = URI.create(url).getPath();
			String category = path.split("/").length > 3 ? path.split("/")[3] : "gif";
			String name = r.has("anime_name") ? category + " · " + r.get("anime_name").getAsString() : category;
			int w = 498;
			int h = 280;
			if (r.has("dimensions") && r.get("dimensions").isJsonObject()) {
				JsonObject d = r.getAsJsonObject("dimensions");
				w = d.has("width") ? d.get("width").getAsInt() : w;
				h = d.has("height") ? d.get("height").getAsInt() : h;
			}
			out.add(new GifInfo(NEKOS + path.substring(path.lastIndexOf('/') + 1), name, url, Math.max(1, w), Math.max(1, h), url));
		}
		return out;
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

	private static int[] dims(@Nullable com.google.gson.JsonArray arr) {
		if (arr == null || arr.size() != 2) {
			return new int[] {220, 160};
		}
		return new int[] {Math.max(1, arr.get(0).getAsInt()), Math.max(1, arr.get(1).getAsInt())};
	}
}
