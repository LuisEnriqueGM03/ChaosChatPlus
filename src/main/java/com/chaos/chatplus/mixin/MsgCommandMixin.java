package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.chaos.chatplus.ChaosChatPlus;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import com.chaos.chatplus.server.ChatDecorator;
import com.chaos.chatplus.platform.Platform;

/** Privados (/msg, /tell, /w) con estilo: «✉ Luis » hola» para quien lo recibe y «✉ → Pepe » hola» para quien lo manda. */
@Mixin(MsgCommand.class)
abstract class MsgCommandMixin {

	@ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true)
	private static PlayerChatMessage chaoschatplus$decoratePrivate(PlayerChatMessage message,
			CommandSourceStack source, Collection<ServerPlayer> recipients, PlayerChatMessage original) {
		// Fabric already runs its content decorator for signed command arguments.
		if (Platform.get().loaded("fabricloader") || source.getPlayer() == null) return message;
		var content = message.decoratedContent();
		var decorated = ChatDecorator.decoratePrivate(source.getPlayer(), content, recipients);
		return decorated == content ? message : message.withUnsignedContent(decorated);
	}

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
