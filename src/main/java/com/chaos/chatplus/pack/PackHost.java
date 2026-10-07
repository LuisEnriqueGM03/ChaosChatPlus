package com.chaos.chatplus.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.mixin.ServerCommonPacketListenerImplAccessor;
import com.sun.net.httpserver.HttpServer;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;

/**
 * Sirve el resource pack de los emojis y lo manda a los jugadores (al entrar, durante la configuración, y cada vez
 * que cambian los emojis). Lleva su propio UUID, así convive con el pack de server.properties.
 * Apunta qué jugadores lo tienen cargado: a los demás los emojis les llegan como :nombre:.
 */
public final class PackHost {

	public static final UUID PACK_ID = UUID.nameUUIDFromBytes("chaoschatplus:emoji_pack".getBytes(StandardCharsets.UTF_8));

	private static final Map<UUID, Boolean> LOADED = new ConcurrentHashMap<>();

	private static volatile byte[] zip;
	private static volatile String sha1 = "";
	private static int builtVersion = -1;
	@Nullable private static HttpServer http;
	private static String mode = "none";

	private PackHost() {}

	public static void start(MinecraftServer server) {
		stop();
		ChatConfig.ResourcePack cfg = ChatConfig.get().resourcePack;
		mode = ChatConfig.get().emojis.enabled ? cfg.mode : "none";
		rebuild();
		if (mode.equals("embedded")) {
			try {
				HttpServer h = HttpServer.create(new InetSocketAddress(cfg.port), 0);
				h.createContext("/", exchange -> {
					byte[] data = zip;
					String path = exchange.getRequestURI().getPath();
					if (data == null || !path.equals("/" + fileName())) {
						exchange.sendResponseHeaders(404, -1);
						exchange.close();
						return;
					}
					exchange.getResponseHeaders().add("Content-Type", "application/zip");
					exchange.sendResponseHeaders(200, data.length);
					try (OutputStream out = exchange.getResponseBody()) {
						out.write(data);
					}
				});
				h.setExecutor(Executors.newFixedThreadPool(2, r -> {
					Thread t = new Thread(r, "ChaosChatPlus pack");
					t.setDaemon(true);
					return t;
				}));
				h.start();
				http = h;
				ChaosChatPlus.LOGGER.info("Pack de emojis servido en el puerto {} ({} KB)", cfg.port, zip.length / 1024);
			} catch (IOException e) {
				ChaosChatPlus.LOGGER.error("No se pudo abrir el puerto {} para el pack de emojis: los jugadores sin el "
						+ "mod verán :nombre:. Cambia resourcePack.port o usa mode \"external\"", cfg.port, e);
				mode = "none";
			}
		} else if (mode.equals("external") && cfg.externalUrl.isBlank()) {
			ChaosChatPlus.LOGGER.warn("resourcePack.mode es \"external\" pero externalUrl está vacío");
			mode = "none";
		}
	}

	public static void stop() {
		if (http != null) {
			http.stop(0);
			http = null;
		}
		LOADED.clear();
	}

	/** Rehace el zip si cambiaron los emojis. Devuelve true si cambió. */
	public static synchronized boolean rebuild() {
		if (builtVersion == EmojiRegistry.version() && zip != null) {
			return false;
		}
		try {
			byte[] data = PackBuilder.build();
			String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
			boolean changed = !hash.equals(sha1);
			zip = data;
			sha1 = hash;
			builtVersion = EmojiRegistry.version();
			return changed;
		} catch (IOException | NoSuchAlgorithmException e) {
			ChaosChatPlus.LOGGER.error("No se pudo montar el pack de emojis", e);
			return false;
		}
	}

	public static boolean active() {
		return !mode.equals("none") && zip != null;
	}

	public static String hash() {
		return sha1;
	}

	/** Guarda el zip en un archivo (para subirlo a otro sitio con mode "external"). */
	public static Path export() throws IOException {
		rebuild();
		Path out = ChatConfig.dir().resolve("chaoschatplus-emojis.zip");
		Files.write(out, zip);
		return out;
	}

	private static String fileName() {
		return "chaoschatplus-" + sha1 + ".zip";
	}

	/** URL del pack para un jugador: publicHost, o la dirección con la que él entró al servidor. */
	@Nullable
	public static String url(@Nullable Connection connection) {
		ChatConfig.ResourcePack cfg = ChatConfig.get().resourcePack;
		if (mode.equals("external")) {
			return cfg.externalUrl;
		}
		if (!mode.equals("embedded")) {
			return null;
		}
		String host = cfg.publicHost.strip();
		if (host.isEmpty() && connection instanceof ConnectionHost h) {
			host = h.chaoschatplus$host();
		}
		if (host == null || host.isBlank()) {
			host = "127.0.0.1";
		}
		int nul = host.indexOf('\0');
		if (nul >= 0) {
			host = host.substring(0, nul);
		}
		if (host.endsWith(".")) {
			host = host.substring(0, host.length() - 1);
		}
		if (host.contains(":") && !host.startsWith("[")) {
			host = "[" + host + "]";
		}
		return "http://" + host + ":" + cfg.port + "/" + fileName();
	}

	@Nullable
	private static ClientboundResourcePackPushPacket pushPacket(@Nullable Connection connection) {
		if (!active()) {
			return null;
		}
		String url = url(connection);
		if (url == null) {
			return null;
		}
		ChatConfig.ResourcePack cfg = ChatConfig.get().resourcePack;
		Optional<Component> prompt = cfg.prompt.isBlank() ? Optional.empty() : Optional.of(Component.literal(cfg.prompt));
		return new ClientboundResourcePackPushPacket(PACK_ID, url, sha1, cfg.required, prompt);
	}

	/** Fabric: añade el envío del pack a la fase de configuración (antes de entrar al mundo). */
	public static void configure(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		ClientboundResourcePackPushPacket packet =
				pushPacket(((ServerCommonPacketListenerImplAccessor) handler).chaoschatplus$connection());
		if (packet != null) {
			handler.addTask(new PackTask(packet));
		}
	}

	/** Vuelve a mandar el pack a todos (tras añadir o quitar emojis). */
	public static void pushAll(MinecraftServer server) {
		if (!rebuild()) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ClientboundResourcePackPushPacket packet =
					pushPacket(((ServerCommonPacketListenerImplAccessor) player.connection).chaoschatplus$connection());
			if (packet != null) {
				LOADED.remove(player.getUUID());
				player.connection.send(packet);
			}
		}
	}

	/** Respuesta de un cliente a nuestro pack. */
	public static void onResponse(UUID player, ServerboundResourcePackPacket.Action action) {
		switch (action) {
			case SUCCESSFULLY_LOADED -> LOADED.put(player, true);
			case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> LOADED.remove(player);
			default -> { }
		}
	}

	public static boolean hasPack(ServerPlayer player) {
		return LOADED.containsKey(player.getUUID());
	}

	public static void forget(ServerPlayer player) {
		LOADED.remove(player.getUUID());
	}

	/**
	 * Tarea de configuración que manda el pack y espera la respuesta. Usa el mismo tipo que la de vanilla para que
	 * el código de vanilla la dé por terminada cuando el cliente responde.
	 */
	private record PackTask(ClientboundResourcePackPushPacket packet) implements ConfigurationTask {

		@Override
		public void start(Consumer<Packet<?>> consumer) {
			consumer.accept(packet);
		}

		@Override
		public Type type() {
			return ServerResourcePackConfigurationTask.TYPE;
		}
	}

	/** Lo implementa Connection (mixin): la dirección que escribió el jugador para entrar. */
	public interface ConnectionHost {
		@Nullable
		String chaoschatplus$host();

		void chaoschatplus$setHost(String host);
	}
}
