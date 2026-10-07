package com.chaos.chatplus.gif;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GIFs del servidor, todos en config/chaoschatplus/gifs/ (con subcarpetas: cada subcarpeta es una categoría del
 * selector). La primera vez se copian ahí los de serie del mod (gifs/default/&lt;categoría&gt;/...); desde entonces la
 * carpeta manda: borrar un archivo lo quita y soltar uno nuevo lo añade (se vigila cada pocos segundos).
 * Cada GIF se ajusta solo al chat (GifShrinker) y se guarda ya reducido en gifs/.cache/.
 * <p>
 * El cliente con el mod tiene los de serie en su jar ("jar:id") y guarda los demás en su caché por SHA-1 ("sha1:..."),
 * así solo se descarga cada GIF una vez.
 */
public final class GifRegistry {

	private record Entry(GifInfo info, byte[] data, String category, List<String> tags) {}

	/** Datos de un GIF de serie del jar. */
	private record Builtin(String id, String name, List<String> tags, String category) {}

	private static final String EXPORTED = ".exportado";
	private static Map<String, Entry> gifs = Map.of();
	private static List<String> categories = List.of();
	private static Map<String, Builtin> builtins;
	private static Map<String, String> builtinSha1;
	private static String snapshot = "";

	private GifRegistry() {}

	public static Path dir() {
		return ChatConfig.dir().resolve("gifs");
	}

	/** Carpeta de categoría de un GIF de serie según su prefijo. */
	private static String builtinCategory(String id) {
		if (id.startsWith("t_")) return "texto";
		if (id.startsWith("s_")) return "efectos";
		if (id.startsWith("x_")) return "troll";
		if (id.startsWith("e_")) return "emojis";
		return "animados";
	}

	private static synchronized Map<String, Builtin> builtins() {
		if (builtins != null) {
			return builtins;
		}
		Map<String, Builtin> out = new LinkedHashMap<>();
		Map<String, String> sha = new HashMap<>();
		try (InputStream in = GifRegistry.class.getResourceAsStream("/assets/chaoschatplus/gifs/index.json")) {
			if (in != null) {
				for (JsonElement el : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
					JsonObject o = el.getAsJsonObject();
					String id = o.get("id").getAsString();
					List<String> tags = new ArrayList<>();
					if (o.has("tags")) {
						o.getAsJsonArray("tags").forEach(t -> tags.add(t.getAsString().toLowerCase(Locale.ROOT)));
					}
					out.put(id, new Builtin(id, o.get("name").getAsString(), List.copyOf(tags), builtinCategory(id)));
					byte[] data = jarData(id);
					if (data != null) {
						sha.put(id, sha1(data));
					}
				}
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.error("No se pudo leer la lista de GIFs de serie", e);
		}
		builtins = out;
		builtinSha1 = sha;
		return out;
	}

	@Nullable
	public static byte[] jarData(String builtinId) {
		try (InputStream gif = GifRegistry.class.getResourceAsStream("/assets/chaoschatplus/gifs/" + builtinId + ".gif")) {
			return gif == null ? null : gif.readAllBytes();
		} catch (IOException e) {
			return null;
		}
	}

	/** Copia a la carpeta los GIFs de serie que nunca se copiaron (los borrados por el dueño no vuelven). */
	public static int export(boolean restoreAll) {
		Path base = dir().resolve("default");
		Path marker = dir().resolve(EXPORTED);
		Set<String> done = new LinkedHashSet<>();
		try {
			if (Files.exists(marker) && !restoreAll) {
				done.addAll(Files.readAllLines(marker, StandardCharsets.UTF_8));
			}
			int copied = 0;
			for (Builtin b : builtins().values()) {
				Path target = base.resolve(b.category()).resolve(b.id() + ".gif");
				if (done.contains(b.id()) && !restoreAll) {
					continue;
				}
				if (!Files.exists(target)) {
					byte[] data = jarData(b.id());
					if (data != null) {
						Files.createDirectories(target.getParent());
						Files.write(target, data);
						copied++;
					}
				}
				done.add(b.id());
			}
			Files.createDirectories(dir());
			Files.write(marker, done, StandardCharsets.UTF_8);
			return copied;
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudieron copiar los GIFs de serie a {}", base, e);
			return 0;
		}
	}

	public static synchronized void load() {
		export(false);
		Map<String, Entry> found = new LinkedHashMap<>();
		Set<String> cats = new LinkedHashSet<>();
		Path root = dir();
		long max = ChatConfig.get().gifs.maxKilobytes * 1024L;
		List<Path> files = files(root);
		for (Path p : files) {
			try {
				byte[] original = Files.readAllBytes(p);
				if (original.length > max) {
					ChaosChatPlus.LOGGER.warn("GIF {} demasiado grande ({} KB, máximo {} KB)", p.getFileName(), original.length / 1024, max / 1024);
					continue;
				}
				byte[] data = shrunk(original);
				if (data == null) {
					ChaosChatPlus.LOGGER.warn("GIF {} no válido", root.relativize(p));
					continue;
				}
				String rel = root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
				String key = rel.substring(0, rel.length() - 4).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_/]", "_");
				String file = p.getFileName().toString();
				String base = file.substring(0, file.length() - 4);
				Path parent = root.relativize(p).getParent();
				String category = parent == null ? "servidor" : parent.getFileName().toString().toLowerCase(Locale.ROOT);
				Builtin b = builtins().get(base);
				String name = b != null ? b.name() : base.replace('_', ' ');
				List<String> tags = new ArrayList<>(b != null ? b.tags() : List.of());
				tags.add(category);
				String hash = sha1(data);
				String url = b != null && hash.equals(builtinSha1.get(base)) ? "jar:" + base : "sha1:" + hash;
				int[] size = size(data);
				String id = GifInfo.LOCAL + key;
				found.put(id, new Entry(new GifInfo(id, name, url, size[0], size[1], ""), data, category, List.copyOf(tags)));
				cats.add(category);
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.warn("No se pudo leer el GIF {}", p, e);
			}
		}
		gifs = found;
		categories = List.copyOf(cats);
		snapshot = snapshot(files);
		ChaosChatPlus.LOGGER.info("GIFs: {} en {} categorías ({})", gifs.size(), categories.size(), root);
	}

	/** ¿Cambió algo en la carpeta desde la última carga? */
	public static boolean changed() {
		return !snapshot(files(dir())).equals(snapshot);
	}

	private static List<Path> files(Path root) {
		try {
			Files.createDirectories(root);
			try (Stream<Path> walk = Files.walk(root)) {
				return walk.filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gif"))
						.filter(p -> !root.relativize(p).toString().startsWith(".cache"))
						.sorted()
						.toList();
			}
		} catch (IOException e) {
			return List.of();
		}
	}

	private static String snapshot(List<Path> files) {
		StringBuilder sb = new StringBuilder();
		for (Path p : files) {
			try {
				sb.append(p).append('|').append(Files.size(p)).append('|').append(Files.getLastModifiedTime(p).toMillis()).append('\n');
			} catch (IOException ignored) {
				// se borró mientras se miraba
			}
		}
		return sb.toString();
	}

	/** El GIF ajustado al chat, desde gifs/.cache si ya se hizo antes. */
	@Nullable
	private static byte[] shrunk(byte[] original) {
		try {
			String key = sha1(original);
			Path cached = dir().resolve(".cache").resolve(key + ".gif");
			if (Files.exists(cached)) {
				return Files.readAllBytes(cached);
			}
			byte[] out = GifShrinker.shrink(original);
			if (out != original) {
				Files.createDirectories(cached.getParent());
				Files.write(cached, out);
			}
			return out;
		} catch (Exception e) {
			return null;
		}
	}

	private static int[] size(byte[] data) {
		try {
			GifDecoder.Frames f = GifDecoder.decode(data, Integer.MAX_VALUE);
			return new int[] {f.images().get(0).getWidth(), f.images().get(0).getHeight()};
		} catch (Exception e) {
			return new int[] {64, 64};
		}
	}

	public static String sha1(byte[] data) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static List<String> categories() {
		return categories;
	}

	/** Categoría especial en la búsqueda: "@cat:nombre". */
	public static final String CATEGORY_QUERY = "@cat:";

	/** GIFs que encajan: vacío = todos; "@cat:x" = los de esa carpeta; si no, por nombre o etiquetas. */
	public static List<GifInfo> search(String query) {
		String q = Normalizer.normalize(query.toLowerCase(Locale.ROOT).strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String category = q.startsWith(CATEGORY_QUERY) ? q.substring(CATEGORY_QUERY.length()) : null;
		List<GifInfo> out = new ArrayList<>();
		for (Entry e : gifs.values()) {
			boolean match;
			if (q.isEmpty()) {
				match = true;
			} else if (category != null) {
				match = e.category().equals(category);
			} else {
				String name = Normalizer.normalize(e.info().name().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
				match = name.contains(q) || e.tags().stream().anyMatch(t -> t.contains(q));
			}
			if (match) {
				out.add(e.info());
			}
		}
		return out;
	}

	@Nullable
	public static GifInfo info(String id) {
		Entry e = gifs.get(id);
		return e == null ? null : e.info();
	}

	@Nullable
	public static byte[] data(String id) {
		Entry e = gifs.get(id);
		return e == null ? null : e.data();
	}
}
