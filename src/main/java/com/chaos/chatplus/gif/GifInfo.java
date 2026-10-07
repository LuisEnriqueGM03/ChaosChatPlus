package com.chaos.chatplus.gif;

import com.google.gson.JsonObject;

/**
 * Un GIF que se puede mandar: los propios ("local:nombre", el cliente pide los bytes al servidor) y los de internet
 * ("klipy:id", el cliente los descarga de {@code url}, solo de Klipy).
 * {@code page} es la página web del GIF (para el enlace de los clientes vanilla).
 */
public record GifInfo(String id, String name, String url, int width, int height, String page) {

	public static final String LOCAL = "local:";

	public boolean local() {
		return id.startsWith(LOCAL);
	}

	public JsonObject toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("id", id);
		o.addProperty("name", name);
		o.addProperty("url", url);
		o.addProperty("w", width);
		o.addProperty("h", height);
		return o;
	}

	public static GifInfo fromJson(JsonObject o) {
		return new GifInfo(o.get("id").getAsString(), o.get("name").getAsString(), o.get("url").getAsString(),
				o.get("w").getAsInt(), o.get("h").getAsInt(), "");
	}

	/** Lo que va en la marca del chat: id|w|h|url|nombre. */
	public String encode() {
		return id + "|" + width + "|" + height + "|" + url + "|" + name.replace('|', ' ');
	}

	public static GifInfo decode(String s) {
		String[] p = s.split("\\|", 5);
		if (p.length < 5) {
			return null;
		}
		try {
			return new GifInfo(p[0], p[4], p[3], Integer.parseInt(p[1]), Integer.parseInt(p[2]), "");
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
