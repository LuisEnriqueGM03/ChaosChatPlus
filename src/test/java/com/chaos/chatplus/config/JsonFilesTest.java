package com.chaos.chatplus.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonFilesTest {
	@TempDir Path dir;
	static class Settings { int limit = 42; }
	private final Gson gson = new Gson();
	@Test void missingFileUsesDefaultsAndValidFileRoundTrips() throws Exception {
		Path file = dir.resolve("settings.json");
		Settings value = JsonFiles.read(file, gson, Settings.class, Settings::new);
		assertFalse(Files.exists(file)); value.limit = 7;
		JsonFiles.write(file, gson, value);
		assertEquals(7, JsonFiles.read(file, gson, Settings.class, Settings::new).limit);
		try (var files = Files.list(dir)) { assertEquals(1, files.count()); }
	}
	@Test void invalidExistingConfigurationRemainsUntouched() throws Exception {
		Path file = dir.resolve("settings.json"); String broken = "{invalid config";
		Files.writeString(file, broken);
		assertThrows(IOException.class, () -> JsonFiles.read(file, gson, Settings.class, Settings::new));
		assertEquals(broken, Files.readString(file));
	}
	@Test void rejectsNullInsteadOfReplacingWithDefaults() throws Exception {
		Path file = dir.resolve("settings.json"); Files.writeString(file, "null");
		assertThrows(IOException.class, () -> JsonFiles.read(file, gson, Settings.class, Settings::new));
		assertEquals("null", Files.readString(file));
	}
}
