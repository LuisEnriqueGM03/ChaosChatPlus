package com.chaos.chatplus.fabric;

import com.chaos.chatplus.client.*;
import com.chaos.chatplus.net.*;
import com.chaos.chatplus.platform.ClientDispatch;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class FabricClientBootstrap implements ClientModInitializer {
    public void onInitializeClient() {
        ClientNetwork.install(new ClientNetwork.Transport() {
            public void send(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
            public boolean canSend(CustomPacketPayload.Type<?> type) { return ClientPlayNetworking.canSend(type); }
        });
        ChaosChatPlusClient.initialize();
        if (ChaosChatPlusClient.ACTIVE) {
            ClientConfigurationNetworking.registerGlobalReceiver(ChatPayloads.Hello.TYPE, (payload, context) -> {});
            PayloadCatalog.ALL.forEach(FabricClientBootstrap::register);
        }
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChaosChatPlusClient::disconnect));
        ClientTickEvents.START_CLIENT_TICK.register(ChaosChatPlusClient::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(ChaosChatPlusClient::endTick);
    }
    private static <T extends CustomPacketPayload> void register(PayloadCatalog.Entry<T> entry) {
        if (!entry.toServer()) ClientPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> ClientDispatch.receive(payload));
    }
}
