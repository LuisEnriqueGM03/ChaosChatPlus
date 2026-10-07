package com.chaos.chatplus.server;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Aviso al mencionado: sonido y texto en la action bar. Funciona también con clientes vanilla. */
public final class Mentions {

	private Mentions() {}

	public static void ping(ServerPlayer target, ServerPlayer sender) {
		ChatConfig.Mentions cfg = ChatConfig.get().mentions;
		ResourceLocation sound = cfg.sound.isBlank() ? null : ResourceLocation.tryParse(cfg.sound);
		if (sound != null) {
			target.playNotifySound(SoundEvent.createVariableRangeEvent(sound), SoundSource.PLAYERS, cfg.volume, cfg.pitch);
		}
		if (cfg.actionBar) {
			target.displayClientMessage(Lang.tr("mention.actionbar", sender.getDisplayName()), true);
		}
	}
}
