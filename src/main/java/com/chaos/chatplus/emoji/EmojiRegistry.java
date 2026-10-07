package com.chaos.chatplus.emoji;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	/** Lo que se guarda en config/chaoschatplus/emojis.json. */
	private static class Store {
		List<Custom> custom = new ArrayList<>();
		List<String> disabled = new ArrayList<>();
		List<Pending> pending = new ArrayList<>();
		/** Rangos que pueden usar cada emoji de serie (vacío o ausente = todos). */
		Map<String, List<String>> builtinRanks = new LinkedHashMap<>();
	}

	private static class Custom {
		String name;
		int codepoint;
		List<String> aliases = new ArrayList<>();
		List<String> ranks = new ArrayList<>();
	}

	public static class Pending {
		public String name;
		public String uploader;
		public long time;
	}

	private static final List<Emoji> BUILTIN = readBuiltin();

	private static Store store = new Store();
	private static List<Emoji> all = List.of();
	private static Map<String, Emoji> byName = Map.of();
	private static Map<Integer, Emoji> byCodepoint = Map.of();
	private static Map<String, Emoji> byUnicode = Map.of();
	private static int maxUnicodeLength;
	private static Map<String, byte[]> customPng = Map.of();
	private static int version;

	private EmojiRegistry() {}

	// ------------------------------------------------------------------ consultas

	public static List<Emoji> all() {
		return all;
	}

	@Nullable
	public static Emoji byName(String nameOrAlias) {
		return byName.get(nameOrAlias.toLowerCase(Locale.ROOT));
	}

	@Nullable
	public static Emoji byCodepoint(int codepoint) {
		return byCodepoint.get(codepoint);
	}

	/** Busca un emoji Unicode que empiece en {@code index}; devuelve el emoji y cuántos chars ocupa (o null). */
	@Nullable
	public static Map.Entry<Emoji, Integer> unicodeAt(String text, int index) {
		for (int len = Math.min(maxUnicodeLength, text.length() - index); len > 0; len--) {
			Emoji e = byUnicode.get(text.substring(index, index + len));
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
		return customPng;
	}

	public static List<Pending> pending() {
		return Collections.unmodifiableList(store.pending);
	}

	/** Cambia cada vez que cambia algo (para saber si hay que rehacer el pack). */
	public static int version() {
		return version;
	}

	/** PNG del emoji (de serie: del jar; propio: de la carpeta). */
	@Nullable
	public static byte[] png(Emoji emoji) {
		if (!emoji.builtin()) {
			return customPng.get(emoji.name());
		}
		try (InputStream in = EmojiRegistry.class.getResourceAsStream(
				"/assets/chaoschatplus/textures/emoji/" + emoji.name() + ".png")) {
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
		Store loaded = null;
		if (Files.exists(file)) {
			try {
				loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Store.class);
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.error("No se pudo leer {}", file, e);
			}
		}
		store = loaded != null ? loaded : new Store();
		if (store.custom == null) store.custom = new ArrayList<>();
		if (store.disabled == null) store.disabled = new ArrayList<>();
		if (store.pending == null) store.pending = new ArrayList<>();
		if (store.builtinRanks == null) store.builtinRanks = new LinkedHashMap<>();

		// Los PNG de la carpeta mandan: se añaden los nuevos y se olvidan los que ya no están.
		Map<String, byte[]> images = new LinkedHashMap<>();
		try {
			Files.createDirectories(dir().resolve("pending"));
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir(), "*.png")) {
				for (Path p : stream) {
					String name = fileName(p);
					if (!NAME.matcher(name).matches()) {
						ChaosChatPlus.LOGGER.warn("Emoji {} ignorado: el nombre debe ser [a-z0-9_] de 2 a 32 letras", p.getFileName());
						continue;
					}
					byte[] png = normalize(Files.readAllBytes(p));
					if (png == null) {
						ChaosChatPlus.LOGGER.warn("Emoji {} ignorado: no es una imagen PNG válida", p.getFileName());
						continue;
					}
					images.put(name, png);
				}
			}
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudieron leer los emojis de {}", dir(), e);
		}
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
		ChaosChatPlus.LOGGER.info("Emojis: {} de serie, {} propios, {} pendientes de aprobar",
				BUILTIN.size() - store.disabled.size(), store.custom.size(), store.pending.size());
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
			if (store.disabled.contains(b.name())) {
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
			Emoji e = new Emoji(c.name, List.copyOf(c.aliases), Emoji.CUSTOM_CATEGORY, c.codepoint, false,
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
		all = List.copyOf(list);
		byName = names;
		byCodepoint = cps;
		byUnicode = unicode;
		maxUnicodeLength = maxLen;
		version++;
	}

	private static void save() {
		try {
			Files.createDirectories(ChatConfig.dir());
			Files.writeString(ChatConfig.dir().resolve("emojis.json"), GSON.toJson(store), StandardCharsets.UTF_8);
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
		if (byName.containsKey(name) || store.pending.stream().anyMatch(p -> p.name.equals(name))) {
			return Result.TAKEN;
		}
		if (png.length > MAX_UPLOAD_BYTES) {
			return Result.TOO_BIG;
		}
		if (store.custom.size() >= ChatConfig.get().emojis.maxCustom) {
			return Result.LIMIT;
		}
		byte[] clean = normalize(png);
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
		if (p == null) {
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
				Files.deleteIfExists(dir().resolve(e.name() + ".png"));
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
		return Collections.unmodifiableList(store.disabled);
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
		if (byName.containsKey(a)) {
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

	/**
	 * Comprueba que sea una imagen de verdad y la vuelve a codificar como PNG limpio (sin metadatos ni datos extra),
	 * reducida a como mucho 32 px. Devuelve null si no es una imagen válida o es demasiado grande.
	 */
	@Nullable
	static byte[] normalize(byte[] data) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			if (img == null || img.getWidth() < 1 || img.getHeight() < 1
					|| img.getWidth() > MAX_UPLOAD_SIZE * 8 || img.getHeight() > MAX_UPLOAD_SIZE * 8) {
				return null;
			}
			int w = img.getWidth();
			int h = img.getHeight();
			if (w > STORE_SIZE || h > STORE_SIZE) {
				double scale = (double) STORE_SIZE / Math.max(w, h);
				w = Math.max(1, (int) Math.round(w * scale));
				h = Math.max(1, (int) Math.round(h * scale));
			}
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(img, 0, 0, w, h, null);
			g.dispose();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ImageIO.write(out, "png", bytes);
			return bytes.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}

	/** Para los mensajes: comprueba la imagen de una subida antes de nada (dimensiones del original). */
	public static boolean validUploadSize(byte[] data) {
		try {
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
			return img != null && img.getWidth() <= MAX_UPLOAD_SIZE && img.getHeight() <= MAX_UPLOAD_SIZE;
		} catch (Exception e) {
			return false;
		}
	}
}
