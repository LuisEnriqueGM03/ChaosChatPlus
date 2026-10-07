package com.chaos.chatplus.client.screen;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.net.ChatPayloads;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * "Mostrar ítem": tu inventario (armadura, mano secundaria, inventario y barra rápida). Clic en un ítem = se muestra
 * en el chat. El servidor lee el ítem de verdad de ese hueco; aquí solo se elige cuál.
 */
public class ItemPickerScreen extends Screen {

	private static final int SLOT = 18;
	private static final int PAD = 8;
	private static final int W = 9 * SLOT + PAD * 2;
	private static final int H = 14 + SLOT + 6 + 3 * SLOT + 4 + SLOT + PAD + 12;

	private final Screen parent;
	private int left;
	private int top;

	public ItemPickerScreen(Screen parent) {
		super(Lang.tr("item.picker_title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	/** Posición en pantalla de cada hueco (o null si no se dibuja). */
	@Nullable
	private int[] slotPos(int slot) {
		int gx = left + PAD;
		if (slot >= 36 && slot <= 39) {
			// Armadura: casco, peto, pantalones, botas (de izquierda a derecha).
			return new int[] {gx + (39 - slot) * SLOT, top + 14};
		}
		if (slot == Inventory.SLOT_OFFHAND) {
			return new int[] {gx + 5 * SLOT, top + 14};
		}
		if (slot >= 9 && slot < 36) {
			int i = slot - 9;
			return new int[] {gx + (i % 9) * SLOT, top + 14 + SLOT + 6 + (i / 9) * SLOT};
		}
		if (slot >= 0 && slot < 9) {
			return new int[] {gx + slot * SLOT, top + 14 + SLOT + 6 + 3 * SLOT + 4};
		}
		return null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		Ui.roundBordered(g, left, top, left + W, top + H, 6, Ui.POPUP, Ui.POPUP_BORDER);
		g.drawString(font, title, left + PAD, top + 4, Ui.TEXT, false);
		if (minecraft == null || minecraft.player == null) {
			return;
		}
		Inventory inv = minecraft.player.getInventory();
		ItemStack hovered = ItemStack.EMPTY;
		for (int slot = 0; slot <= Inventory.SLOT_OFFHAND; slot++) {
			int[] p = slotPos(slot);
			if (p == null) {
				continue;
			}
			ItemStack stack = inv.getItem(slot);
			boolean hover = Ui.inside(mouseX, mouseY, p[0], p[1], p[0] + SLOT - 1, p[1] + SLOT - 1);
			int bg = hover && !stack.isEmpty() ? 0x55FFFFFF : 0x22FFFFFF;
			Ui.round(g, p[0], p[1], p[0] + SLOT - 1, p[1] + SLOT - 1, 3, bg);
			if (!stack.isEmpty()) {
				g.renderItem(stack, p[0] + 1, p[1] + 1);
				g.renderItemDecorations(font, stack, p[0] + 1, p[1] + 1);
				if (hover) {
					hovered = stack;
				}
			}
		}
		g.drawString(font, Lang.tr("item.picker_hint"), left + PAD, top + H - 11, Ui.MUTED, false);
		if (!hovered.isEmpty()) {
			g.renderTooltip(font, hovered, mouseX, mouseY);
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0x88000000);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (minecraft != null && minecraft.player != null && button == 0) {
			Inventory inv = minecraft.player.getInventory();
			for (int slot = 0; slot <= Inventory.SLOT_OFFHAND; slot++) {
				int[] p = slotPos(slot);
				if (p != null && Ui.inside(mx, my, p[0], p[1], p[0] + SLOT - 1, p[1] + SLOT - 1) && !inv.getItem(slot).isEmpty()) {
					ClientPlayNetworking.send(new ChatPayloads.ShareItem(slot, ClientState.dmTargetOrEmpty()));
					minecraft.setScreen(null);
					return true;
				}
			}
			if (!Ui.inside(mx, my, left, top, left + W, top + H)) {
				onClose();
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
