package com.chaos.chatplus.server;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.config.ChatConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Filtro automático opcional de capturas (moderation.provider en config.json).
 * Con "sightengine" cada captura se analiza con su modelo de desnudos; si no se puede verificar, se rechaza.
 */
public final class Moderation {

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
	private static final String[] NSFW_CLASSES = {"sexual_activity", "sexual_display", "erotica", "very_suggestive"};

	private Moderation() {}

	/** true = vale, false = NSFW, null = no se pudo comprobar. */
	public static CompletableFuture<Boolean> check(byte[] jpeg) {
		ChatConfig.Moderation cfg = ChatConfig.get().screenshots.moderation;
		if (!"sightengine".equalsIgnoreCase(cfg.provider)) {
			return CompletableFuture.completedFuture(true);
		}
		if (cfg.apiUser.isBlank() || cfg.apiSecret.isBlank()) {
			ChaosChatPlus.LOGGER.warn("moderation.provider es sightengine pero faltan apiUser/apiSecret: captura rechazada");
			return CompletableFuture.completedFuture(null);
		}
		String boundary = "----chaoschatplus" + UUID.randomUUID().toString().replace("-", "");
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		field(body, boundary, "models", "nudity-2.1");
		field(body, boundary, "api_user", cfg.apiUser);
		field(body, boundary, "api_secret", cfg.apiSecret);
		body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"media\"; filename=\"shot.jpg\"\r\n"
				+ "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		body.writeBytes(jpeg);
		body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.sightengine.com/1.0/check.json"))
				.timeout(Duration.ofSeconds(20))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
				.build();
		double threshold = cfg.threshold;
		return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
					if (!"success".equals(json.has("status") ? json.get("status").getAsString() : "")) {
						ChaosChatPlus.LOGGER.warn("Sightengine respondió con error: {}", response.body());
						return (Boolean) null;
					}
					JsonObject nudity = json.getAsJsonObject("nudity");
					double score = 0;
					for (String key : NSFW_CLASSES) {
						if (nudity != null && nudity.has(key)) {
							score = Math.max(score, nudity.get(key).getAsDouble());
						}
					}
					return score < threshold;
				})
				.exceptionally(e -> {
					ChaosChatPlus.LOGGER.warn("No se pudo consultar Sightengine", e);
					return null;
				});
	}

	private static void field(ByteArrayOutputStream body, String boundary, String name, String value) {
		body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
				+ value + "\r\n").getBytes(StandardCharsets.UTF_8));
	}
}
