package com.chaos.chatplus.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import com.google.gson.Gson;

/** An invalid existing file is never replaced by defaults. Writes replace the file atomically when supported. */
public final class JsonFiles {
	private JsonFiles() {}

	public static <T> T read(Path path, Gson gson, Class<T> type, java.util.function.Supplier<T> defaults) throws IOException {
		if (!Files.exists(path)) return defaults.get();
		try {
			T value = gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
			if (value == null) throw new IOException("JSON must contain an object");
			return value;
		} catch (RuntimeException e) {
			throw new IOException("Invalid JSON in " + path, e);
		}
	}

	public static void write(Path path, Gson gson, Object value) throws IOException {
		Files.createDirectories(path.toAbsolutePath().getParent());
		Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), path.getFileName().toString(), ".tmp");
		try {
			Files.writeString(temp, gson.toJson(value), StandardCharsets.UTF_8);
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
