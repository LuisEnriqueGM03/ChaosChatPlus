package com.chaos.chatplus.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.mixin.client.FontManagerAccessor;
import com.chaos.chatplus.mixin.client.MinecraftAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Carga las fuentes que manda el servidor (emojis añadidos y etiquetas de Chaos Ranks) directamente en el gestor de
 * fuentes del juego, sin paquete de recursos ni recarga de texturas.
 */
public final class FontInjector {

	private static byte[][] parts = new byte[0][];
	private static final List<GlyphProvider> LOADED = new ArrayList<>();

	private FontInjector() {}

	public static void accept(int part, int count, byte[] data) {
		if (part == 0 || parts.length != count) {
			parts = new byte[count][];
		}
		if (part < 0 || part >= count) {
			return;
		}
		parts[part] = data;
		for (byte[] p : parts) {
			if (p == null) {
				return;
			}
		}
		try {
			ByteArrayOutputStream all = new ByteArrayOutputStream();
			for (byte[] p : parts) {
				all.write(p);
			}
			inject(read(all.toByteArray()));
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("No se pudieron cargar las fuentes del servidor", e);
		}
		parts = new byte[0][];
	}

	private static Map<String, byte[]> read(byte[] gz) throws Exception {
		Map<String, byte[]> files = new HashMap<>();
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(gz)))) {
			int n = in.readInt();
			for (int i = 0; i < n; i++) {
				byte[] path = in.readNBytes(in.readInt());
				byte[] data = in.readNBytes(in.readInt());
				files.put(new String(path, StandardCharsets.UTF_8), data);
			}
		}
		return files;
	}

	private static void inject(Map<String, byte[]> files) throws Exception {
		Minecraft mc = Minecraft.getInstance();
		FontManager fonts = ((MinecraftAccessor) mc).chaoschatplus$fontManager();
		FontManagerAccessor access = (FontManagerAccessor) fonts;
		ResourceManager resources = new Files(files, mc.getResourceManager());
		LOADED.forEach(GlyphProvider::close);
		LOADED.clear();
		int count = 0;
		for (Map.Entry<String, byte[]> f : files.entrySet()) {
			// assets/<ns>/font/<nombre>.json
			String[] p = f.getKey().split("/", 4);
			if (p.length != 4 || !p[0].equals("assets") || !p[2].equals("font") || !p[3].endsWith(".json")) {
				continue;
			}
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(p[1], p[3].substring(0, p[3].length() - 5));
			JsonObject json = JsonParser.parseString(new String(f.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
			List<GlyphProvider.Conditional> providers = new ArrayList<>();
			for (JsonElement el : json.getAsJsonArray("providers")) {
				GlyphProviderDefinition def = GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, el).getOrThrow();
				Optional<GlyphProviderDefinition.Loader> loader = def.unpack().left();
				if (loader.isPresent()) {
					GlyphProvider provider = loader.get().load(resources);
					LOADED.add(provider);
					providers.add(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS));
				}
			}
			FontSet set = access.chaoschatplus$fontSets()
					.computeIfAbsent(id, k -> new FontSet(access.chaoschatplus$textureManager(), k));
			set.reload(providers, Set.of());
			count++;
		}
		ChaosChatPlus.LOGGER.info("Fuentes del servidor cargadas: {}", count);
	}

	/** Recursos en memoria; lo que no esté se busca en los del juego. */
	private record Files(Map<String, byte[]> files, ResourceManager fallback) implements ResourceManager {

		@Override
		public Optional<Resource> getResource(ResourceLocation id) {
			byte[] data = files.get("assets/" + id.getNamespace() + "/" + id.getPath());
			if (data == null) {
				return fallback.getResource(id);
			}
			return Optional.of(new Resource(Minecraft.getInstance().getVanillaPackResources(),
					() -> (InputStream) new ByteArrayInputStream(data)));
		}

		@Override
		public Set<String> getNamespaces() {
			return fallback.getNamespaces();
		}

		@Override
		public List<Resource> getResourceStack(ResourceLocation id) {
			return getResource(id).map(List::of).orElse(List.of());
		}

		@Override
		public Map<ResourceLocation, Resource> listResources(String path, java.util.function.Predicate<ResourceLocation> filter) {
			return Map.of();
		}

		@Override
		public Map<ResourceLocation, List<Resource>> listResourceStacks(String path,
				java.util.function.Predicate<ResourceLocation> filter) {
			return Map.of();
		}

		@Override
		public java.util.stream.Stream<net.minecraft.server.packs.PackResources> listPacks() {
			return java.util.stream.Stream.empty();
		}
	}
}
