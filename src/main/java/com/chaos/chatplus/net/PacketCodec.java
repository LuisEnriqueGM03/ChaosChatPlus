package com.chaos.chatplus.net;
import net.minecraft.network.FriendlyByteBuf;
public interface PacketCodec<T> {
 void encode(FriendlyByteBuf buffer, T value);
 T decode(FriendlyByteBuf buffer);
}
