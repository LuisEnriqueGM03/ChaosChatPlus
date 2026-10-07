package com.chaos.chatplus.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

@Mixin(FontManager.class)
public interface FontManagerAccessor {

	@Accessor("fontSets")
	Map<ResourceLocation, FontSet> chaoschatplus$fontSets();

	@Accessor("textureManager")
	TextureManager chaoschatplus$textureManager();
}
