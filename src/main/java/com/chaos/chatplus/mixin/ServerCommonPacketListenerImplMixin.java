package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.pack.PackHost;
import com.mojang.authlib.GameProfile;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/** Respuestas de los clientes al pack de emojis: quién lo tiene cargado, y echar a quien lo rechace si es obligatorio. */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class ServerCommonPacketListenerImplMixin {

	@Shadow
	protected abstract GameProfile playerProfile();

	@Shadow
	public abstract void disconnect(Component reason);

	@Inject(method = "handleResourcePackResponse", at = @At("HEAD"))
	private void chaoschatplus$packResponse(ServerboundResourcePackPacket packet, CallbackInfo ci) {
		if (!PackHost.PACK_ID.equals(packet.id())) {
			return;
		}
		PackHost.onResponse(playerProfile().getId(), packet.action());
		if (packet.action() == ServerboundResourcePackPacket.Action.DECLINED && ChatConfig.get().resourcePack.required) {
			disconnect(Lang.tr("pack.required"));
		}
	}
}
