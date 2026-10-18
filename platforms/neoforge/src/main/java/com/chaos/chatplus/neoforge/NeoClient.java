package com.chaos.chatplus.neoforge;
import com.chaos.chatplus.client.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid = "chaoschatplus", value = Dist.CLIENT)
public final class NeoClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientNetwork.install(new ClientNetwork.Transport() {
                public void send(CustomPacketPayload payload) { net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload); }
            public boolean canSend(CustomPacketPayload.Type<?> type) {
                return Minecraft.getInstance().getConnection() != null && Minecraft.getInstance().getConnection().hasChannel(type.id());
            }
            });
            ChaosChatPlusClient.initialize();
            NeoForge.EVENT_BUS.addListener(NeoClient::start);
            NeoForge.EVENT_BUS.addListener(NeoClient::end);
            NeoForge.EVENT_BUS.addListener(NeoClient::logout);
        });
    }
    private static void start(ClientTickEvent.Pre event) { ChaosChatPlusClient.startTick(Minecraft.getInstance()); }
    private static void end(ClientTickEvent.Post event) { ChaosChatPlusClient.endTick(Minecraft.getInstance()); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosChatPlusClient.disconnect(); }
}
