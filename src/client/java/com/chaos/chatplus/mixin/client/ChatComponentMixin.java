package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.ui.ChatTags;
import com.chaos.chatplus.client.ui.ModernChat;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

/** Chat moderno (si está activo), etiqueta de mención y botón "Enviar al chat" tras una captura con F2. */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$modern(GuiGraphics g, int tick, int mouseX, int mouseY, CallbackInfo ci) {
		if (ModernChat.enabled()) {
			ModernChat.render((ChatComponent) (Object) this, g, tick, mouseX, mouseY,
					net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen);
			ci.cancel();
		}
	}

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true)
	private Component chaoschatplus$screenshotButton(Component message) {
		return ShotCapture.decorateScreenshotMessage(message);
	}

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private GuiMessageTag chaoschatplus$mentionTag(GuiMessageTag tag, Component message,
            MessageSignature signature, GuiMessageTag original) {
        return ChatTags.tagFor(message, tag);
    }
}
