package com.chaos.chatplus.emoji;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.chaos.chatplus.config.JsonFiles;

/** Persistence model is independent of image decoding and runtime lookup. */
final class EmojiStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	/** Lo que se guarda en config/chaoschatplus/emojis.json. */
	static class Store {
		List<Custom> custom = new ArrayList<>();
		List<String> disabled = new ArrayList<>();
		List<EmojiRegistry.Pending> pending = new ArrayList<>();
		/** Rangos que pueden usar cada emoji de serie (vacío o ausente = todos). */
		Map<String, List<String>> builtinRanks = new LinkedHashMap<>();
	}

	static class Custom {
		String name;
		int codepoint;
		List<String> aliases = new ArrayList<>();
		List<String> ranks = new ArrayList<>();
	}


	static Store read(Path path) throws IOException {
		Store store = JsonFiles.read(path, GSON, Store.class, Store::new);
		if (store.custom == null) store.custom = new ArrayList<>();
		if (store.disabled == null) store.disabled = new ArrayList<>();
		if (store.pending == null) store.pending = new ArrayList<>();
		if (store.builtinRanks == null) store.builtinRanks = new LinkedHashMap<>();
		store.pending.removeIf(p -> p == null || p.name == null || !EmojiRegistry.NAME.matcher(p.name).matches());
		return store;
	}

	static void write(Path path, Store store) throws IOException { JsonFiles.write(path, GSON, store); }
}
