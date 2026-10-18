package com.chaos.chatplus.neoforge;
import com.chaos.chatplus.client.*;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.Minecraft;
import com.chaos.chatplus.net.CustomPacketPayload;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid = "chaoschatplus", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientNetwork.install(new ClientNetwork.Transport() {
                public void send(CustomPacketPayload payload) { NeoNetwork.CHANNEL.sendToServer(payload); }
            public boolean canSend(CustomPacketPayload.Type<?> type) {
                return Minecraft.getInstance().getConnection() != null && NeoNetwork.CHANNEL.isRemotePresent(Minecraft.getInstance().getConnection().getConnection());
            }
            });
            ChaosChatPlusClient.initialize();
            MinecraftForge.EVENT_BUS.addListener(NeoClient::start);
            MinecraftForge.EVENT_BUS.addListener(NeoClient::end);
            MinecraftForge.EVENT_BUS.addListener(NeoClient::logout);
            MinecraftForge.EVENT_BUS.addListener(NeoClient::renderGui);
        });
    }
    private static void start(TickEvent.ClientTickEvent event) { if(event.phase==TickEvent.Phase.START) ChaosChatPlusClient.startTick(Minecraft.getInstance()); }
    private static void end(TickEvent.ClientTickEvent event) { if(event.phase==TickEvent.Phase.END) ChaosChatPlusClient.endTick(Minecraft.getInstance()); }
    private static void renderGui(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
        if (ShotCapture.hideHud()) {
            ShotCapture.renderViewfinder(event.getGuiGraphics());
            event.setCanceled(true);
        }
    }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosChatPlusClient.disconnect(); }
}
