package com.chaos.chatplus.emoji;

import com.chaos.chatplus.emoji.EmojiStore.Store;
import com.chaos.chatplus.emoji.EmojiStore.Custom;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Todos los emojis: los de serie (assets/chaoschatplus/emojis.json, codepoints fijos desde U+E000) y los propios del
 * servidor (config/chaoschatplus/emojis/*.png, codepoints estables desde U+E800 guardados en emojis.json).
 * Las subidas de los jugadores esperan en emojis/pending/ hasta que un OP las apruebe.
 */
public final class EmojiRegistry {

	public static final Pattern NAME = Pattern.compile("[a-z0-9_]{2,32}");
	public static final int MAX_UPLOAD_BYTES = 30 * 1024;
	public static final int MAX_UPLOAD_SIZE = 128;
	/** Los emojis se guardan como mucho a este tamaño (la fuente los dibuja a 8 px de alto). */
	private static final int STORE_SIZE = 32;
	private static final int FIRST_CUSTOM = 0xE800;
	private static final int LAST_CUSTOM = 0xF8FF;



	public static class Pending {
		public String name;
		public String uploader;
		public long time;
	}

	private static final List<Emoji> BUILTIN = readBuiltin();

	private static Store store = new Store();
	private record State(List<Emoji> all, Map<String, Emoji> names, Map<Integer, Emoji> codepoints,
			Map<String, Emoji> unicode, int maxUnicodeLength, Map<String, byte[]> images,
			Map<String, byte[]> defaults, int version) {}
	private static volatile State state = new State(List.of(), Map.of(), Map.of(), Map.of(), 0, Map.of(), null, 0);
	private static volatile List<Pending> pendingView = List.of();
	private static volatile List<String> disabledView = List.of();
	private static Map<String, byte[]> customPng = Map.of();
	/** Categoría (subcarpeta) y archivo de cada emoji propio. */
	private static Map<String, String> customCategory = Map.of();
	private static Map<String, Path> customFile = Map.of();
	/** Emojis de serie que están en emojis/default/ (con su PNG, por si el dueño lo cambió). Null = nunca se exportaron. */
	@Nullable private static Map<String, byte[]> builtinFiles;
	private static volatile String snapshot = "";
	private static final String EXPORTED = ".exportado";
	private static int version;

	private EmojiRegistry() {}

	// ------------------------------------------------------------------ consultas

	public static List<Emoji> all() {
		return state.all();
	}

	@Nullable
	public static Emoji byName(String nameOrAlias) {
		return state.names().get(nameOrAlias.toLowerCase(Locale.ROOT));
	}

	@Nullable
	public static Emoji byCodepoint(int codepoint) {
		return state.codepoints().get(codepoint);
	}

	/** Busca un emoji Unicode que empiece en {@code index}; devuelve el emoji y cuántos chars ocupa (o null). */
	@Nullable
	public static Map.Entry<Emoji, Integer> unicodeAt(String text, int index) {
		State view = state;
		for (int len = Math.min(view.maxUnicodeLength(), text.length() - index); len > 0; len--) {
			Emoji e = view.unicode().get(text.substring(index, index + len));
			if (e != null) {
				int end = index + len;
				// Selector de variación (❤️ = ❤ + U+FE0F): se lo come también.
				if (end < text.length() && text.charAt(end) == '️') {
					end++;
				}
				return Map.entry(e, end - index);
			}
		}
		return null;
	}

	public static Map<String, byte[]> customImages() {
		return state.images();
	}

	public static List<Pending> pending() {
		return pendingView;
	}

	/** Cambia cada vez que cambia algo (para saber si hay que rehacer el pack). */
	public static int version() {
		return state.version();
	}

	/** PNG del emoji (de serie: el de emojis/default/ si está, si no el del jar; propio: el de su carpeta). */
	@Nullable
	public static byte[] png(Emoji emoji) {
		if (!emoji.builtin()) {
			return state.images().get(emoji.name());
		}
		Map<String, byte[]> defaults = state.defaults();
		if (defaults != null && defaults.containsKey(emoji.name())) {
			return defaults.get(emoji.name());
		}
		return jarPng(emoji.name());
	}

	@Nullable
	private static byte[] jarPng(String name) {
		try (InputStream in = EmojiRegistry.class.getResourceAsStream(
				"/assets/chaoschatplus/textures/emoji/" + name + ".png")) {
			return in == null ? null : in.readAllBytes();
		} catch (IOException e) {
			return null;
		}
	}

	// ------------------------------------------------------------------ carga

	public static Path dir() {
		return ChatConfig.dir().resolve("emojis");
	}

	public static synchronized void load() {
		Path file = ChatConfig.dir().resolve("emojis.json");
		try {
			store = EmojiStore.read(file);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudo leer {}; se conservan los emojis y el archivo", file, e);
			return;
		}

		// Las carpetas mandan: emojis/default/<categoría>/ = los de serie (borrar uno lo quita), y cualquier otra
		// subcarpeta (o la raíz) = emojis propios, con la subcarpeta como categoría.
		export(false);
		Map<String, byte[]> images = new LinkedHashMap<>();
		Map<String, String> cats = new HashMap<>();
		Map<String, Path> paths = new HashMap<>();
		Map<String, byte[]> defaults = Files.isDirectory(dir().resolve("default")) ? new HashMap<>() : null;
		List<Path> files = pngFiles();
		for (Path p : files) {
			Path rel = dir().relativize(p);
			String first = rel.getNameCount() > 1 ? rel.getName(0).toString() : "";
			String name = fileName(p);
			try {
				if (first.equals("default")) {
					if (defaults != null && BUILTIN.stream().anyMatch(b -> b.name().equals(name))) {
						if (Files.size(p) > EmojiImages.MAX_FILE_BYTES) continue;
						byte[] png = Files.readAllBytes(p);
						byte[] normalized = EmojiImages.normalize(png);
						defaults.put(name, normalized != null ? normalized : jarPng(name));
					}
					continue;
				}
				if (!NAME.matcher(name).matches()) {
					ChaosChatPlus.LOGGER.warn("Emoji {} ignorado: el nombre debe ser [a-z0-9_] de 2 a 32 letras", rel);
					continue;
				}
				if (Files.size(p) > EmojiImages.MAX_FILE_BYTES) continue;
				byte[] png = EmojiImages.normalize(Files.readAllBytes(p));
				if (png == null) {
					ChaosChatPlus.LOGGER.warn("Emoji {} ignorado: no es una imagen PNG válida", rel);
					continue;
				}
				images.put(name, png);
				paths.put(name, p);
				cats.put(name, rel.getNameCount() > 1 ? rel.getName(rel.getNameCount() - 2).toString().toLowerCase(Locale.ROOT)
						: Emoji.CUSTOM_CATEGORY);
			} catch (IOException e) {
				ChaosChatPlus.LOGGER.warn("No se pudo leer el emoji {}", rel, e);
			}
		}
		builtinFiles = defaults;
		customCategory = cats;
		customFile = paths;
		snapshot = snapshot(files);
		store.custom.removeIf(c -> c == null || c.name == null || !images.containsKey(c.name));
		Set<String> known = new HashSet<>();
		store.custom.forEach(c -> known.add(c.name));
		for (String name : images.keySet()) {
			if (!known.contains(name)) {
				Custom c = new Custom();
				c.name = name;
				c.codepoint = 0;
				store.custom.add(c);
			}
		}
		store.pending.removeIf(p -> p == null || p.name == null || !Files.exists(pendingFile(p.name)));
		customPng = images;
		rebuild();
		save();
		ChaosChatPlus.LOGGER.info("Emojis: {} activos ({} propios), {} pendientes de aprobar",
				state.all().size(), store.custom.size(), store.pending.size());
	}

	/** Copia los emojis de serie a emojis/default/&lt;categoría&gt;/ (solo los que nunca se copiaron). */
	public static int export(boolean restoreAll) {
		Path marker = dir().resolve(EXPORTED);
		Set<String> done = new java.util.LinkedHashSet<>();
		int copied = 0;
		try {
			if (Files.exists(marker) && !restoreAll) {
				done.addAll(Files.readAllLines(marker, StandardCharsets.UTF_8));
			}
			for (Emoji b : BUILTIN) {
				Path target = dir().resolve("default").resolve(b.category()).resolve(b.name() + ".png");
				// Los de serie pasaron de 16x16 a 32x32: la copia vieja (16x16) se cambia por la nueva.
				if (Files.exists(target) && pngWidth(Files.readAllBytes(target)) == 16) {
					byte[] png = jarPng(b.name());
					if (png != null && pngWidth(png) == 32) {
						Files.write(target, png);
						copied++;
					}
				}
				if (done.contains(b.name()) && !restoreAll) {
					continue;
				}
				if (!Files.exists(target)) {
					byte[] png = jarPng(b.name());
					if (png != null) {
						Files.createDirectories(target.getParent());
						Files.write(target, png);
						copied++;
					}
				}
				done.add(b.name());
			}
			Files.createDirectories(dir());
			Files.write(marker, done, StandardCharsets.UTF_8);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudieron copiar los emojis de serie a {}", dir(), e);
		}
		return copied;
	}

	/** Ancho de un PNG (de su cabecera IHDR); -1 si no es un PNG. */
	private static int pngWidth(byte[] png) {
		if (png == null || png.length < 24 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
			return -1;
		}
		return ((png[16] & 0xFF) << 24) | ((png[17] & 0xFF) << 16) | ((png[18] & 0xFF) << 8) | (png[19] & 0xFF);
	}

	/** ¿Cambió algo en la carpeta de emojis desde la última carga? */
	public static boolean changed() {
		return !snapshot(pngFiles()).equals(snapshot);
	}

	private static List<Path> pngFiles() {
		try {
			Files.createDirectories(dir().resolve("pending"));
			try (java.util.stream.Stream<Path> walk = Files.walk(dir())) {
				return walk.filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
						.filter(p -> !dir().relativize(p).toString().startsWith("pending"))
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

	private static void rebuild() {
		int maxCustom = ChatConfig.get().emojis.maxCustom;
		Set<Integer> used = new HashSet<>();
		store.custom.forEach(c -> {
			if (c.codepoint >= FIRST_CUSTOM && c.codepoint <= LAST_CUSTOM && !used.add(c.codepoint)) {
				c.codepoint = 0; // repetido: se le da otro
			}
		});
		int next = FIRST_CUSTOM;
		List<Emoji> list = new ArrayList<>();
		Map<String, Emoji> names = new HashMap<>();
		Map<Integer, Emoji> cps = new HashMap<>();
		Map<String, Emoji> unicode = new HashMap<>();
		int maxLen = 0;
		for (Emoji b : BUILTIN) {
			if (store.disabled.contains(b.name()) || (builtinFiles != null && !builtinFiles.containsKey(b.name()))) {
				continue;
			}
			List<String> ranks = store.builtinRanks.getOrDefault(b.name(), List.of());
			Emoji e = new Emoji(b.name(), b.aliases(), b.category(), b.codepoint(), true, List.copyOf(ranks), b.unicode());
			list.add(e);
			cps.put(e.codepoint(), e);
			for (String u : e.unicode()) {
				unicode.putIfAbsent(u, e);
				maxLen = Math.max(maxLen, u.length());
			}
		}
		int count = 0;
		for (Custom c : store.custom) {
			if (count++ >= maxCustom) {
				break;
			}
			if (c.codepoint < FIRST_CUSTOM || c.codepoint > LAST_CUSTOM) {
				while (used.contains(next) && next <= LAST_CUSTOM) {
					next++;
				}
				if (next > LAST_CUSTOM) {
					ChaosChatPlus.LOGGER.warn("No quedan codepoints libres para el emoji {}", c.name);
					continue;
				}
				c.codepoint = next;
				used.add(next);
			}
			if (c.aliases == null) c.aliases = new ArrayList<>();
			if (c.ranks == null) c.ranks = new ArrayList<>();
			Emoji e = new Emoji(c.name, List.copyOf(c.aliases), customCategory.getOrDefault(c.name, Emoji.CUSTOM_CATEGORY), c.codepoint, false,
					List.copyOf(c.ranks), List.of());
			list.add(e);
			cps.put(e.codepoint(), e);
		}
		// Primero los nombres y luego los alias: un nombre nunca queda tapado por el alias de otro.
		for (Emoji e : list) {
			names.put(e.name(), e);
		}
		for (Emoji e : list) {
			for (String a : e.aliases()) {
				names.putIfAbsent(a.toLowerCase(Locale.ROOT), e);
			}
		}
		state = new State(List.copyOf(list), Map.copyOf(names), Map.copyOf(cps), Map.copyOf(unicode),
				maxLen, Map.copyOf(customPng), builtinFiles == null ? null : Map.copyOf(builtinFiles), ++version);

	}

	private static void save() {
		try {
			Files.createDirectories(ChatConfig.dir());
			EmojiStore.write(ChatConfig.dir().resolve("emojis.json"), store);
			pendingView = List.copyOf(store.pending);
			disabledView = List.copyOf(store.disabled);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudo guardar emojis.json", e);
		}
	}

	private static List<Emoji> readBuiltin() {
		List<Emoji> out = new ArrayList<>();
		try (InputStream in = EmojiRegistry.class.getResourceAsStream("/assets/chaoschatplus/emojis.json")) {
			JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
			for (JsonElement el : arr) {
				JsonObject o = el.getAsJsonObject();
				out.add(new Emoji(o.get("name").getAsString(), strings(o.getAsJsonArray("aliases")),
						o.get("category").getAsString(), o.get("codepoint").getAsInt(), true, List.of(),
						strings(o.getAsJsonArray("unicode"))));
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.error("No se pudo leer la lista de emojis de serie", e);
		}
		return List.copyOf(out);
	}

	private static List<String> strings(@Nullable JsonArray arr) {
		List<String> out = new ArrayList<>();
		if (arr != null) {
			arr.forEach(e -> out.add(e.getAsString()));
		}
		return List.copyOf(out);
	}

	// ------------------------------------------------------------------ cambios

	public enum Result { OK, PENDING, BAD_NAME, TAKEN, BAD_IMAGE, TOO_BIG, LIMIT, NOT_FOUND }

	/** Añade un emoji propio (directamente o a la cola de aprobación). */
	public static synchronized Result add(String rawName, byte[] png, String uploader, boolean approved) {
		String name = rawName.toLowerCase(Locale.ROOT).strip();
		if (!NAME.matcher(name).matches()) {
			return Result.BAD_NAME;
		}
		if (state.names().containsKey(name) || store.pending.stream().anyMatch(p -> p.name.equals(name))) {
			return Result.TAKEN;
		}
		if (png.length > MAX_UPLOAD_BYTES) {
			return Result.TOO_BIG;
		}
		if (!UploadLimits.hasCapacity(store.custom.size(), store.pending.size(),
				store.pending.stream().filter(p -> uploader.equals(p.uploader)).count(), approved, ChatConfig.get().emojis)) {
			return Result.LIMIT;
		}
		byte[] clean = EmojiImages.normalize(png);
		if (clean == null) {
			return Result.BAD_IMAGE;
		}
		try {
			if (approved) {
				Files.write(dir().resolve(name + ".png"), clean);
			} else {
				Files.createDirectories(dir().resolve("pending"));
				Files.write(pendingFile(name), clean);
				Pending p = new Pending();
				p.name = name;
				p.uploader = uploader;
				p.time = System.currentTimeMillis();
				store.pending.add(p);
				save();
				return Result.PENDING;
			}
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudo guardar el emoji {}", name, e);
			return Result.BAD_IMAGE;
		}
		load();
		return Result.OK;
	}

	@Nullable
	public static synchronized Pending approve(String name) {
		Pending p = findPending(name);
		if (p == null || store.custom.size() >= ChatConfig.get().emojis.maxCustom) {
			return null;
		}
		try {
			Files.move(pendingFile(p.name), dir().resolve(p.name + ".png"));
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudo aprobar el emoji {}", name, e);
			return null;
		}
		store.pending.remove(p);
		save();
		load();
		return p;
	}

	@Nullable
	public static synchronized Pending deny(String name) {
		Pending p = findPending(name);
		if (p == null) {
			return null;
		}
		try {
			Files.deleteIfExists(pendingFile(p.name));
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.warn("No se pudo borrar {}", pendingFile(p.name), e);
		}
		store.pending.remove(p);
		save();
		return p;
	}

	@Nullable
	public static byte[] pendingPng(String name) {
		try {
			Path f = pendingFile(name);
			return Files.exists(f) ? Files.readAllBytes(f) : null;
		} catch (IOException e) {
			return null;
		}
	}

	/** Quita un emoji propio, o desactiva uno de serie. */
	public static synchronized Result remove(String name) {
		Emoji e = byName(name);
		if (e == null) {
			return Result.NOT_FOUND;
		}
		if (e.builtin()) {
			store.disabled.add(e.name());
		} else {
			try {
				Files.deleteIfExists(customFile.getOrDefault(e.name(), dir().resolve(e.name() + ".png")));
			} catch (IOException ex) {
				ChaosChatPlus.LOGGER.warn("No se pudo borrar el emoji {}", e.name(), ex);
			}
		}
		save();
		load();
		return Result.OK;
	}

	/** Vuelve a activar un emoji de serie desactivado. */
	public static synchronized Result enable(String name) {
		if (!store.disabled.remove(name.toLowerCase(Locale.ROOT))) {
			return Result.NOT_FOUND;
		}
		save();
		load();
		return Result.OK;
	}

	public static Collection<String> disabled() {
		return disabledView;
	}

	public static synchronized Result addAlias(String name, String alias) {
		Emoji e = byName(name);
		String a = alias.toLowerCase(Locale.ROOT).strip();
		if (e == null) {
			return Result.NOT_FOUND;
		}
		if (!NAME.matcher(a).matches()) {
			return Result.BAD_NAME;
		}
		if (state.names().containsKey(a)) {
			return Result.TAKEN;
		}
		if (e.builtin()) {
			return Result.NOT_FOUND;
		}
		store.custom.stream().filter(c -> c.name.equals(e.name())).findFirst().ifPresent(c -> c.aliases.add(a));
		save();
		load();
		return Result.OK;
	}

	/** Rangos de Chaos Ranks que pueden usar el emoji (lista vacía = todos). */
	public static synchronized Result setRanks(String name, List<String> ranks) {
		Emoji e = byName(name);
		if (e == null) {
			return Result.NOT_FOUND;
		}
		List<String> clean = new ArrayList<>();
		ranks.forEach(r -> clean.add(r.toLowerCase(Locale.ROOT).strip()));
		clean.removeIf(String::isEmpty);
		if (e.builtin()) {
			if (clean.isEmpty()) {
				store.builtinRanks.remove(e.name());
			} else {
				store.builtinRanks.put(e.name(), clean);
			}
		} else {
			store.custom.stream().filter(c -> c.name.equals(e.name())).findFirst().ifPresent(c -> c.ranks = clean);
		}
		save();
		load();
		return Result.OK;
	}

	@Nullable
	private static Pending findPending(String name) {
		String n = name.toLowerCase(Locale.ROOT);
		return store.pending.stream().filter(p -> p.name.equals(n)).findFirst().orElse(null);
	}

	private static Path pendingFile(String name) {
		return dir().resolve("pending").resolve(name + ".png");
	}

	private static String fileName(Path p) {
		String n = p.getFileName().toString();
		return n.substring(0, n.length() - 4).toLowerCase(Locale.ROOT);
	}

}
