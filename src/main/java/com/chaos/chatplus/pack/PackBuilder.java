package com.chaos.chatplus.pack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Prepara los archivos de fuentes de los emojis: la fuente chaoschatplus:emoji (un proveedor bitmap por emoji, 8 px de alto)
 * y sus texturas. Estos archivos se sincronizan por el protocolo del mod.
 */
public final class PackBuilder {

		private PackBuilder() {}

	/** Fuentes del chat: ruta (assets/...) → contenido. */
	public static Map<String, byte[]> files() throws IOException {
		Map<String, byte[]> zip = new java.util.LinkedHashMap<>();
		{
			JsonArray providers = new JsonArray();
			for (Emoji e : EmojiRegistry.all()) {
				byte[] png = EmojiRegistry.png(e);
				if (png == null) {
					continue;
				}
				zip.put("assets/chaoschatplus/textures/emoji/" + e.name() + ".png", png);
				JsonObject p = new JsonObject();
				p.addProperty("type", "bitmap");
				p.addProperty("file", "chaoschatplus:emoji/" + e.name() + ".png");
				p.addProperty("height", 8);
				p.addProperty("ascent", 7);
				JsonArray chars = new JsonArray();
				chars.add(e.glyph());
				p.add("chars", chars);
				providers.add(p);
			}
			JsonObject font = new JsonObject();
			font.add("providers", providers);
			zip.put("assets/chaoschatplus/font/emoji.json", font.toString().getBytes(StandardCharsets.UTF_8));

			// Con Chaos Ranks: sus fuentes y texturas también van en el pack, para los clientes con el mod.
			for (Map.Entry<String, byte[]> e : ChaosRanksCompat.clientAssets().entrySet()) {
				zip.put(e.getKey(), e.getValue());
			}
		}
		return zip;
	}

}
