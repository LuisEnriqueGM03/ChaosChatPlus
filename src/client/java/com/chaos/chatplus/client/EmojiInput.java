package com.chaos.chatplus.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.text.ChatMarks;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Emojis dentro de la barra de escribir: al completar ":nombre:" se cambia por el carácter del emoji (se ve como
 * imagen y se borra de una pulsación), y al enviar se vuelve a escribir ":nombre:" para el servidor.
 */
public final class EmojiInput {

	private static final Pattern CODE = Pattern.compile(":([a-z0-9_+\\-]{1,32}):");
	private static final Style EMOJI_STYLE = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(ChatMarks.EMOJI_FONT));

	private EmojiInput() {}

    public static FormattedCharSequence format(String text, int offset) {
        if (!hasGlyph(text) || text.startsWith("/")) return null;
        return wrap((part, start) -> FormattedCharSequence.forward(part, Style.EMPTY)).apply(text, offset);
    }

	/** Resultado de convertir: texto nuevo y dónde queda el cursor. */
	public record Converted(String text, int cursor) {}

	/** ¿Este cliente puede dibujar el emoji? (los de serie vienen en el jar; los del servidor, en su pack) */
	private static boolean drawable(ClientState.ClientEmoji e) {
		return e.builtin() || ClientState.pack();
	}

	/** Cambia los ":nombre:" completos por su carácter. Null si no hay nada que cambiar. */
	@Nullable
	public static Converted toGlyphs(String text, int cursor) {
		if (!ClientState.serverMod() || text.startsWith("/") || text.indexOf(':') < 0) {
			return null;
		}
		Matcher m = CODE.matcher(text);
		StringBuilder out = new StringBuilder();
		int last = 0;
		int newCursor = cursor;
		boolean changed = false;
		while (m.find()) {
			ClientState.ClientEmoji e = ClientState.emoji(m.group(1));
			if (e == null || !drawable(e)) {
				continue;
			}
			out.append(text, last, m.start()).appendCodePoint(e.codepoint());
			int removed = m.group().length() - Character.charCount(e.codepoint());
			if (cursor >= m.end()) {
				newCursor -= removed;
			} else if (cursor > m.start()) {
				newCursor = out.length();
			}
			last = m.end();
			changed = true;
		}
		if (!changed) {
			return null;
		}
		out.append(text.substring(last));
		return new Converted(out.toString(), Math.max(0, Math.min(newCursor, out.length())));
	}

	/** Vuelve a escribir los caracteres de emoji como ":nombre:" (para enviar). */
	public static String toCodes(String text) {
		if (!hasGlyph(text)) {
			return text;
		}
		StringBuilder out = new StringBuilder();
		text.codePoints().forEach(cp -> {
			ClientState.ClientEmoji e = isGlyph(cp) ? ClientState.emojiByCodepoint(cp) : null;
			if (e != null) {
				out.append(e.code());
			} else {
				out.appendCodePoint(cp);
			}
		});
		return out.toString();
	}

	public static boolean isGlyph(int cp) {
		return cp >= 0xE000 && cp <= 0xF8FF;
	}

	private static boolean hasGlyph(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (isGlyph(text.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/** Formatter del campo de texto: los emojis con su fuente, el resto como lo pinte vanilla. */
	public static BiFunction<String, Integer, FormattedCharSequence> wrap(BiFunction<String, Integer, FormattedCharSequence> original) {
		return (text, offset) -> {
			if (!hasGlyph(text) || text.startsWith("/")) {
				return original.apply(text, offset);
			}
			List<FormattedCharSequence> parts = new ArrayList<>();
			int start = 0;
			for (int i = 0; i < text.length(); i++) {
				if (isGlyph(text.charAt(i))) {
					if (i > start) {
						parts.add(original.apply(text.substring(start, i), offset + start));
					}
					parts.add(FormattedCharSequence.codepoint(text.charAt(i), EMOJI_STYLE));
					start = i + 1;
				}
			}
			if (start < text.length()) {
				parts.add(original.apply(text.substring(start), offset + start));
			}
			return FormattedCharSequence.composite(parts);
		};
	}
}
