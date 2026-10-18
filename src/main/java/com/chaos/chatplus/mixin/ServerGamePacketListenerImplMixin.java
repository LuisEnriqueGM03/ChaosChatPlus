package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.chaos.chatplus.server.ChatTransform;
import com.chaos.chatplus.server.ServerMessages;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Cada jugador recibe su versión del mensaje: emojis en texto si no puede verlos y su mención resaltada.
 * Solo cambia el contenido sin firmar; la firma y el resto del mensaje quedan igual.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {

	@Shadow
	public ServerPlayer player;

	@ModifyVariable(method = "sendPlayerChatMessage", at = @At("HEAD"), argsOnly = true)
	private PlayerChatMessage chaoschatplus$perViewer(PlayerChatMessage message) {
		Component unsigned = message.unsignedContent();
		if (unsigned == null) {
			return message;
		}
		Component adapted = ChatTransform.forRecipient(player, unsigned);
		return adapted == unsigned ? message : message.withUnsignedContent(adapted);
	}

	@ModifyArg(method = "onDisconnect", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
			index = 0)
	private Component chaoschatplus$leave(Component message) {
		return ServerMessages.leave(message, player);
	}

	@ModifyVariable(method = "sendDisguisedChatMessage", at = @At("HEAD"), argsOnly = true)
	private Component chaoschatplus$perViewerDisguised(Component message) {
		return ChatTransform.forRecipient(player, message);
	}
}
