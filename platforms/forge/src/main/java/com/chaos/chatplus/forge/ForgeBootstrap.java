package com.chaos.chatplus.forge;
import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.command.ChatPlusCommand;
import com.chaos.chatplus.server.*;
import com.chaos.chatplus.platform.Platform;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;

@Mod(ChaosChatPlus.MOD_ID)
public final class ForgeBootstrap {
    public ForgeBootstrap() {
        ForgeNetwork.register();
        Platform.install(new ForgeEnvironment());
        ForgeNetwork.install();
        ChaosChatPlus.initialize();
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::join);
        MinecraftForge.EVENT_BUS.addListener(this::leave);
        MinecraftForge.EVENT_BUS.addListener(this::chat);
    }
    private void commands(RegisterCommandsEvent event) { ChatPlusCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosChatPlus.serverStarted(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChatServer.stop(); }
    private void tick(TickEvent.ServerTickEvent event) { if(event.phase==TickEvent.Phase.END) ChatServer.tick(event.getServer()); }
    private void join(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.join(player); }
    private void leave(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.leave(player); }
    private void chat(ServerChatEvent event) { event.setMessage(ChatDecorator.decorate(event.getPlayer(), event.getMessage())); }
}
