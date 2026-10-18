package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.chatplus.client.ShotCapture;


import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** En modo cámara no se pinta el HUD: solo el visor de la captura. */
@Mixin(Gui.class)
abstract class GuiMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$camera(GuiGraphics g, float partial, CallbackInfo ci) {
		if (ShotCapture.hideHud()) {
			ShotCapture.renderViewfinder(g);
			ci.cancel();
		}
	}
}
