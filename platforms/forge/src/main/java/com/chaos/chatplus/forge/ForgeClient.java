package com.chaos.chatplus.forge;
import com.chaos.chatplus.client.*;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid = "chaoschatplus", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientNetwork.install(new ClientNetwork.Transport() {
                public void send(CustomPacketPayload payload) { ForgeNetwork.CHANNEL.send(payload, net.minecraftforge.network.PacketDistributor.SERVER.noArg()); }
            public boolean canSend(CustomPacketPayload.Type<?> type) {
                return Minecraft.getInstance().getConnection() != null && ForgeNetwork.CHANNEL.isRemotePresent(Minecraft.getInstance().getConnection().getConnection());
            }
            });
            ChaosChatPlusClient.initialize();
            MinecraftForge.EVENT_BUS.addListener(ForgeClient::start);
            MinecraftForge.EVENT_BUS.addListener(ForgeClient::end);
            MinecraftForge.EVENT_BUS.addListener(ForgeClient::logout);
        });
    }
    private static void start(TickEvent.ClientTickEvent.Pre event) { ChaosChatPlusClient.startTick(Minecraft.getInstance()); }
    private static void end(TickEvent.ClientTickEvent.Post event) { ChaosChatPlusClient.endTick(Minecraft.getInstance()); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosChatPlusClient.disconnect(); }
}
