package com.chaos.chatplus.client.ui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.client.ChaosChatPlusClient;
import com.chaos.chatplus.client.ClientPrefs;
import com.chaos.chatplus.client.GifCache;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifServer;
import com.chaos.chatplus.mixin.client.ChatComponentAccessor;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * El chat con el diseño moderno: un único panel translúcido con esquinas redondeadas (en vez de una franja negra por
 * línea), mensajes nuevos que entran deslizándose, el mensaje bajo el ratón resaltado, las menciones a ti con una
 * barra dorada y una barra de scroll fina. Usa las mismas medidas que vanilla, así los clics y tooltips coinciden.
 */
public final class ModernChat {

	/** Duración de la animación de entrada (en ticks). */
	private static final float ENTER_TICKS = 3.0F;
	private static final int ERROR_TAG = 0xFF5555;

	/** Dónde se dibujó cada GIF en el último frame (en píxeles de la pantalla), para poder hacer clic. */
	private record GifRect(GifInfo gif, int x0, int y0, int x1, int y1) {}

	private static final List<GifRect> GIF_RECTS = new ArrayList<>();

	private ModernChat() {}

    private static boolean hidden() {
        return Minecraft.getInstance().options.chatVisibility().get() == net.minecraft.world.entity.player.ChatVisiblity.HIDDEN;
    }
    private static double chatX(ChatComponentAccessor acc, double x) { return x / acc.chaoschatplus$scale() - 4; }
    private static double chatY(ChatComponentAccessor acc, double y) {
        return (Minecraft.getInstance().getWindow().getGuiScaledHeight() - 40 - y) / acc.chaoschatplus$scale();
    }
    private static int messageEndAt(ChatComponentAccessor acc, double x, double y) {
        if (x < 0 || x > acc.chaoschatplus$width() / acc.chaoschatplus$scale() || y < 0) return -1;
        int line = (int)(y / acc.chaoschatplus$lineHeight()) + acc.chaoschatplus$scroll();
        if (line >= acc.chaoschatplus$lines().size() || line - acc.chaoschatplus$scroll() >= ((ChatComponent)(Object)acc).getLinesPerPage()) return -1;
        while (line + 1 < acc.chaoschatplus$lines().size() && !acc.chaoschatplus$lines().get(line).endOfEntry()) line++;
        return line;
    }
    private static double timeFactor(int age) {
        double factor = Math.max(0, Math.min(1, (1.0 - age / 200.0) * 10));
        return factor * factor;
    }

    @Nullable
    public static net.minecraft.network.chat.Style styleAt(double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        ChatComponent chat = mc.gui.getChat();
        ChatComponentAccessor acc = (ChatComponentAccessor) chat;
        double x = chatX(acc, mx), y = chatY(acc, my);
        if (x < 0 || x > acc.chaoschatplus$width() / acc.chaoschatplus$scale() || y < 0) return null;
        int row = (int)(y / acc.chaoschatplus$lineHeight());
        int index = row + acc.chaoschatplus$scroll();
        if (row >= chat.getLinesPerPage() || index >= acc.chaoschatplus$lines().size()) return null;
        net.minecraft.network.chat.Style[] found = {null};
        float[] cursor = {0};
        acc.chaoschatplus$lines().get(index).content().accept((i, style, cp) -> {
            cursor[0] += mc.font.width(net.minecraft.util.FormattedCharSequence.codepoint(cp, style));
            if (x < cursor[0]) { found[0] = style; return false; }
            return true;
        });
        return found[0];
    }

	/** El GIF bajo el ratón (o null). */
	@Nullable
	public static GifInfo gifAt(double mx, double my) {
		if (!enabled()) {
			return null;
		}
		for (GifRect r : GIF_RECTS) {
			if (mx >= r.x0() && mx < r.x1() && my >= r.y0() && my < r.y1()) {
				return r.gif();
			}
		}
		return null;
	}

	/** La marca de GIF de una línea del chat (inserción "chaoschatplus:gif:..."), si la tiene. */
	@Nullable
	private static GifInfo gifMark(GuiMessage.Line line) {
		String[] found = new String[1];
		line.content().accept((index, style, codepoint) -> {
			String ins = style.getInsertion();
			if (ins != null && ins.startsWith(GifServer.MARK)) {
				found[0] = ins.substring(GifServer.MARK.length());
				return false;
			}
			return true;
		});
		return found[0] == null ? null : GifInfo.decode(found[0]);
	}

	public static boolean enabled() {
		return ChaosChatPlusClient.ACTIVE && ClientPrefs.get().modernChat;
	}

	public static void render(ChatComponent chat, GuiGraphicsExtractor g, int tick, int mouseX, int mouseY, boolean focused) {
		ChatComponentAccessor acc = (ChatComponentAccessor) chat;
		if (hidden()) {
			return;
		}
		List<GuiMessage.Line> lines = acc.chaoschatplus$lines();
		int total = lines.size();
		if (total == 0) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.util.profiling.Profiler.get().push("chat");
		int perPage = chat.getLinesPerPage();
		int scroll = acc.chaoschatplus$scroll();
		float scale = (float) acc.chaoschatplus$scale();
		int width = Mth.ceil(acc.chaoschatplus$width() / scale);
		int bottom = Mth.floor((g.guiHeight() - 40) / scale);
		int hovered = messageEndAt(acc, chatX(acc, mouseX), chatY(acc, mouseY));
		double textOpacity = mc.options.chatOpacity().get() * 0.9F + 0.1F;
		double bgOpacity = mc.options.textBackgroundOpacity().get();
		double spacing = mc.options.chatLineSpacing().get();
		int lineHeight = acc.chaoschatplus$lineHeight();
		int textOffset = (int) Math.round(-8.0 * (spacing + 1.0) + 4.0 * spacing);
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean animate = ClientPrefs.get().animations;

		GIF_RECTS.clear();
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		g.pose().translate(4.0F, 0.0F);

		// Qué líneas se ven y con qué opacidad.
		int count = Math.min(perPage, total - scroll);
		double[] fade = new double[Math.max(0, count)];
		int topVisible = -1;
		double maxFade = 0;
		int visibleLines = 0;
		for (int u = 0; u < count; u++) {
			GuiMessage.Line line = lines.get(u + scroll);
			int age = tick - line.addedTime();
			if (age < 200 || focused) {
				double f = focused ? 1.0 : timeFactor(age);
				if ((int) (255.0 * f * textOpacity) > 3) {
					fade[u] = f;
					topVisible = u;
					maxFade = Math.max(maxFade, f);
					visibleLines++;
				}
			}
		}

		if (topVisible >= 0) {
			// Panel único detrás de las líneas visibles.
			int panelAlpha = (int) (255.0 * maxFade * bgOpacity);
			int top = bottom - (topVisible + 1) * lineHeight - 2;
			Ui.round(g, -4, top, width + 8, bottom + 2, 4, Ui.alpha(Ui.PANEL, panelAlpha));

			// Mensaje bajo el ratón.
			if (focused && hovered >= 0) {
				int first = hovered;
				int last = hovered;
				while (last + 1 < total && !lines.get(last + 1).endOfEntry()) {
					last++;
				}
				int yTop = bottom - (last - scroll + 1) * lineHeight;
				int yBottom = bottom - (first - scroll) * lineHeight;
				int clipTop = bottom - (topVisible + 1) * lineHeight;
				g.fill(-2, Math.max(yTop, clipTop), width + 6, Math.min(yBottom, bottom), Ui.HOVER);
			}
		}

		for (int u = 0; u < count; u++) {
			double f = fade[u];
			if (f <= 0) {
				continue;
			}
			int index = u + scroll;
			GuiMessage.Line line = lines.get(index);
			int alpha = (int) (255.0 * f * textOpacity);
			int lineBottom = bottom - u * lineHeight;
			int textY = lineBottom + textOffset;
			GuiMessageTag tag = line.tag();

			if (ChatTags.isMention(tag)) {
				g.fill(-2, lineBottom - lineHeight, width + 6, lineBottom, Ui.alpha(0xFFD54A, (int) (0x26 * f)));
				g.fill(-4, lineBottom - lineHeight, -2, lineBottom, Ui.alpha(0xFFD54A, alpha));
			} else if (tag != null && tag.indicatorColor() == ERROR_TAG) {
				g.fill(-4, lineBottom - lineHeight, -2, lineBottom, Ui.alpha(ERROR_TAG, alpha));
			}
			if (tag != null && tag.icon() != null && index == hovered) {
				GuiMessageTag.Icon icon = tag.icon();
				icon.extractRenderState(g, mc.font.width(line.content()) + 4, textY + 9 - icon.height - 1);
			}

			float slide = 0;
			if (animate && !focused) {
				float age = tick - line.addedTime() + partial;
				if (age < ENTER_TICKS) {
					float t = Math.max(0, age / ENTER_TICKS);
					slide = (1 - t) * (1 - t) * -10.0F;
					alpha = (int) (alpha * t);
				}
			}
			if (alpha <= 3) {
				continue;
			}
			g.pose().pushMatrix();
			g.nextStratum(); g.pose().translate(slide, 0.0F);
			g.text(mc.font, line.content(), 0, textY, 0xFFFFFF | (alpha << 24));
			GifInfo gif = gifMark(line);
			// El GIF ocupa esta línea y las vacías de debajo: solo se dibuja si se ven todas.
			if (gif != null && u >= GifServer.LINES - 1) {
				int boxH = GifServer.LINES * lineHeight - 2;
				// La caja se ajusta a la forma del GIF (sin franjas vacías a los lados).
				int fitW = (boxH - 4) * Math.max(1, gif.width()) / Math.max(1, gif.height()) + 4;
				int boxW = Math.min(Math.min(width - 4, boxH * 2), fitW);
				int top = lineBottom - lineHeight + 1;
				Ui.round(g, 0, top, boxW, top + boxH, 4, Ui.alpha(0x000000, (int) (alpha * 0.35)));
				int used = GifCache.draw(g, gif, 2, top + 2, boxW - 4, boxH - 4, false, 0xFFFFFF | (alpha << 24));
				GIF_RECTS.add(new GifRect(gif, (int) ((4 + slide) * scale), (int) (top * scale),
						(int) ((4 + slide + used + 2) * scale), (int) ((top + boxH) * scale)));
			}
			g.pose().popMatrix();
		}

		// Mensajes en espera (chat con retraso).
		long queued = mc.getChatListener().queueSize();
		if (queued > 0) {
			int a = (int) (128.0 * textOpacity);
			g.pose().pushMatrix();
			g.pose().translate(0.0F, bottom);
			Ui.round(g, -2, 0, width + 4, 10, 3, Ui.alpha(Ui.PANEL, (int) (255.0 * bgOpacity)));
			g.nextStratum(); g.pose().translate(0.0F, 0.0F);
			g.text(mc.font, Component.translatable("chat.queue", queued), 0, 1, 0xFFFFFF | (a << 24));
			g.pose().popMatrix();
		}

		// Barra de scroll fina.
		if (focused) {
			int contentH = total * lineHeight;
			int viewH = visibleLines * lineHeight;
			if (contentH != viewH && contentH > 0) {
				int barH = Math.max(6, viewH * viewH / contentH);
				int offset = scroll * viewH / total;
				int x = width + 5;
				int y1 = bottom - offset;
				int y0 = y1 - barH;
				int color = acc.chaoschatplus$newSinceScroll() ? Ui.ACCENT : 0xAAFFFFFF;
				Ui.round(g, x, y0, x + 2, y1, 1, color);
			}
		}

		g.pose().popMatrix();
		net.minecraft.util.profiling.Profiler.get().pop();
	}
}
