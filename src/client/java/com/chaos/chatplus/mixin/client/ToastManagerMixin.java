package com.chaos.chatplus.mixin.client;

import com.chaos.chatplus.client.ShotCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toasts render outside the HUD and must also stay out of camera captures. */
@Mixin(ToastManager.class)
abstract class ToastManagerMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void chaoschatplus$camera(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ShotCapture.hideHud()) ci.cancel();
    }
}
