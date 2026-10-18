package com.chaos.chatplus.client;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.OnlineGifs;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;

/**
 * Búsquedas en Klipy desde el juego del jugador (como pide Klipy: las peticiones salen del cliente, no de un servidor
 * intermedio). La clave la da el servidor. Sin caché: cada búsqueda se pide de nuevo.
 */
public final class KlipyClient {

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final int LIMIT = 24;
	/** Búsquedas por minuto de este jugador (para no gastar de golpe la clave del servidor). */
	private static final int PER_MINUTE = 10;
	private static final Deque<Long> CALLS = new ArrayDeque<>();

	/** Resultado: los GIFs, o {@code limited} si vas demasiado rápido. */
	public record Result(String query, List<GifInfo> gifs, boolean limited) {}

	private KlipyClient() {}

	/** Busca ("" = destacados) y llama a {@code done} en el hilo del juego. */
	public static void search(String query, Consumer<Result> done) {
		var connection = Minecraft.getInstance().getConnection();
		String key = ClientState.klipyKey();
		if (key.isEmpty()) {
			done.accept(new Result(query, List.of(), false));
			return;
		}
		long now = Util.getMillis();
		while (!CALLS.isEmpty() && now - CALLS.peekFirst() > 60_000) {
			CALLS.pollFirst();
		}
		if (CALLS.size() >= PER_MINUTE) {
			done.accept(new Result(query, List.of(), true));
			return;
		}
		CALLS.addLast(now);
		String q = query.strip();
		String base = q.isEmpty() ? "https://api.klipy.com/v2/featured?"
				: "https://api.klipy.com/v2/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&";
		String url = base + "key=" + URLEncoder.encode(key, StandardCharsets.UTF_8)
				+ "&client_key=chaoschatplus&limit=" + LIMIT + "&media_filter=tinygif,gif"
				+ "&locale=" + Minecraft.getInstance().getLanguageManager().getSelected();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
				.header("User-Agent", "ChaosChatPlus/1.2 (Minecraft mod)").GET().build();
		HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) ->
				Minecraft.getInstance().execute(() -> {
					if (Minecraft.getInstance().getConnection() != connection) return;
					List<GifInfo> gifs = List.of();
					if (error == null && response.statusCode() == 200) {
						try {
							gifs = OnlineGifs.parse(response.body());
						} catch (Exception e) {
							ChaosChatPlus.LOGGER.warn("Respuesta de Klipy no válida", e);
						}
					} else {
						ChaosChatPlus.LOGGER.warn("Klipy respondió {}", error != null ? error.toString() : response.statusCode());
					}
					done.accept(new Result(query, gifs, false));
				}));
	}
}
