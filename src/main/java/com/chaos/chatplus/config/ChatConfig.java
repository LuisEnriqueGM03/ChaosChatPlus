package com.chaos.chatplus.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Configuración del servidor: config/chaoschatplus/config.json. */
public final class ChatConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static ChatConfig current = new ChatConfig();

	/** Quién puede usar algo: OP de ese nivel (0 = todos) o alguno de esos rangos de Chaos Ranks. */
	public static class Access {
		public boolean enabled = true;
		public int opLevel;
		public List<String> allowedRanks = new ArrayList<>();

		public Access() {}

		Access(int opLevel) {
			this.opLevel = opLevel;
		}

		void sanitize() {
			opLevel = Math.max(0, Math.min(4, opLevel));
			if (allowedRanks == null) {
				allowedRanks = new ArrayList<>();
			}
			allowedRanks.replaceAll(s -> s == null ? "" : s.toLowerCase(Locale.ROOT).strip());
			allowedRanks.removeIf(String::isEmpty);
		}
	}

	public static class Emojis {
		public boolean enabled = true;
		/** Convertir los emojis Unicode (😂, ❤️...) en los del mod. */
		public boolean convertUnicode = true;
		/** Subir emojis soltando un PNG en el chat (con el mod). */
		public Access upload = new Access(2);
		/** Las subidas esperan a /emoji approve. Si es false se publican al momento. */
		public boolean requireApproval = true;
		public int maxCustom = 500;
	}

	public static class ResourcePack {
		/** embedded = lo sirve el propio servidor por HTTP; external = lo subes tú a externalUrl; none = sin pack. */
		public String mode = "embedded";
		public int port = 25599;
		/** IP o dominio con el que los jugadores llegan al puerto. Vacío = el mismo con el que entraron al server. */
		public String publicHost = "";
		/** Con mode = external: URL del zip (descárgalo con /chatplus pack export). */
		public String externalUrl = "";
		public boolean required = false;
		public String prompt = "Emojis del chat (Chaos ChatPlus)";
	}

	public static class Mentions {
		public boolean enabled = true;
		public String color = "#7AA2FF";
		public String sound = "minecraft:block.note_block.bell";
		public float volume = 1.0F;
		public float pitch = 1.3F;
		public boolean actionBar = true;
		/** @everyone y @here. */
		public Access everyone = new Access(2);
		/** @rango (solo con Chaos Ranks). */
		public Access ranks = new Access(2);
	}

	public static class Items {
		public Access access = new Access(0);
		/** Escribir [item] o [i] en el chat comparte el ítem de la mano (también desde clientes vanilla). */
		public boolean chatToken = true;
		public int cooldownSeconds = 5;
	}

	public static class Moderation {
		/** none o sightengine (necesita apiUser y apiSecret de sightengine.com). */
		public String provider = "none";
		public String apiUser = "";
		public String apiSecret = "";
		/** 0..1: por encima de esto la captura se rechaza. */
		public double threshold = 0.5;
	}

	public static class Screenshots {
		public Access access = new Access(0);
		public int cooldownSeconds = 60;
		/** Minutos jugados en el servidor para poder mandar capturas (0 = sin mínimo). */
		public int minPlayMinutes = 0;
		public int maxKilobytes = 512;
		public int expireMinutes = 30;
		/** Las capturas se ven difuminadas hasta que el jugador pulsa "Mostrar". */
		public boolean blurUntilClick = false;
		/** Con tantos reportes de jugadores distintos se oculta sola (0 = nunca). */
		public int autoHideReports = 3;
		public Moderation moderation = new Moderation();
	}

	public static class Messages {
		/** Muertes con estilo (calavera tras el nombre, asesino en rojo, [Ver ítem] en el arma). */
		public boolean deaths = true;
		/** «➜ Luis entró al servidor» / «← Luis salió» y bienvenida la primera vez. */
		public boolean joinLeave = true;
	}

	public static class Gifs {
		public Access access = new Access(0);
		public int cooldownSeconds = 5;
		/** GIFs de reacciones de anime de nekos.best (sin clave ni registro). */
		public boolean nekosBest = false;
		/**
		 * Pestaña Anime: cada jugador guarda en su PC unos 100 GIFs de reacciones de nekos.best (se descargan de poco en
		 * poco, directamente de nekos.best; el servidor no los aloja).
		 */
		public boolean anime = true;
		/** Clave gratuita de Klipy (klipy.com, el sucesor de Tenor). Si está, se busca en Klipy en lugar de nekos.best. */
		public String klipyKey = "";
		/** Filtro de contenido de Klipy: off, low, medium o high. */
		public String contentFilter = "high";
		/** Tope del archivo original de un GIF de la carpeta (se reduce solo a ≤ 300 KB para el chat). */
		public int maxKilobytes = 10240;
	}

	/** Idioma de los mensajes para clientes sin el mod (es_es, en_us...). */
	public String language = "es_es";
	public Emojis emojis = new Emojis();
	public ResourcePack resourcePack = new ResourcePack();
	public Mentions mentions = new Mentions();
	public Items items = new Items();
	public Screenshots screenshots = new Screenshots();
	public Messages messages = new Messages();
	public Gifs gifs = new Gifs();

	public static ChatConfig get() {
		return current;
	}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve(ChaosChatPlus.MOD_ID);
	}

	public static void load() {
		Path file = dir().resolve("config.json");
		ChatConfig loaded = null;
		if (Files.exists(file)) {
			try {
				loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ChatConfig.class);
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.error("No se pudo leer {}; se usa la configuración por defecto", file, e);
			}
		}
		if (loaded == null) {
			loaded = new ChatConfig();
		}
		loaded.sanitize();
		current = loaded;
		Lang.setServerLanguage(current.language);
		try {
			Files.createDirectories(dir());
			Files.writeString(file, GSON.toJson(current), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudo guardar config.json", e);
		}
	}

	/** Color de las menciones como RGB. */
	public int mentionRgb() {
		return parseColor(mentions.color, 0x7AA2FF);
	}

	private void sanitize() {
		if (emojis == null) emojis = new Emojis();
		if (emojis.upload == null) emojis.upload = new Access(2);
		emojis.upload.sanitize();
		emojis.maxCustom = Math.max(0, Math.min(5000, emojis.maxCustom));
		if (resourcePack == null) resourcePack = new ResourcePack();
		resourcePack.mode = resourcePack.mode == null ? "embedded" : resourcePack.mode.toLowerCase(Locale.ROOT).strip();
		if (resourcePack.publicHost == null) resourcePack.publicHost = "";
		if (resourcePack.externalUrl == null) resourcePack.externalUrl = "";
		if (resourcePack.prompt == null) resourcePack.prompt = "";
		resourcePack.port = Math.max(1, Math.min(65535, resourcePack.port));
		if (mentions == null) mentions = new Mentions();
		if (mentions.everyone == null) mentions.everyone = new Access(2);
		if (mentions.ranks == null) mentions.ranks = new Access(2);
		mentions.everyone.sanitize();
		mentions.ranks.sanitize();
		if (mentions.sound == null) mentions.sound = "";
		if (items == null) items = new Items();
		if (items.access == null) items.access = new Access(0);
		items.access.sanitize();
		items.cooldownSeconds = Math.max(0, items.cooldownSeconds);
		if (screenshots == null) screenshots = new Screenshots();
		if (screenshots.access == null) screenshots.access = new Access(0);
		screenshots.access.sanitize();
		if (screenshots.moderation == null) screenshots.moderation = new Moderation();
		screenshots.cooldownSeconds = Math.max(0, screenshots.cooldownSeconds);
		screenshots.minPlayMinutes = Math.max(0, screenshots.minPlayMinutes);
		screenshots.maxKilobytes = Math.max(32, Math.min(4096, screenshots.maxKilobytes));
		screenshots.expireMinutes = Math.max(1, screenshots.expireMinutes);
		screenshots.autoHideReports = Math.max(0, screenshots.autoHideReports);
		if (language == null) language = "en_us";
		if (messages == null) messages = new Messages();
		if (gifs == null) gifs = new Gifs();
		if (gifs.access == null) gifs.access = new Access(0);
		gifs.access.sanitize();
		if (gifs.klipyKey == null) gifs.klipyKey = "";
		if (gifs.contentFilter == null) gifs.contentFilter = "high";
		gifs.cooldownSeconds = Math.max(0, gifs.cooldownSeconds);
		gifs.maxKilobytes = Math.max(64, Math.min(51200, gifs.maxKilobytes));
	}

	public static int parseColor(String s, int fallback) {
		if (s == null) {
			return fallback;
		}
		String hex = s.strip();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		try {
			return Integer.parseInt(hex, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
