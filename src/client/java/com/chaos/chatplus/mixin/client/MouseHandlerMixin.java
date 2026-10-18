package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.client.ShotCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

/** En modo cámara la rueda hace zoom (en vez de cambiar de hueco en la barra). */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$zoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (ShotCapture.cameraMode() && Minecraft.getInstance().gui.screen() == null && vertical != 0) {
			ShotCapture.scrollZoom(vertical);
			ci.cancel();
		}
	}
}
