package com.chaos.chatplus.server;

import java.util.Locale;

import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.pack.PackHost;
import com.chaos.chatplus.text.ChatMarks;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ajusta cada mensaje para quien lo recibe (solo el contenido sin firmar, la firma no se toca):
 * <ul>
 * <li>Emojis que no puede ver (sin el pack; o con el mod pero sin el pack y el emoji es propio del servidor) →
 * ":nombre:" en texto.</li>
 * <li>Su "@Nombre" (o @everyone/@here/@surango) → amarillo y en negrita, para que lo vea aunque no tenga el mod.</li>
 * </ul>
 */
public final class ChatTransform {

	private ChatTransform() {}

	public static Component forRecipient(ServerPlayer viewer, Component message) {
		if (!ChatMarks.any(message, c -> ChatMarks.isEmoji(c.getStyle()) || ChatMarks.mentionToken(c) != null)) {
			return message;
		}
		boolean pack = PackHost.hasPack(viewer);
		boolean modded = ChatServer.isModded(viewer);
		String me = "@" + viewer.getGameProfile().getName().toLowerCase(Locale.ROOT);
		String rank = ChaosRanksCompat.present() ? ChaosRanksCompat.rankId(viewer) : null;
		String myRank = rank == null ? null : "@" + rank.toLowerCase(Locale.ROOT);
		return ChatMarks.rebuild(message, node -> {
			if (ChatMarks.isEmoji(node.getStyle())) {
				String text = ChatMarks.literal(node);
				Emoji emoji = text == null || text.isEmpty() ? null : EmojiRegistry.byCodepoint(text.codePointAt(0));
				boolean visible = pack || (modded && emoji != null && emoji.builtin());
				if (visible) {
					return node;
				}
				String code = emoji != null ? emoji.code() : node.getStyle().getInsertion();
				return Component.literal(code == null ? "?" : code)
						.setStyle(node.getStyle().withFont(null).withColor((TextColor) null));
			}
			String token = ChatMarks.mentionToken(node);
			if (token != null && (token.equals(me) || token.equals("@everyone") || token.equals("@here")
					|| token.equals(myRank))) {
				return Component.literal(ChatMarks.literal(node))
						.setStyle(node.getStyle().withColor(ChatMarks.SELF_MENTION_COLOR).withBold(true));
			}
			return node;
		});
	}
}
