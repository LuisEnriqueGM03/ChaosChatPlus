package com.chaos.chatplus.server;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.text.ChatMarks;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;

/**
 * Mensajes del servidor con estilo: muertes (☠ después del nombre, asesino en rojo y [Ver ítem] en el arma) y
 * entrada/salida («➜ Luis entró al servidor», bienvenida la primera vez, «← Luis salió»).
 * Los emojis van como iconos decorativos: a quien no pueda verlos se le quitan solos (ChatTransform).
 */
public final class ServerMessages {

	private static final int DEATH_TEXT = 0xD3D6DD;
	private static final int KILLER = 0xFF8A80;
	private static final int SKULL = 0xE8E8E8;
	private static final int JOIN = 0x6BE38E;
	private static final int FIRST_JOIN = 0xFFD54A;
	private static final int LEAVE = 0x9AA0A6;

	private ServerMessages() {}

	private static Component icon(String name) {
		Emoji e = EmojiRegistry.byName(name);
		return e == null ? Component.empty() : Component.literal(" ").append(ChatMarks.icon(e));
	}

	/** Mensaje de muerte con estilo (si es uno de vanilla "death.*"; si no, tal cual). */
	public static Component death(Component message) {
		if (!ChatConfig.get().messages.deaths || !(message.getContents() instanceof TranslatableContents t)
				|| !t.getKey().startsWith("death.") || t.getArgs().length == 0) {
			return message;
		}
		Object[] args = t.getArgs().clone();
		if (args[0] instanceof Component victim) {
			args[0] = Component.empty().append(victim).append(Component.literal(" ☠").withStyle(style -> style.withColor(SKULL)));
		}
		if (args.length > 1 && args[1] instanceof Component killer) {
			args[1] = Component.empty().withStyle(style -> style.withColor(KILLER)).append(killer);
		}
		if (args.length > 2 && args[2] instanceof Component weapon) {
			ItemStack stack = itemOf(weapon);
			if (stack != null) {
				args[2] = Component.empty().append(weapon).append(" ").append(ItemShare.viewLink(stack));
			}
		}
		return MutableComponent.create(new TranslatableContents(t.getKey(), t.getFallback(), args))
				.setStyle(message.getStyle().withColor(DEATH_TEXT));
	}

	@Nullable
	private static ItemStack itemOf(Component c) {
		HoverEvent hover = c.getStyle().getHoverEvent();
		if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_ITEM) {
			HoverEvent.ItemStackInfo info = hover.getValue(HoverEvent.Action.SHOW_ITEM);
			return info == null ? null : info.getItemStack();
		}
		for (Component s : c.getSiblings()) {
			ItemStack found = itemOf(s);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** «➜ [OP] Luis entró al servidor», o la bienvenida si es la primera vez. */
	public static Component join(Component original, ServerPlayer player) {
		if (!ChatConfig.get().messages.joinLeave) {
			return original;
		}
		boolean first = player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0
				&& player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) < 20;
		if (first) {
			return Component.literal("✦ ").withStyle(style -> style.withColor(FIRST_JOIN))
					.append(Lang.tr("join.first", player.getDisplayName()).withStyle(style -> style.withColor(FIRST_JOIN)));
		}
		return Component.literal("➜ ").withStyle(style -> style.withColor(JOIN)).append(Lang.tr("join.joined", player.getDisplayName()).withStyle(style -> style.withColor(JOIN)));
	}

	public static Component leave(Component original, ServerPlayer player) {
		if (!ChatConfig.get().messages.joinLeave) {
			return original;
		}
		return Component.literal("← ").withStyle(style -> style.withColor(LEAVE)).append(Lang.tr("join.left", player.getDisplayName()).withStyle(style -> style.withColor(LEAVE)));
	}
}
