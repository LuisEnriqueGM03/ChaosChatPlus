package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.chaos.chatplus.ChaosChatPlus;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.MsgCommand;

/** Privados (/msg, /tell, /w) con estilo: «✉ Luis » hola» para quien lo recibe y «✉ → Pepe » hola» para quien lo manda. */
@Mixin(MsgCommand.class)
abstract class MsgCommandMixin {

	@Unique
	private static final ResourceKey<ChatType> INCOMING = ResourceKey.create(Registries.CHAT_TYPE, ChaosChatPlus.id("dm_incoming"));
	@Unique
	private static final ResourceKey<ChatType> OUTGOING = ResourceKey.create(Registries.CHAT_TYPE, ChaosChatPlus.id("dm_outgoing"));

	@ModifyArg(method = "sendMessage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/chat/ChatType;bind(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/commands/CommandSourceStack;)Lnet/minecraft/network/chat/ChatType$Bound;"),
			index = 0)
	private static ResourceKey<ChatType> chaoschatplus$style(ResourceKey<ChatType> key) {
		if (key == ChatType.MSG_COMMAND_INCOMING) {
			return INCOMING;
		}
		if (key == ChatType.MSG_COMMAND_OUTGOING) {
			return OUTGOING;
		}
		return key;
	}
}
