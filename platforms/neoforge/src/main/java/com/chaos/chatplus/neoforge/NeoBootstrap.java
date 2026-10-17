package com.chaos.chatplus.neoforge;
import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.command.ChatPlusCommand;
import com.chaos.chatplus.server.*;
import com.chaos.chatplus.platform.Platform;
import net.neoforged.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ChaosChatPlus.MOD_ID)
public final class NeoBootstrap {
    public NeoBootstrap(net.neoforged.bus.api.IEventBus modBus) {
        modBus.addListener(NeoNetwork::register);
        Platform.install(new NeoEnvironment());
        NeoNetwork.install();
        ChaosChatPlus.initialize();
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::stopped);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::join);
        NeoForge.EVENT_BUS.addListener(this::leave);
        NeoForge.EVENT_BUS.addListener(this::chat);
    }
    private void commands(RegisterCommandsEvent event) { ChatPlusCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosChatPlus.serverStarted(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChatServer.stop(); }
    private void tick(ServerTickEvent.Post event) { ChatServer.tick(event.getServer()); }
    private void join(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.join(player); }
    private void leave(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.leave(player); }
    private void chat(ServerChatEvent event) { event.setMessage(ChatDecorator.decorate(event.getPlayer(), event.getMessage())); }
}
