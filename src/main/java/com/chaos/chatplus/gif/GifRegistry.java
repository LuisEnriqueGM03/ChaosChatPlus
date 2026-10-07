package com.chaos.chatplus.gif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** GIFs propios: los de serie del mod (assets/chaoschatplus/gifs) y los del servidor (config/chaoschatplus/gifs/*.gif). */
public final class GifRegistry {

	private record Entry(GifInfo info, byte[] data) {}

	private static Map<String, Entry> gifs = Map.of();

	private GifRegistry() {}

	public static void load() {
		Map<String, Entry> found = new LinkedHashMap<>();
		Path dir = ChatConfig.dir().resolve("gifs");
		int max = ChatConfig.get().gifs.maxKilobytes * 1024;
		try {
			Files.createDirectories(dir);
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.gif")) {
				for (Path p : stream) {
					String file = p.getFileName().toString();
					String name = file.substring(0, file.length() - 4);
					byte[] data = Files.readAllBytes(p);
					if (data.length > max) {
						ChaosChatPlus.LOGGER.warn("GIF {} demasiado grande ({} KB, máximo {} KB)", file, data.length / 1024, max / 1024);
						continue;
					}
					add(found, name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"), name.replace('_', ' '), data);
				}
			}
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudieron leer los GIFs de {}", dir, e);
		}
		try (InputStream in = GifRegistry.class.getResourceAsStream("/assets/chaoschatplus/gifs/index.json")) {
			if (in != null) {
				for (JsonElement el : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) {
					JsonObject o = el.getAsJsonObject();
					String id = o.get("id").getAsString();
					try (InputStream gif = GifRegistry.class.getResourceAsStream("/assets/chaoschatplus/gifs/" + id + ".gif")) {
						if (gif != null && !found.containsKey(GifInfo.LOCAL + id)) {
							add(found, id, o.get("name").getAsString(), gif.readAllBytes());
						}
					}
				}
			}
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.error("No se pudieron leer los GIFs de serie", e);
		}
		gifs = found;
		ChaosChatPlus.LOGGER.info("GIFs propios: {}", gifs.size());
	}

	private static void add(Map<String, Entry> out, String key, String name, byte[] data) {
		int[] size = size(data);
		if (size == null) {
			ChaosChatPlus.LOGGER.warn("GIF {} no válido", key);
			return;
		}
		String id = GifInfo.LOCAL + key;
		out.put(id, new Entry(new GifInfo(id, name, "", size[0], size[1], ""), data));
	}

	@Nullable
	private static int[] size(byte[] data) {
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(in);
				return new int[] {reader.getWidth(0), reader.getHeight(0)};
			} finally {
				reader.dispose();
			}
		} catch (Exception e) {
			return null;
		}
	}

	public static List<GifInfo> all() {
		List<GifInfo> out = new ArrayList<>();
		gifs.values().forEach(e -> out.add(e.info()));
		return out;
	}

	public static List<GifInfo> search(String query) {
		String q = query.toLowerCase(Locale.ROOT).strip();
		List<GifInfo> out = new ArrayList<>();
		for (Entry e : gifs.values()) {
			if (q.isEmpty() || e.info().name().toLowerCase(Locale.ROOT).contains(q) || e.info().id().contains(q)) {
				out.add(e.info());
			}
		}
		return out;
	}

	@Nullable
	public static GifInfo info(String id) {
		Entry e = gifs.get(id);
		return e == null ? null : e.info();
	}

	@Nullable
	public static byte[] data(String id) {
		Entry e = gifs.get(id);
		return e == null ? null : e.data();
	}
}
