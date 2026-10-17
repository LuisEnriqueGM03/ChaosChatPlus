package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

	@Accessor("fontManager")
	FontManager chaoschatplus$fontManager();
}
