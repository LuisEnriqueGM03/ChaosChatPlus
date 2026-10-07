package com.chaos.chatplus.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.gif.GifInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Preferencias del cliente: config/chaoschatplus-client.json. */
public final class ClientPrefs {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int MAX_RECENT = 18;
	private static ClientPrefs current = new ClientPrefs();

	/** Chat con el diseño moderno (false = el de vanilla). */
	public boolean modernChat = true;
	/** Animación de entrada de los mensajes nuevos. */
	public boolean animations = true;
	/** Sonido al mencionarte en servidores sin el mod (con el mod lo manda el servidor). */
	public boolean mentionSound = true;
	public List<String> recentEmojis = new ArrayList<>();
	public Set<String> favoriteEmojis = new LinkedHashSet<>();
	/** GIFs favoritos (con su enlace, para poder mandar también los de Klipy). */
	public List<GifInfo> favoriteGifs = new ArrayList<>();

	public static ClientPrefs get() {
		return current;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("chaoschatplus-client.json");
	}

	public static void load() {
		Path f = file();
		if (Files.exists(f)) {
			try {
				ClientPrefs loaded = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), ClientPrefs.class);
				if (loaded != null) {
					if (loaded.recentEmojis == null) loaded.recentEmojis = new ArrayList<>();
					if (loaded.favoriteEmojis == null) loaded.favoriteEmojis = new LinkedHashSet<>();
					if (loaded.favoriteGifs == null) loaded.favoriteGifs = new ArrayList<>();
					loaded.favoriteGifs.removeIf(g -> g == null || g.id() == null || g.url() == null || g.id().startsWith("klipy:"));
					current = loaded;
				}
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.warn("No se pudo leer {}", f, e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(current), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.warn("No se pudo guardar chaoschatplus-client.json", e);
		}
	}

	public static void useEmoji(String name) {
		List<String> recent = current.recentEmojis;
		recent.remove(name);
		recent.add(0, name);
		while (recent.size() > MAX_RECENT) {
			recent.remove(recent.size() - 1);
		}
		save();
	}

	public static void toggleFavorite(String name) {
		if (!current.favoriteEmojis.remove(name)) {
			current.favoriteEmojis.add(name);
		}
		save();
	}

	public static boolean isFavoriteGif(String id) {
		return current.favoriteGifs.stream().anyMatch(g -> g.id().equals(id));
	}

	/** Marca o desmarca un GIF (como mucho 100; el más nuevo va primero). */
	public static void toggleFavoriteGif(GifInfo gif) {
		if (!current.favoriteGifs.removeIf(g -> g.id().equals(gif.id()))) {
			current.favoriteGifs.add(0, gif);
			while (current.favoriteGifs.size() > 100) {
				current.favoriteGifs.remove(current.favoriteGifs.size() - 1);
			}
		}
		save();
	}
}
