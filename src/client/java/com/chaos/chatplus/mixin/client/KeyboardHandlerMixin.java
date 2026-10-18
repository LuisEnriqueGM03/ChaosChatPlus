package com.chaos.chatplus.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.client.ShotCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;

/** Modo cámara: Enter captura y Esc cancela (sin abrir el menú de pausa). */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$camera(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        int key = event.key();
		if (!ShotCapture.cameraMode() || Minecraft.getInstance().gui.screen() != null || action != 1) {
			return;
		}
		if (key == InputConstants.KEY_ESCAPE) { // Esc
			ShotCapture.cancel();
			ci.cancel();
		} else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { // Enter
			ShotCapture.shoot();
			ci.cancel();
		}
	}
}
