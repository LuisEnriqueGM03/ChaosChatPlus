package com.chaos.chatplus.mixin.client;

import java.util.function.BiFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

	@Accessor("maxLength")
	int chaoschatplus$maxLength();
}
