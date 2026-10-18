package com.chaos.chatplus.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Carga las fuentes que manda el servidor (emojis añadidos y etiquetas de Chaos Ranks) directamente en el gestor de
 * fuentes del juego, sin paquete de recursos ni recarga de texturas.
 */
public final class FontInjector {

	private static com.chaos.chatplus.net.PartAssembly parts;
	private static final Map<Identifier, FontSet> ORIGINALS = new HashMap<>();
	private static int fontGeneration;
	private static final Set<Identifier> INJECTED = new java.util.HashSet<>();
    private static final List<GlyphProvider> OWNED_PROVIDERS = new ArrayList<>();

    private static void invalidateCaches() {
        Minecraft mc = Minecraft.getInstance();
        for (var font : List.of(mc.font, mc.fontFilterFishy)) {
            var provider = ((com.chaos.chatplus.mixin.client.FontAccessor) font).chaoschatplus$provider();
            if (provider instanceof com.chaos.chatplus.mixin.client.FontCacheAccessor cached) cached.chaoschatplus$invalidate();
        }
    }

	private FontInjector() {}

	public static void accept(int part, int count, byte[] data) {
		try {
			if (part == 0) parts = new com.chaos.chatplus.net.PartAssembly(count, 16, 900_000, 12_000_000);
			if (parts == null) return;
			parts.accept(part, count, data);
			if (!parts.complete()) return;
			inject(com.chaos.chatplus.net.FontArchive.read(parts.join()));
			parts = null;
		} catch (Exception e) {
			parts = null;
			ChaosChatPlus.LOGGER.warn("No se pudieron cargar las fuentes del servidor", e);
		}
	}

	public static void clear() {
		parts = null;
		FontManager fonts = ((MinecraftAccessor) Minecraft.getInstance()).chaoschatplus$fontManager();
		Map<Identifier, FontSet> sets = ((FontManagerAccessor) fonts).chaoschatplus$fontSets();
		for (Identifier id : INJECTED) {
			FontSet injected = sets.remove(id);
			if (injected != null) injected.close();
			FontSet original = ORIGINALS.get(id);
			if (original != null) sets.put(id, original);
		}
		INJECTED.clear(); ORIGINALS.clear();
        OWNED_PROVIDERS.forEach(GlyphProvider::close);
        OWNED_PROVIDERS.clear();
        invalidateCaches();
	}

	private static void inject(Map<String, byte[]> files) throws Exception {
		Minecraft mc = Minecraft.getInstance();
		FontManager fonts = ((MinecraftAccessor) mc).chaoschatplus$fontManager();
		FontManagerAccessor access = (FontManagerAccessor) fonts;
		ResourceManager resources = new Files(files, mc.getResourceManager());
		// Restore previous local sets before replacing them with the new server snapshot.
		clear();
		int count = 0;
		for (Map.Entry<String, byte[]> f : files.entrySet()) {
			// assets/<ns>/font/<nombre>.json
			String[] p = f.getKey().split("/", 4);
			if (p.length != 4 || !p[0].equals("assets") || !p[2].equals("font") || !p[3].endsWith(".json")) {
				continue;
			}
			Identifier id = Identifier.fromNamespaceAndPath(p[1], p[3].substring(0, p[3].length() - 5));
			JsonObject json = JsonParser.parseString(new String(f.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
			List<GlyphProvider.Conditional> providers = new ArrayList<>();
			for (JsonElement el : json.getAsJsonArray("providers")) {
				GlyphProviderDefinition def = GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, el).getOrThrow();
				Optional<GlyphProviderDefinition.Loader> loader = def.unpack().left();
				if (loader.isPresent()) {
					GlyphProvider provider = loader.get().load(resources);
                    OWNED_PROVIDERS.add(provider);
					providers.add(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS));
				}
			}
			FontSet set = new FontSet(new net.minecraft.client.gui.font.GlyphStitcher(access.chaoschatplus$textureManager(), ChaosChatPlus.id("server_font/" + (++fontGeneration))));
			try {
				set.reload(providers, Set.of());
				FontSet previous = access.chaoschatplus$fontSets().put(id, set);
				if (previous != null) ORIGINALS.put(id, previous);
				INJECTED.add(id);
			} catch (Exception error) {
				set.close(); throw error;
			}
			count++;
		}
        invalidateCaches();
		ChaosChatPlus.LOGGER.info("Fuentes del servidor cargadas: {}", count);
	}

	/** Recursos en memoria; lo que no esté se busca en los del juego. */
	private record Files(Map<String, byte[]> files, ResourceManager fallback) implements ResourceManager {

		@Override
		public Optional<Resource> getResource(Identifier id) {
			byte[] data = files.get("assets/" + id.getNamespace() + "/" + id.getPath());
			if (data == null) {
				return fallback.getResource(id);
			}
			return Optional.of(new Resource(Minecraft.getInstance().getVanillaPackResources().fullResources(),
					() -> (InputStream) new ByteArrayInputStream(data)));
		}

		@Override
		public Set<String> getNamespaces() {
			return fallback.getNamespaces();
		}

		@Override
		public List<Resource> getResourceStack(Identifier id) {
			return getResource(id).map(List::of).orElse(List.of());
		}

		@Override
		public Map<Identifier, Resource> listResources(String path, ResourceManager.Selector filter) {
			return Map.of();
		}

		@Override
		public Map<Identifier, List<Resource>> listResourceStacks(String path,
				ResourceManager.Selector filter) {
			return Map.of();
		}

		@Override
		public java.util.stream.Stream<net.minecraft.server.packs.PackResources> listPacks() {
			return java.util.stream.Stream.empty();
		}
	}
}
