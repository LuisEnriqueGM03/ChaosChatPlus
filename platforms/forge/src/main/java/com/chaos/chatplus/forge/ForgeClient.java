package com.chaos.chatplus.forge;
import com.chaos.chatplus.client.*;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

public final class ForgeClient {
    public static void register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(ForgeClient::setup);
    }
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientNetwork.install(new ClientNetwork.Transport() {
                public void send(CustomPacketPayload payload) { ForgeNetwork.CHANNEL.send(payload, net.minecraftforge.network.PacketDistributor.SERVER.noArg()); }
            public boolean canSend(CustomPacketPayload.Type<?> type) {
                return Minecraft.getInstance().getConnection() != null && ForgeNetwork.CHANNEL.isRemotePresent(Minecraft.getInstance().getConnection().getConnection());
            }
            });
            ChaosChatPlusClient.initialize();
            TickEvent.ClientTickEvent.Pre.BUS.addListener(ForgeClient::start);
            TickEvent.ClientTickEvent.Post.BUS.addListener(ForgeClient::end);
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(ForgeClient::logout);
        });
    }
    private static void start(TickEvent.ClientTickEvent.Pre event) { ChaosChatPlusClient.startTick(Minecraft.getInstance()); }
    private static void end(TickEvent.ClientTickEvent.Post event) { ChaosChatPlusClient.endTick(Minecraft.getInstance()); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosChatPlusClient.disconnect(); }
}
