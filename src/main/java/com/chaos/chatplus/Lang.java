package com.chaos.chatplus;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Textos traducibles (assets/chaoschatplus/lang). Los clientes con el mod los ven en su idioma; los vanilla ven el
 * respaldo, que sale del idioma del servidor ("language" en config.json) y si falta, del inglés.
 */
public final class Lang {

	private static final Map<String, String> ENGLISH = read("en_us");
	private static Map<String, String> server = ENGLISH;

	private Lang() {}

	/** Idioma de los textos de respaldo (es_es, en_us...). */
	public static void setServerLanguage(String code) {
		String lang = code == null ? "en_us" : code.toLowerCase(Locale.ROOT);
		Map<String, String> found = lang.equals("en_us") ? ENGLISH : read(lang);
		server = found.isEmpty() ? ENGLISH : found;
	}

	public static MutableComponent tr(String key, Object... args) {
		String full = ChaosChatPlus.MOD_ID + "." + key;
		return Component.translatableWithFallback(full, text(full), args);
	}

	/** El texto de respaldo tal cual (para la consola o para formatear a mano). */
	public static String text(String fullKey) {
		String s = server.get(fullKey);
		return s != null ? s : ENGLISH.getOrDefault(fullKey, fullKey);
	}

	private static Map<String, String> read(String lang) {
		Map<String, String> out = new HashMap<>();
		try (InputStream in = Lang.class.getResourceAsStream("/assets/chaoschatplus/lang/" + lang + ".json")) {
			if (in == null) {
				return out;
			}
			for (Map.Entry<String, JsonElement> e : JsonParser.parseReader(
					new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
				out.put(e.getKey(), e.getValue().getAsString());
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.error("No se pudo leer {}.json", lang, e);
		}
		return out;
	}
}
