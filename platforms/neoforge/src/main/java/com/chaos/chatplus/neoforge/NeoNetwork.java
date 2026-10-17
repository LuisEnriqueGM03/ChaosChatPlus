package com.chaos.chatplus.neoforge;

import com.chaos.chatplus.net.PayloadCatalog;
import com.chaos.chatplus.platform.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoNetwork {
    public static void install() {
        ServerNetwork.install(new ServerNetwork.Transport() {
            public void send(ServerPlayer player, CustomPacketPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
            public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) { return player.connection.hasChannel(type.id()); }
        });
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        PayloadCatalog.ALL.forEach(entry -> register(registrar, entry));
    }
    private static <T extends CustomPacketPayload> void register(PayloadRegistrar registrar, PayloadCatalog.Entry<T> entry) {
        if (entry.toServer()) registrar.playToServer(entry.type(), entry.codec(), (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ServerNetwork.receive(payload, player);
        });
        else registrar.playToClient(entry.type(), entry.codec(), (payload, context) -> ClientDispatch.receive(payload));
    }
}
