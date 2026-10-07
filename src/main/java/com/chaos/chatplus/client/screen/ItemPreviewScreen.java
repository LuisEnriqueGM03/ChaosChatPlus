package com.chaos.chatplus.client.screen;

import java.util.List;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ui.FlatButton;
import com.chaos.chatplus.client.ui.Ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Vista ampliada de un ítem compartido en el chat: el ítem grande y todo su tooltip al lado. */
public class ItemPreviewScreen extends Screen {

	private static final int W = 260;
	private static final int H = 150;

	private final ItemStack stack;
	private final Screen parent;
	private List<FormattedCharSequence> lines = List.of();
	private int left;
	private int top;

	public ItemPreviewScreen(ItemStack stack, Screen parent) {
		super(stack.getHoverName());
		this.stack = stack;
		this.parent = parent;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		List<Component> tooltip = minecraft != null && minecraft.player != null
				? stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player,
						minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
				: List.of(stack.getHoverName());
		lines = tooltip.stream().flatMap(c -> font.split(c, W - 96).stream()).toList();
		addRenderableWidget(new FlatButton(left + W - 64, top + H - 22, 56, 14, Lang.tr("button.close"), false,
				b -> onClose()));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		super.render(g, mouseX, mouseY, partial);
		Ui.roundBordered(g, left, top, left + W, top + H, 6, Ui.POPUP, Ui.POPUP_BORDER);
		Ui.round(g, left + 8, top + 8, left + 80, top + 80, 6, 0x22FFFFFF);

		// Ítem grande con un leve vaivén.
		float bob = Mth.sin((System.currentTimeMillis() % 3000L) / 3000.0F * Mth.TWO_PI) * 2.0F;
		g.pose().pushPose();
		g.pose().translate(left + 44, top + 44 + bob, 100);
		g.pose().scale(4.0F, 4.0F, 4.0F);
		g.renderItem(stack, -8, -8);
		g.pose().popPose();
		if (stack.getCount() > 1) {
			g.drawString(font, "×" + stack.getCount(), left + 10, top + 84, Ui.TEXT, false);
		}

		int y = top + 10;
		for (FormattedCharSequence line : lines) {
			if (y > top + H - 30) {
				g.drawString(font, "…", left + 88, y, Ui.MUTED, false);
				break;
			}
			g.drawString(font, line, left + 88, y, 0xFFFFFFFF, true);
			y += 10;
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
		g.fill(0, 0, width, height, 0x88000000);
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
