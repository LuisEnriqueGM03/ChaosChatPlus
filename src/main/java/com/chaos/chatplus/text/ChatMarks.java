package com.chaos.chatplus.text;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.Lang;
import com.chaos.chatplus.emoji.Emoji;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

/**
 * Cómo se marcan los emojis y las menciones dentro de un mensaje (lo usan el servidor y el cliente):
 * <ul>
 * <li>Emoji: el carácter del emoji en la fuente chaoschatplus:emoji, en blanco, con inserción ":nombre:".</li>
 * <li>Mención: "@Nombre" en el color de las menciones, con inserción "@Nombre" (Mayús+clic la escribe en el chat).</li>
 * </ul>
 */
public final class ChatMarks {

	public static final ResourceLocation EMOJI_FONT = ChaosChatPlus.id("emoji");
	/** Color de "@TuNombre" cuando la mención es para ti. */
	public static final int SELF_MENTION_COLOR = 0xFFD54A;

	private ChatMarks() {}

	public static MutableComponent emoji(Emoji emoji) {
		return Component.literal(emoji.glyph()).withStyle(Style.EMPTY
				.withFont(EMOJI_FONT)
				.withColor(0xFFFFFF)
				.withInsertion(emoji.code())
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(emoji.code()))));
	}

	/** Emoji decorativo (en muertes, entradas, capturas...): sin inserción; si quien lo recibe no lo puede ver, se quita. */
	public static MutableComponent icon(Emoji emoji) {
		return Component.literal(emoji.glyph()).withStyle(Style.EMPTY.withFont(EMOJI_FONT).withColor(0xFFFFFF));
	}

	public static boolean isEmoji(Style style) {
		return EMOJI_FONT.equals(style.getFont());
	}

	public static MutableComponent mention(String token, int color) {
		return Component.literal(token).withStyle(Style.EMPTY
				.withColor(color)
				.withInsertion(token)
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Lang.tr("mention.hover", token))));
	}

	/** El "@algo" de una mención (en minúsculas), o null si ese trozo no es una mención. */
	@Nullable
	public static String mentionToken(Component leaf) {
		String insertion = leaf.getStyle().getInsertion();
		if (insertion == null || !insertion.startsWith("@") || isEmoji(leaf.getStyle())) {
			return null;
		}
		String text = literal(leaf);
		return insertion.equals(text) ? insertion.toLowerCase(Locale.ROOT) : null;
	}

	/** Texto propio del nodo si es literal (sin hijos), o null. */
	@Nullable
	public static String literal(Component c) {
		return c.getContents() instanceof PlainTextContents.LiteralContents lit ? lit.text() : null;
	}

	/**
	 * Copia del árbol del texto cambiando los nodos que {@code map} quiera (devuelve el nodo nuevo, o el mismo si no
	 * cambia). Los hijos se copian detrás de cada nodo, igual que en el original.
	 */
	public static Component rebuild(Component c, UnaryOperator<Component> map) {
		ComponentContents contents = c.getContents();
		if (contents instanceof TranslatableContents t && t.getArgs().length > 0) {
			Object[] args = t.getArgs().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Component arg) {
					args[i] = rebuild(arg, map);
				}
			}
			contents = new TranslatableContents(t.getKey(), t.getFallback(), args);
		}
		MutableComponent self = MutableComponent.create(contents).setStyle(c.getStyle());
		Component mapped = map.apply(self);
		MutableComponent out = mapped instanceof MutableComponent m && mapped != self ? m.copy() : self;
		for (Component sibling : c.getSiblings()) {
			out.append(rebuild(sibling, map));
		}
		return out;
	}

	/** ¿Hay algún nodo que cumpla esto? (también dentro de los argumentos de textos traducibles, como los del chat) */
	public static boolean any(Component c, Predicate<Component> test) {
		if (test.test(c)) {
			return true;
		}
		if (c.getContents() instanceof TranslatableContents t) {
			for (Object arg : t.getArgs()) {
				if (arg instanceof Component ac && any(ac, test)) {
					return true;
				}
			}
		}
		for (Component sibling : c.getSiblings()) {
			if (any(sibling, test)) {
				return true;
			}
		}
		return false;
	}
}
