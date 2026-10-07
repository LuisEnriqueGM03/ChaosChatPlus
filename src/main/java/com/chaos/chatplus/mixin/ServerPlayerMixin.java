package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.chaos.chatplus.server.ChatTransform;
import com.chaos.chatplus.server.ServerMessages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;

/** Mensajes del sistema adaptados a cada jugador (emojis que puede ver) y muertes con estilo. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

	@ModifyVariable(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), argsOnly = true)
	private Component chaoschatplus$perViewer(Component message) {
		return ChatTransform.forRecipient((ServerPlayer) (Object) this, message);
	}

	@WrapOperation(method = "die", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
	private Component chaoschatplus$death(CombatTracker tracker, Operation<Component> original) {
		return ServerMessages.death(original.call(tracker));
	}
}
