package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chaos.chatplus.client.ShotCapture;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

/** Zoom del modo cámara: reduce el campo de visión del mundo (la mano no cambia). */
@Mixin(GameRenderer.class)
abstract class CameraZoomMixin {

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void chaoschatplus$zoom(Camera camera, float partial, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
		if (useFovSetting && ShotCapture.cameraMode() && ShotCapture.zoom() > 1.0) {
			cir.setReturnValue(cir.getReturnValue() / ShotCapture.zoom());
		}
	}
}
