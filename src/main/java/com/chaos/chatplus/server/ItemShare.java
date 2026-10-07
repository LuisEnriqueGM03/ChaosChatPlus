package com.chaos.chatplus.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.perm.Perms;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Mostrar ítems en el chat. El servidor siempre lee el ítem real del inventario del jugador (no se fía de lo que diga
 * el cliente) y lo manda con su tooltip de vanilla, así lo ven completo también los clientes sin el mod.
 */
public final class ItemShare {

	/** Huecos válidos: 0-8 barra, 9-35 inventario, 36-39 armadura, 40 mano secundaria. */
	public static final int SLOTS = 41;

	private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

	private ItemShare() {}

	/** Comando del enlace [Ver ítem] (el cliente con el mod lo intercepta y abre la vista ampliada). */
	public static final String VIEW_COMMAND = "/chatplus item view";
	public static final int VIEW_COLOR = 0x55AAFF;

	/** "Espada de diamante ×3 [Ver ítem]": el nombre en el color de su rareza; el ítem se ve con [Ver ítem]. */
	public static MutableComponent component(ItemStack stack) {
		MutableComponent name = stack.getHoverName().copy().withStyle(stack.getRarity().color());
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			name.withStyle(ChatFormatting.ITALIC);
		}
		MutableComponent out = Component.empty().append(name);
		if (stack.getCount() > 1) {
			out.append(Component.literal(" ×" + stack.getCount()).withStyle(ChatFormatting.GRAY));
		}
		return out.append(" ").append(viewLink(stack));
	}

	/** "[Ver ítem]" en azul: hover con el ítem y clic para verlo en grande. */
	public static MutableComponent viewLink(ItemStack stack) {
		return Lang.tr("item.view").withStyle(s -> s
				.withColor(VIEW_COLOR)
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)))
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, VIEW_COMMAND)));
	}

	public static boolean ready(ServerPlayer player) {
		long cooldown = ChatConfig.get().items.cooldownSeconds * 1000L;
		Long last = LAST_USE.get(player.getUUID());
		return last == null || player.hasPermissions(2) || System.currentTimeMillis() - last >= cooldown;
	}

	private static long secondsLeft(ServerPlayer player) {
		long cooldown = ChatConfig.get().items.cooldownSeconds * 1000L;
		Long last = LAST_USE.get(player.getUUID());
		return last == null ? 0 : Math.max(1, (cooldown - (System.currentTimeMillis() - last) + 999) / 1000);
	}

	public static void markUsed(ServerPlayer player) {
		LAST_USE.put(player.getUUID(), System.currentTimeMillis());
	}

	public static void forget(ServerPlayer player) {
		LAST_USE.remove(player.getUUID());
	}

	/** Botón "Mostrar ítem" del chat: comparte el ítem de ese hueco con todos. */
	public static void shareSlot(ServerPlayer player, int slot) {
		if (!Perms.canItems(player)) {
			player.sendSystemMessage(Lang.tr("item.no_permission").withStyle(ChatFormatting.RED));
			return;
		}
		if (slot < 0 || slot >= SLOTS) {
			return;
		}
		if (!ready(player)) {
			player.sendSystemMessage(Lang.tr("cooldown", secondsLeft(player)).withStyle(ChatFormatting.RED));
			return;
		}
		ItemStack stack = player.getInventory().getItem(slot).copy();
		if (stack.isEmpty()) {
			return;
		}
		markUsed(player);
		Component item = component(stack);
		player.server.sendSystemMessage(Lang.tr("item.shared", player.getDisplayName(), "", item));
		for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
			viewer.sendSystemMessage(Lang.tr("item.shared", player.getDisplayName(), ChatTransform.icon(viewer, "espada"), item));
		}
	}
}
