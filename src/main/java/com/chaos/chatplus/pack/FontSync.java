package com.chaos.chatplus.pack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.ChatPayloads;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fuentes del chat para los clientes (todos llevan el mod): la de emojis (con los que añade el servidor) y, con Chaos
 * Ranks, sus etiquetas. Se mandan por el canal del mod y el cliente las carga sin paquete de recursos.
 */
public final class FontSync {

	/** Trozo máximo por paquete. */
	private static final int PART = 900_000;
	private static byte[] data = new byte[0];

	private FontSync() {}

	/** Vuelve a juntar las fuentes (al arrancar o si cambian los emojis). */
	public static synchronized void rebuild() {
		try {
			Map<String, byte[]> files = PackBuilder.files();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
				out.writeInt(files.size());
				for (Map.Entry<String, byte[]> e : files.entrySet()) {
					byte[] path = e.getKey().getBytes(StandardCharsets.UTF_8);
					out.writeInt(path.length);
					out.write(path);
					out.writeInt(e.getValue().length);
					out.write(e.getValue());
				}
			}
			data = bytes.toByteArray();
			ChaosChatPlus.LOGGER.info("Fuentes del chat listas ({} archivos, {} KB)", files.size(), data.length / 1024);
		} catch (IOException e) {
			ChaosChatPlus.LOGGER.error("No se pudieron preparar las fuentes del chat", e);
		}
	}

	public static void send(ServerPlayer player) {
		byte[] all = data;
		int parts = Math.max(1, (all.length + PART - 1) / PART);
		for (int i = 0; i < parts; i++) {
			byte[] chunk = Arrays.copyOfRange(all, i * PART, Math.min(all.length, (i + 1) * PART));
			ServerPlayNetworking.send(player, new ChatPayloads.FontPart(i, parts, chunk));
		}
	}

	public static void sendAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, ChatPayloads.FontPart.TYPE)) {
				send(player);
			}
		}
	}
}
