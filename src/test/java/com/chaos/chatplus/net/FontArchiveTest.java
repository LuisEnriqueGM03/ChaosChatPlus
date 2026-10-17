package com.chaos.chatplus.net;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class FontArchiveTest {
	byte[] archive(String name, int size, byte[] data) throws Exception {
		var bytes = new ByteArrayOutputStream();
		try (var out = new DataOutputStream(new GZIPOutputStream(bytes))) {
			out.writeInt(1); byte[] path = name.getBytes(StandardCharsets.UTF_8);
			out.writeInt(path.length); out.write(path); out.writeInt(size); out.write(data);
		}
		return bytes.toByteArray();
	}
	@Test void acceptsExpectedAssetPath() throws Exception {
		assertArrayEquals(new byte[]{1,2}, FontArchive.read(archive("assets/chaoschatplus/font/emoji.json", 2, new byte[]{1,2})).values().iterator().next());
	}
	@Test void rejectsTruncatedFilesAndOversizedLengths() throws Exception {
		assertThrows(IOException.class, () -> FontArchive.read(archive("assets/chaosranks/textures/font/test.png", 4, new byte[]{1})));
		assertThrows(IOException.class, () -> FontArchive.read(archive("assets/chaoschatplus/font/emoji.json", Integer.MAX_VALUE, new byte[0])));
	}
	@Test void rejectsTraversalAndForeignNamespaces() throws Exception {
		assertThrows(IOException.class, () -> FontArchive.read(archive("assets/chaoschatplus/../secret", 1, new byte[]{1})));
		assertThrows(IOException.class, () -> FontArchive.read(archive("assets/minecraft/font/default.json", 1, new byte[]{1})));
	}
}
