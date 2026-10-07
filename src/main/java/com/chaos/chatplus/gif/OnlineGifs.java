package com.chaos.chatplus.gif;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GIFs de Klipy (pestaña Custom), siguiendo sus requisitos de integración (docs.klipy.com):
 * <ul>
 * <li>Las búsquedas y la descarga de los GIFs salen del juego de cada jugador, directo a Klipy (el servidor solo le pasa la
 * clave).</li>
 * <li>Los GIFs no se guardan en disco ni en el servidor: se cargan de sus enlaces tal cual.</li>
 * <li>Los resultados se muestran en el orden y con el contenido que da Klipy, en su propia pestaña. El filtro de contenido
 * se configura en el Partner Panel de Klipy.</li>
 * </ul>
 */
public final class OnlineGifs {

	public static final String KLIPY = "klipy:";

	private OnlineGifs() {}

	public static boolean enabled() {
		return !ChatConfig.get().gifs.klipyKey.isBlank();
	}

	/** Nombre de la fuente para el selector ("" = ninguna). */
	public static String sourceName() {
		return enabled() ? "Klipy" : "";
	}

	/** ¿Es un enlace de Klipy? (solo de ahí se cargan GIFs de internet) */
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
	 * Un GIF de Klipy que manda un jugador (lo eligió en su juego): se acepta solo si su enlace es de Klipy y el servidor
	 * tiene Klipy activado.
	 */
	@Nullable
	public static GifInfo fromClient(String id, String encoded) {
		if (!enabled() || !id.startsWith(KLIPY)) {
			return null;
		}
		GifInfo info = GifInfo.decode(encoded);
		if (info == null || !info.id().equals(id) || !allowedUrl(info.url())) {
			return null;
		}
		return info;
	}

	/** Lee una respuesta de la API (v2, compatible con Tenor), sin reordenar ni quitar resultados. */
	public static List<GifInfo> parse(@Nullable String body) {
		List<GifInfo> out = new ArrayList<>();
		if (body == null) {
			return out;
		}
		for (JsonElement el : JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("results")) {
			JsonObject r = el.getAsJsonObject();
			JsonObject formats = r.getAsJsonObject("media_formats");
			JsonObject media = null;
			for (String f : new String[] {"tinygif", "gif", "nanogif", "mediumgif"}) {
				if (media == null && formats != null && formats.has(f)) {
					media = formats.getAsJsonObject(f);
				}
			}
			if (media == null || !media.has("url")) {
				continue; // sin ningún formato GIF no se puede mostrar
			}
			String url = media.get("url").getAsString();
			int[] dims = dims(media.has("dims") ? media.getAsJsonArray("dims") : null);
			String name = text(r, "title");
			if (name.isEmpty()) {
				name = text(r, "content_description");
			}
			if (name.isEmpty()) {
				name = "GIF";
			}
			String page = r.has("itemurl") ? r.get("itemurl").getAsString() : url;
			out.add(new GifInfo(KLIPY + r.get("id").getAsString(), name, url, dims[0], dims[1], page));
		}
		return out;
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
