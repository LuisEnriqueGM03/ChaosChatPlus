package com.chaos.chatplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonPacketListenerImplAccessor {

	@Accessor("connection")
	Connection chaoschatplus$connection();
}
