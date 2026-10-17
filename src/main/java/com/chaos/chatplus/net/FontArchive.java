package com.chaos.chatplus.net;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class FontArchive {
	private static final int MAX_TOTAL = 16 * 1024 * 1024;
	private FontArchive() {}
	public static Map<String, byte[]> read(byte[] gzip) throws IOException {
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(gzip)))) {
			int count = in.readInt(), total = 0;
			if (count < 0 || count > 8192) throw new IOException("Excessive font file count");
			Map<String, byte[]> files = new LinkedHashMap<>();
			for (int i = 0; i < count; i++) {
				int pathSize = in.readInt();
				if (pathSize < 1 || pathSize > 512) throw new IOException("Invalid font path length");
				byte[] path = exact(in, pathSize);
				String name = new String(path, StandardCharsets.UTF_8);
				if (!name.matches("assets/(chaoschatplus|chaosranks)/(font/[a-z0-9_/-]+\\.json|textures/[a-z0-9_/-]+\\.png)")
						|| name.contains("..") || files.containsKey(name)) throw new IOException("Invalid font path");
				int size = in.readInt();
				if (size < 1 || size > 2 * 1024 * 1024 || (long) total + size + pathSize > MAX_TOTAL) {
					throw new IOException("Excessive font archive");
				}
				total += size + pathSize;
				files.put(name, exact(in, size));
			}
			if (in.read() != -1) throw new IOException("Trailing font data");
			return files;
		}
	}
	private static byte[] exact(DataInputStream in, int length) throws IOException {
		byte[] bytes = in.readNBytes(length);
		if (bytes.length != length) throw new EOFException("Truncated font archive");
		return bytes;
	}
}
