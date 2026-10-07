package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.server.ServerMessages;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;

/** «➜ Luis entró al servidor» en lugar del aviso amarillo de vanilla. */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {

	@Unique
	private static final ThreadLocal<ServerPlayer> chaoschatplus$joining = new ThreadLocal<>();

	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void chaoschatplus$remember(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		chaoschatplus$joining.set(player);
	}

	@Inject(method = "placeNewPlayer", at = @At("RETURN"))
	private void chaoschatplus$forget(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		chaoschatplus$joining.remove();
	}

	@ModifyArg(method = "placeNewPlayer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"),
			index = 0)
	private Component chaoschatplus$join(Component message) {
		ServerPlayer player = chaoschatplus$joining.get();
		return player == null ? message : ServerMessages.join(message, player);
	}
}
