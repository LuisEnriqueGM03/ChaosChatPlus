package com.chaos.chatplus.mixin.client;

import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chaos.chatplus.client.ChatClicks;
import com.chaos.chatplus.client.EmojiInput;
import com.chaos.chatplus.client.SelfTest;
import com.chaos.chatplus.client.ui.ChatBar;
import com.chaos.chatplus.client.screen.GifViewerScreen;
import com.chaos.chatplus.client.ui.ModernChat;
import com.chaos.chatplus.gif.GifInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** La pantalla de escribir (tecla T): barra moderna, botones, autocompletado y clics especiales en el chat. */
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen implements SelfTest.BarAccess {

	@Shadow
	protected EditBox input;

	@Unique
	@Nullable
	private ChatBar chaoschatplus$bar;
	@Unique
	private int chaoschatplus$mouseX;
	@Unique
	private int chaoschatplus$mouseY;

	protected ChatScreenMixin(Component title) {
		super(title);
	}

	@Override
	@Nullable
	public ChatBar chaoschatplus$bar() {
		return chaoschatplus$bar;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void chaoschatplus$init(CallbackInfo ci) {
		chaoschatplus$bar = ModernChat.enabled() ? new ChatBar((ChatScreen) (Object) this, input) : null;
		if (chaoschatplus$bar != null) {
			chaoschatplus$bar.onEdited(); // por si el chat se abre ya con texto
		}
	}

	@Inject(method = "onEdited", at = @At("TAIL"))
	private void chaoschatplus$edited(String value, CallbackInfo ci) {
		if (chaoschatplus$bar != null) {
			chaoschatplus$bar.onEdited();
		}
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void chaoschatplus$mouse(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		chaoschatplus$mouseX = mouseX;
		chaoschatplus$mouseY = mouseY;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void chaoschatplus$bar(GuiGraphics g, int x0, int y0, int x1, int y1, int color, Operation<Void> original) {
		if (chaoschatplus$bar != null) {
			chaoschatplus$bar.renderBar(g, chaoschatplus$mouseX, chaoschatplus$mouseY);
		} else {
			original.call(g, x0, y0, x1, y1, color);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void chaoschatplus$overlays(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
		if (chaoschatplus$bar != null) {
			chaoschatplus$bar.renderOverlays(g, mouseX, mouseY);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentHoverEffect(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Style;II)V"))
	private void chaoschatplus$hover(GuiGraphics g, Font font, @Nullable Style style, int x, int y, Operation<Void> original) {
		if (!ChatClicks.renderHover(g, style, x, y)) {
			original.call(g, font, style, x, y);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$keys(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (chaoschatplus$bar != null && chaoschatplus$bar.keyPressed(key)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$click(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
		if (chaoschatplus$bar != null && chaoschatplus$bar.mouseClicked(mx, my, button)) {
			cir.setReturnValue(true);
			return;
		}
		GifInfo gif = ModernChat.gifAt(mx, my);
		if (gif != null && button == 0) {
			Minecraft.getInstance().setScreen(new GifViewerScreen(gif, (ChatScreen) (Object) this));
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void chaoschatplus$scroll(double mx, double my, double h, double v, CallbackInfoReturnable<Boolean> cir) {
		if (chaoschatplus$bar != null && chaoschatplus$bar.mouseScrolled(mx, my, v)) {
			cir.setReturnValue(true);
		}
	}

	@ModifyVariable(method = "handleChatInput", at = @At("HEAD"), argsOnly = true)
	private String chaoschatplus$codes(String message) {
		return EmojiInput.toCodes(message);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		if (chaoschatplus$bar != null) {
			if (chaoschatplus$bar.charTypedPicker(c)) {
				return true;
			}
			chaoschatplus$bar.charTyped();
		}
		return super.charTyped(c, modifiers);
	}

	@Override
	public boolean handleComponentClicked(@Nullable Style style) {
		if (style != null && !hasShiftDown() && ChatClicks.handle(style, (ChatScreen) (Object) this)) {
			return true;
		}
		return super.handleComponentClicked(style);
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		ChatClicks.filesDropped(paths, (ChatScreen) (Object) this);
	}
}
