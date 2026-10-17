package com.chaos.chatplus;

import org.junit.jupiter.api.Test;
import com.google.gson.*;
import com.chaos.chatplus.gif.GifDecoder;
import com.chaos.chatplus.image.SafeImages;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceIntegrityTest {
	byte[] bytes(String path) throws Exception {
		try (var in = getClass().getResourceAsStream("/assets/chaoschatplus/" + path)) {
			assertNotNull(in, path); return in.readAllBytes();
		}
	}
	JsonElement json(String path) throws Exception {
		return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8));
	}
	@Test void everyBuiltinImageAndGifPassesProductionValidation() throws Exception {
		Set<Integer> codepoints = new HashSet<>(); Set<String> names = new HashSet<>();
		for (var entry : json("emojis.json").getAsJsonArray()) {
			var emoji = entry.getAsJsonObject(); String name = emoji.get("name").getAsString();
			assertTrue(names.add(name)); assertTrue(codepoints.add(emoji.get("codepoint").getAsInt()));
			assertNotNull(SafeImages.read(bytes("textures/emoji/" + name + ".png"), 8*1024*1024, 1024, 1048576, "png"));
		}
		for (var entry : json("gifs/index.json").getAsJsonArray()) {
			String id = entry.getAsJsonObject().get("id").getAsString();
			var frames = GifDecoder.decode(bytes("gifs/" + id + ".gif"), Integer.MAX_VALUE);
			assertFalse(frames.images().isEmpty(), id);
		}
	}
	@Test void allTranslationsHaveTheSameKeys() throws Exception {
		Set<String> english = json("lang/en_us.json").getAsJsonObject().keySet();
		for (String language : List.of("es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve")) {
			assertEquals(english, json("lang/" + language + ".json").getAsJsonObject().keySet(), language);
		}
	}
}
