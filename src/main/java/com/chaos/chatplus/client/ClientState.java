package com.chaos.chatplus.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.ChatPayloads;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.network.chat.Component;

/** Lo que el servidor le ha dicho a este cliente (null/false si el servidor no tiene Chaos ChatPlus). */
public final class ClientState {

	/** Un emoji que este jugador puede usar. */
	public record ClientEmoji(String name, List<String> aliases, String category, int codepoint, boolean builtin) {

		public String code() {
			return ":" + name + ":";
		}

		public boolean matches(String query) {
			if (name.startsWith(query)) {
				return true;
			}
			for (String a : aliases) {
				if (a.startsWith(query)) {
					return true;
				}
			}
			return false;
		}
	}

	private static boolean serverMod;
	private static boolean items;
	private static boolean shots;
	private static boolean upload;
	private static boolean mentions = true;
	private static boolean pack;
	private static boolean blurShots;
	private static int mentionColor = 0x7AA2FF;
	private static int maxShotKb = 512;
	private static List<String> specialMentions = List.of();
	private static List<ClientEmoji> emojis = List.of();
	private static Map<String, ClientEmoji> byName = Map.of();
	@Nullable private static Component prefix;
	@Nullable private static Component prefixFallback;

	private ClientState() {}

	public static void setInfo(byte[] data) {
		try {
			JsonObject o = JsonParser.parseString(ChatPayloads.gunzip(data)).getAsJsonObject();
			items = bool(o, "items");
			shots = bool(o, "shots");
			upload = bool(o, "upload");
			mentions = bool(o, "mentions");
			pack = bool(o, "pack");
			blurShots = bool(o, "blurShots");
			mentionColor = o.has("mentionColor") ? o.get("mentionColor").getAsInt() : 0x7AA2FF;
			maxShotKb = o.has("maxShotKb") ? o.get("maxShotKb").getAsInt() : 512;
			List<String> special = new ArrayList<>();
			if (o.has("specialMentions")) {
				o.getAsJsonArray("specialMentions").forEach(e -> special.add(e.getAsString()));
			}
			specialMentions = List.copyOf(special);
			List<ClientEmoji> list = new ArrayList<>();
			Map<String, ClientEmoji> names = new HashMap<>();
			if (o.has("emojis")) {
				for (JsonElement el : o.getAsJsonArray("emojis")) {
					JsonObject j = el.getAsJsonObject();
					List<String> aliases = new ArrayList<>();
					j.getAsJsonArray("a").forEach(a -> aliases.add(a.getAsString()));
					ClientEmoji e = new ClientEmoji(j.get("n").getAsString(), List.copyOf(aliases), j.get("c").getAsString(),
							j.get("p").getAsInt(), j.get("b").getAsBoolean());
					list.add(e);
					names.put(e.name(), e);
				}
			}
			emojis = List.copyOf(list);
			byName = names;
			serverMod = true;
		} catch (Exception e) {
			ChaosChatPlus.LOGGER.warn("Información del servidor no válida", e);
		}
	}

	private static boolean bool(JsonObject o, String key) {
		return o.has(key) && o.get(key).getAsBoolean();
	}

	public static void setPrefix(@Nullable Component prefix, @Nullable Component fallback) {
		ClientState.prefix = prefix;
		ClientState.prefixFallback = fallback;
	}

	public static void reset() {
		serverMod = false;
		items = shots = upload = pack = blurShots = false;
		mentions = true;
		specialMentions = List.of();
		emojis = List.of();
		byName = Map.of();
		prefix = prefixFallback = null;
	}

	public static boolean serverMod() {
		return serverMod;
	}

	public static boolean items() {
		return serverMod && items;
	}

	public static boolean shots() {
		return serverMod && shots;
	}

	public static boolean upload() {
		return serverMod && upload;
	}

	public static boolean mentions() {
		return mentions;
	}

	public static boolean pack() {
		return pack;
	}

	public static boolean blurShots() {
		return blurShots;
	}

	public static int mentionColor() {
		return mentionColor;
	}

	public static int maxShotKb() {
		return maxShotKb;
	}

	public static List<String> specialMentions() {
		return specialMentions;
	}

	public static List<ClientEmoji> emojis() {
		return emojis;
	}

	@Nullable
	public static ClientEmoji emoji(String name) {
		return byName.get(name.toLowerCase(Locale.ROOT));
	}

	/** Prefijo para la barra de escribir: con las fuentes de Chaos Ranks si están, si no el de texto plano. */
	@Nullable
	public static Component prefix() {
		return ChaosChatPlusClient.CHAOS_RANKS ? prefix : prefixFallback;
	}
}
