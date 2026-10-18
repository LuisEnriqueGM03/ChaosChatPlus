package com.chaos.chatplus.server;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Aviso al mencionado: sonido y texto en la action bar. Funciona también con clientes vanilla. */
public final class Mentions {

	private Mentions() {}

	public static void ping(ServerPlayer target, ServerPlayer sender) {
		ChatConfig.Mentions cfg = ChatConfig.get().mentions;
		Identifier sound = cfg.sound.isBlank() ? null : Identifier.tryParse(cfg.sound);
		if (sound != null) {
			target.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(SoundEvent.createVariableRangeEvent(sound)), SoundSource.PLAYERS, target.getX(), target.getY(), target.getZ(), cfg.volume, cfg.pitch, target.getRandom().nextLong()));
		}
		if (cfg.actionBar) {
			target.sendOverlayMessage(Lang.tr("mention.actionbar", sender.getDisplayName()));
		}
	}
}
