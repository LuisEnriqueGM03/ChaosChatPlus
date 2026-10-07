package com.chaos.chatplus.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.chaos.chatplus.pack.PackHost;

import net.minecraft.network.Connection;

/** Guarda la dirección con la que entró el jugador, para darle la URL del pack de emojis con ese mismo host. */
@Mixin(Connection.class)
abstract class ConnectionMixin implements PackHost.ConnectionHost {

	@Unique
	@Nullable
	private String chaoschatplus$host;

	@Override
	@Nullable
	public String chaoschatplus$host() {
		return chaoschatplus$host;
	}

	@Override
	public void chaoschatplus$setHost(String host) {
		chaoschatplus$host = host;
	}
}
