package com.chaos.chatplus.client.ui;

import java.util.Locale;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.client.ChaosChatPlusClient;
import com.chaos.chatplus.client.ClientPrefs;
import com.chaos.chatplus.client.ClientState;
import com.chaos.chatplus.text.ChatMarks;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;

/**
 * Marca los mensajes que te mencionan (etiqueta dorada en el chat). Con el mod en el servidor se reconoce tu "@Nombre"
 * resaltado; en servidores sin el mod se busca "@TuNombre" en el texto y el aviso sonoro lo pone el cliente.
 */
public final class ChatTags {

	public static final GuiMessageTag MENTION = new GuiMessageTag(0xFFD54A, null,
			Lang.tr("mention.tag"), "Mention");

	private ChatTags() {}

	public static boolean isMention(@Nullable GuiMessageTag tag) {
		return tag != null && "Mention".equals(tag.logTag()) && tag.indicatorColor() == 0xFFD54A;
	}

	/** La etiqueta con la que se guarda el mensaje (la de mención si te mencionan). */
	@Nullable
	public static GuiMessageTag tagFor(Component message, @Nullable GuiMessageTag original) {
		if (!ChaosChatPlusClient.ACTIVE || !mentionsMe(message)) {
			return original;
		}
		if (!ClientState.serverMod() && ClientPrefs.get().mentionSound) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.3F));
		}
		return MENTION;
	}

	private static boolean mentionsMe(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return false;
		}
		if (ClientState.serverMod()) {
			TextColor self = TextColor.fromRgb(ChatMarks.SELF_MENTION_COLOR);
			return ChatMarks.any(message, c -> ChatMarks.mentionToken(c) != null
					&& c.getStyle().isBold() && self.equals(c.getStyle().getColor()));
		}
		String name = mc.player.getGameProfile().getName();
		Pattern p = Pattern.compile("(^|[^A-Za-z0-9_])@" + Pattern.quote(name.toLowerCase(Locale.ROOT)) + "($|[^A-Za-z0-9_])");
		return p.matcher(message.getString().toLowerCase(Locale.ROOT)).find();
	}
}
