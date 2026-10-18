package com.chaos.chatplus.server;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.perm.Perms;
import com.chaos.chatplus.text.ChatMarks;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Convierte lo que escribe un jugador: :emoji: y emojis Unicode → glifos, @nombre → mención (con aviso al
 * mencionado) y [item] → el ítem de la mano con su tooltip. Va en la fase de contenido, antes del formato de
 * Chaos Ranks (fase de estilo), que envuelve el resultado.
 */
public final class ChatDecorator {

	private static final Pattern TOKEN = Pattern.compile(
			":([a-zA-Z0-9_+\\-]{1,32}):|@([A-Za-z0-9_]{1,16})|\\[(item|i)]", Pattern.CASE_INSENSITIVE);

	private ChatDecorator() {}


	/** Lo que pasó al decorar un mensaje. */
	private static final class Context {
		final ServerPlayer sender;
		final ChatConfig cfg = ChatConfig.get();
		final Set<ServerPlayer> pinged = new LinkedHashSet<>();
		boolean changed;
		boolean everyone;
		boolean itemShared;

		Context(ServerPlayer sender) {
			this.sender = sender;
		}
	}

	public static Component decorate(ServerPlayer sender, Component message) {
		return decorate(sender, message, null);
	}

	/** Native loader chat events do not decorate commands. Restrict private mention notifications to recipients. */
	public static Component decoratePrivate(ServerPlayer sender, Component message, java.util.Collection<ServerPlayer> recipients) {
		return decorate(sender, message, recipients);
	}

	private static Component decorate(ServerPlayer sender, Component message, java.util.Collection<ServerPlayer> recipients) {
		Context ctx = new Context(sender);
		MutableComponent out = Component.empty();
		message.visit((style, text) -> {
			segment(out, style, text, ctx);
			return Optional.empty();
		}, Style.EMPTY);
		if (!ctx.changed) {
			return message;
		}
		if (ctx.everyone) {
			for (ServerPlayer p : sender.level().getServer().getPlayerList().getPlayers()) {
				ctx.pinged.add(p);
			}
		}
		ctx.pinged.remove(sender);
		if (recipients != null) ctx.pinged.retainAll(recipients);
		for (ServerPlayer target : ctx.pinged) {
			Mentions.ping(target, sender);
		}
		if (ctx.itemShared) {
			ItemShare.markUsed(sender);
		}
		return out;
	}

	private static void segment(MutableComponent out, Style style, String text, Context ctx) {
		Matcher m = TOKEN.matcher(text);
		int last = 0;
		while (m.find()) {
			plain(out, style, text.substring(last, m.start()), ctx);
			Component token = token(m, style, ctx);
			if (token != null) {
				out.append(token);
				ctx.changed = true;
			} else {
				plain(out, style, m.group(), ctx);
			}
			last = m.end();
		}
		plain(out, style, text.substring(last), ctx);
	}

	/** Texto sin tokens: solo se buscan emojis Unicode. */
	private static void plain(MutableComponent out, Style style, String text, Context ctx) {
		if (text.isEmpty()) {
			return;
		}
		if (!ctx.cfg.emojis.enabled || !ctx.cfg.emojis.convertUnicode) {
			out.append(Component.literal(text).setStyle(style));
			return;
		}
		StringBuilder buf = new StringBuilder();
		int i = 0;
		while (i < text.length()) {
			char ch = text.charAt(i);
			if (ch >= 0x2000) {
				Map.Entry<Emoji, Integer> hit = EmojiRegistry.unicodeAt(text, i);
				if (hit != null && Perms.inRanksOrAll(ctx.sender, hit.getKey().ranks())) {
					if (!buf.isEmpty()) {
						out.append(Component.literal(buf.toString()).setStyle(style));
						buf.setLength(0);
					}
					out.append(ChatMarks.emoji(hit.getKey()));
					ctx.changed = true;
					i += hit.getValue();
					continue;
				}
			}
			buf.append(ch);
			i++;
		}
		if (!buf.isEmpty()) {
			out.append(Component.literal(buf.toString()).setStyle(style));
		}
	}

	@org.jetbrains.annotations.Nullable
	private static Component token(Matcher m, Style style, Context ctx) {
		if (m.group(1) != null) {
			if (!ctx.cfg.emojis.enabled) {
				return null;
			}
			Emoji e = EmojiRegistry.byName(m.group(1));
			return e != null && Perms.inRanksOrAll(ctx.sender, e.ranks()) ? ChatMarks.emoji(e) : null;
		}
		if (m.group(2) != null) {
			return mention(m.group(2), ctx);
		}
		return item(ctx);
	}

	@org.jetbrains.annotations.Nullable
	private static Component mention(String name, Context ctx) {
		if (!ctx.cfg.mentions.enabled) {
			return null;
		}
		int color = ctx.cfg.mentionRgb();
		String lower = name.toLowerCase(Locale.ROOT);
		if ((lower.equals("everyone") || lower.equals("here")) && Perms.canMentionEveryone(ctx.sender)) {
			ctx.everyone = true;
			return ChatMarks.mention("@" + lower, color);
		}
		ServerPlayer target = ctx.sender.level().getServer().getPlayerList().getPlayerByName(name);
		if (target != null) {
			ctx.pinged.add(target);
			return ChatMarks.mention("@" + target.getGameProfile().name(), color);
		}
		if (Perms.canMentionRanks(ctx.sender)) {
			for (ChaosRanksCompat.Rank rank : ChaosRanksCompat.ranks()) {
				if (rank.id().equalsIgnoreCase(name)) {
					for (ServerPlayer p : ctx.sender.level().getServer().getPlayerList().getPlayers()) {
						ChaosRanksCompat.Rank r = ChaosRanksCompat.rankOf(p);
						if (r != null && r.id().equals(rank.id())) {
							ctx.pinged.add(p);
						}
					}
					return ChatMarks.mention("@" + rank.id(), rank.bgColor());
				}
			}
		}
		return null;
	}

	@org.jetbrains.annotations.Nullable
	private static Component item(Context ctx) {
		if (!ctx.cfg.items.chatToken || ctx.itemShared || !Perms.canItems(ctx.sender) || !ItemShare.ready(ctx.sender)) {
			return null;
		}
		ItemStack stack = ctx.sender.getMainHandItem();
		if (stack.isEmpty()) {
			return null;
		}
		ctx.itemShared = true;
		return ItemShare.component(stack);
	}

	/** Nombres que valen como mención (para que el cliente los autocomplete). */
	public static List<String> specialMentions(ServerPlayer player) {
		return Perms.canMentionEveryone(player) ? List.of("everyone", "here") : List.of();
	}
}
