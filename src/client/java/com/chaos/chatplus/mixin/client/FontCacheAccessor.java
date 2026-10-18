package com.chaos.chatplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
public interface FontCacheAccessor {
    @Invoker("invalidate") void chaoschatplus$invalidate();
}
