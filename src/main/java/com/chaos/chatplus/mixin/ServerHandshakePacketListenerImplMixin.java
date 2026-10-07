package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.pack.PackHost;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;

/** Apunta el host que escribió el cliente (play.miserver.com) para servirle el pack por esa misma dirección. */
@Mixin(ServerHandshakePacketListenerImpl.class)
abstract class ServerHandshakePacketListenerImplMixin {

	@Shadow
	@Final
	private Connection connection;

	@Inject(method = "handleIntention", at = @At("HEAD"))
	private void chaoschatplus$rememberHost(ClientIntentionPacket packet, CallbackInfo ci) {
		((PackHost.ConnectionHost) connection).chaoschatplus$setHost(packet.hostName());
	}
}
