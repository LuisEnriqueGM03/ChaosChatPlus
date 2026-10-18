package com.chaos.chatplus.mixin.client;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("trimmedMessages") List<GuiMessage.Line> chaoschatplus$lines();
    @Accessor("chatScrollbarPos") int chaoschatplus$scroll();
    @Accessor("newMessageSinceScroll") boolean chaoschatplus$newSinceScroll();
    @Invoker("getLineHeight") int chaoschatplus$lineHeight();
    @Invoker("getScale") double chaoschatplus$scale();
    @Invoker("getWidth") int chaoschatplus$width();
}
