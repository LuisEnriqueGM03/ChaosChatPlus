package com.chaos.chatplus.net;
import net.minecraft.resources.ResourceLocation;
/** Typed mod messages transported through the 1.20.1 loader networking APIs. */
public interface CustomPacketPayload {
 record Type<T extends CustomPacketPayload>(ResourceLocation id) {}
 Type<? extends CustomPacketPayload> type();
}
