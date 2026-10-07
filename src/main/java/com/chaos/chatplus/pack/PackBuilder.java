package com.chaos.chatplus.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Monta el resource pack de los emojis: la fuente chaoschatplus:emoji (un proveedor bitmap por emoji, 8 px de alto)
 * y sus texturas. El zip sale siempre igual para los mismos emojis, así el hash no cambia y los clientes lo guardan.
 */
public final class PackBuilder {

	/** pack_format de la 1.21.1. */
	private static final int PACK_FORMAT = 34;
	/** Fecha fija para las entradas del zip (si cambiara, cambiaría el hash). */
	private static final long FIXED_TIME = 1767225600000L;

	private PackBuilder() {}

	public static byte[] build() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			JsonObject meta = new JsonObject();
			JsonObject pack = new JsonObject();
			pack.addProperty("pack_format", PACK_FORMAT);
			pack.addProperty("description", "Chaos ChatPlus: emojis del chat");
			meta.add("pack", pack);
			put(zip, "pack.mcmeta", meta.toString().getBytes(StandardCharsets.UTF_8));

			try (InputStream icon = PackBuilder.class.getResourceAsStream("/assets/chaoschatplus/icon.png")) {
				if (icon != null) {
					put(zip, "pack.png", icon.readAllBytes());
				}
			}

			JsonArray providers = new JsonArray();
			for (Emoji e : EmojiRegistry.all()) {
				byte[] png = EmojiRegistry.png(e);
				if (png == null) {
					continue;
				}
				put(zip, "assets/chaoschatplus/textures/emoji/" + e.name() + ".png", png);
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
			put(zip, "assets/chaoschatplus/font/emoji.json", font.toString().getBytes(StandardCharsets.UTF_8));

			// Con Chaos Ranks: sus fuentes y texturas también van en el pack, así los clientes vanilla ven las etiquetas.
			for (Map.Entry<String, byte[]> e : ChaosRanksCompat.clientAssets().entrySet()) {
				put(zip, e.getKey(), e.getValue());
			}
		}
		return bytes.toByteArray();
	}

	private static void put(ZipOutputStream zip, String path, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(path);
		entry.setTime(FIXED_TIME);
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}
}
