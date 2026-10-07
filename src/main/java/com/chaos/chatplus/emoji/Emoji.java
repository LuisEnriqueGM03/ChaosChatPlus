package com.chaos.chatplus.emoji;

import java.util.List;

/**
 * Un emoji: se escribe :nombre: (o un alias, o su emoji Unicode) y se dibuja con el carácter {@code codepoint} de la
 * fuente chaoschatplus:emoji. Los de serie vienen en el jar; los propios del servidor van en config/chaoschatplus/emojis.
 */
public record Emoji(String name, List<String> aliases, String category, int codepoint, boolean builtin,
		List<String> ranks, List<String> unicode) {

	public static final String CUSTOM_CATEGORY = "servidor";

	public String glyph() {
		return Character.toString(codepoint);
	}

	public String code() {
		return ":" + name + ":";
	}
}
