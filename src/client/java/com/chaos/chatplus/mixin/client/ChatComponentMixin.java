package com.chaos.chatplus.mixin.client;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.chaos.chatplus.client.ShotCapture;
import com.chaos.chatplus.client.ui.ChatTags;
import com.chaos.chatplus.client.ui.ModernChat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"), cancellable = true)
    private void chaoschatplus$modern(GuiGraphicsExtractor g, Font font, int tick, int mx, int my, ChatComponent.DisplayMode mode, boolean insertion, CallbackInfo ci) {
        if (ModernChat.enabled()) {
            ModernChat.render((ChatComponent)(Object)this, g, tick, mx, my, mode != ChatComponent.DisplayMode.BACKGROUND);
            ci.cancel();
        }
    }
    @ModifyVariable(method = "addClientSystemMessage", at = @At("HEAD"), argsOnly = true)
    private Component chaoschatplus$screenshotButton(Component message) { return ShotCapture.decorateScreenshotMessage(message); }
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true)
    private GuiMessageTag chaoschatplus$mentionTag(GuiMessageTag tag, Component message) { return ChatTags.tagFor(message, tag); }
}
