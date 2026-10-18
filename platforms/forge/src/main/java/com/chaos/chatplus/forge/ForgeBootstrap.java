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
    public ForgeBootstrap(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) ForgeClient.register(context);
        ForgeNetwork.register();
        Platform.install(new ForgeEnvironment());
        ForgeNetwork.install();
        ChaosChatPlus.initialize();
        RegisterCommandsEvent.BUS.addListener(this::commands);
        ServerStartedEvent.BUS.addListener(this::started);
        ServerStoppedEvent.BUS.addListener(this::stopped);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::tick);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::join);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::leave);
        ServerChatEvent.BUS.addListener(this::chat);
    }
    private void commands(RegisterCommandsEvent event) { ChatPlusCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosChatPlus.serverStarted(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChatServer.stop(); }
    private void tick(TickEvent.ServerTickEvent.Post event) { ChatServer.tick(event.server()); }
    private void join(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.join(player); }
    private void leave(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) ChatServer.leave(player); }
    private void chat(ServerChatEvent event) { event.setMessage(ChatDecorator.decorate(event.getPlayer(), event.getMessage())); }
}
