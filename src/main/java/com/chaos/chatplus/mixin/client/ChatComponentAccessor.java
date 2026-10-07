package com.chaos.chatplus.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

	@Accessor("trimmedMessages")
	List<GuiMessage.Line> chaoschatplus$lines();

	@Accessor("chatScrollbarPos")
	int chaoschatplus$scroll();

	@Accessor("newMessageSinceScroll")
	boolean chaoschatplus$newSinceScroll();

	@Invoker("getLineHeight")
	int chaoschatplus$lineHeight();

	@Invoker("isChatHidden")
	boolean chaoschatplus$hidden();

	@Invoker("getMessageEndIndexAt")
	int chaoschatplus$messageEndAt(double chatX, double chatY);

	@Invoker("screenToChatX")
	double chaoschatplus$toChatX(double x);

	@Invoker("screenToChatY")
	double chaoschatplus$toChatY(double y);

	@Invoker("getTagIconLeft")
	int chaoschatplus$tagIconLeft(GuiMessage.Line line);

	@Invoker("getTimeFactor")
	static double chaoschatplus$timeFactor(int age) {
		throw new AssertionError();
	}
}
