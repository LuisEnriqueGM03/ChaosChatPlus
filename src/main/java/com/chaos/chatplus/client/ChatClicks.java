package com.chaos.chatplus.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.screen.EmojiUploadScreen;
import com.chaos.chatplus.client.screen.ItemPreviewScreen;
import com.chaos.chatplus.client.screen.ShotViewerScreen;
import com.chaos.chatplus.client.ui.Ui;
import com.chaos.chatplus.server.Shots;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/** Clics y hover especiales en el chat: capturas, ítems compartidos y el "Enviar al chat" de F2. */
public final class ChatClicks {

	private static final int THUMB_W = 160;

	private ChatClicks() {}

	@Nullable
	private static String shotId(@Nullable Style style) {
		if (style == null || style.getClickEvent() == null) {
			return null;
		}
		ClickEvent click = style.getClickEvent();
		if (click.getAction() == ClickEvent.Action.RUN_COMMAND && click.getValue().startsWith(Shots.VIEW_COMMAND)) {
			return click.getValue().substring(Shots.VIEW_COMMAND.length()).strip();
		}
		return null;
	}

	/** true si el clic lo resolvió el mod (y no hay que hacer lo de vanilla). */
	public static boolean handle(Style style, ChatScreen screen) {
		if (!ChaosChatPlusClient.ACTIVE) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		String id = shotId(style);
		if (id != null && ClientState.serverMod()) {
			mc.setScreen(new ShotViewerScreen(id, screen, false));
			return true;
		}
		ClickEvent click = style.getClickEvent();
		if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND && ClientState.serverMod()
				&& click.getValue().startsWith(Shots.REVIEW_COMMAND)) {
			String reviewId = click.getValue().substring(Shots.REVIEW_COMMAND.length()).strip();
			mc.setScreen(new ShotViewerScreen(reviewId, screen, true));
			return true;
		}
		if (click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND
				&& click.getValue().startsWith(ShotCapture.SEND_FILE_COMMAND)) {
			ShotCapture.openFile(click.getValue().substring(ShotCapture.SEND_FILE_COMMAND.length()));
			return true;
		}
		HoverEvent hover = style.getHoverEvent();
		if (click == null && hover != null && hover.getAction() == HoverEvent.Action.SHOW_ITEM) {
			HoverEvent.ItemStackInfo info = hover.getValue(HoverEvent.Action.SHOW_ITEM);
			if (info != null) {
				ItemStack stack = info.getItemStack();
				if (!stack.isEmpty()) {
					mc.setScreen(new ItemPreviewScreen(stack, screen));
					return true;
				}
			}
		}
		return false;
	}

	/** Miniatura de la captura al pasar el ratón por "[Ver]". */
	public static boolean renderHover(GuiGraphics g, @Nullable Style style, int mouseX, int mouseY) {
		String id = shotId(style);
		if (id == null || !ChaosChatPlusClient.ACTIVE || !ClientState.serverMod()) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		ShotCache.Shot shot = ShotCache.get(id);
		int w = THUMB_W;
		int h = shot == null ? 24 : Math.max(8, shot.height() * w / Math.max(1, shot.width()));
		int x = Math.min(mouseX + 10, g.guiWidth() - w - 6);
		int y = Math.max(4, mouseY - h - 14);
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		Ui.roundBordered(g, x - 3, y - 3, x + w + 3, y + h + 14, 4, Ui.POPUP, Ui.POPUP_BORDER);
		if (shot != null) {
			g.blit(ClientState.blurShots() ? shot.blurred() : shot.texture(), x, y, w, h, 0, 0, 1, 1, 1, 1);
			g.drawString(mc.font, Lang.tr("shot.by", shot.owner()), x, y + h + 3, Ui.MUTED, false);
		} else {
			String key = ShotCache.gone(id) ? "shot.gone" : "shot.loading";
			g.drawString(mc.font, Lang.tr(key), x + 4, y + 8, Ui.MUTED, false);
		}
		g.pose().popPose();
		return true;
	}

	/** Soltar un PNG en el chat = subir un emoji propio (si el servidor te deja). */
	public static void filesDropped(List<Path> paths, ChatScreen screen) {
		if (!ChaosChatPlusClient.ACTIVE || paths.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Path p = paths.get(0);
		if (!p.getFileName().toString().toLowerCase().endsWith(".png") || !Files.isRegularFile(p)) {
			return;
		}
		if (!ClientState.upload()) {
			if (mc.player != null) {
				mc.player.displayClientMessage(Lang.tr("emoji.no_permission").withStyle(ChatFormatting.RED), false);
			}
			return;
		}
		mc.setScreen(new EmojiUploadScreen(p, screen));
	}
}
