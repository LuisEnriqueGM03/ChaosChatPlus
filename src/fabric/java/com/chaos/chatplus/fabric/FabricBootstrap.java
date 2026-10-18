package com.chaos.chatplus.fabric;

import com.chaos.chatplus.*;
import com.chaos.chatplus.command.ChatPlusCommand;
import com.chaos.chatplus.server.*;
import com.chaos.chatplus.net.*;
import com.chaos.chatplus.platform.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.Optional;

public final class FabricBootstrap implements ModInitializer {
    public void onInitialize() {
        Platform.install(new Platform.Environment() {
            public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
            public boolean loaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
            public Optional<Path> resource(String id, String path) { return FabricLoader.getInstance().getModContainer(id).flatMap(mod -> mod.findPath(path)); }
            public String version() { return FabricLoader.getInstance().getModContainer(ChaosChatPlus.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString(); }
        });
        ServerNetwork.install(new ServerNetwork.Transport() {
            public void send(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
            public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) { return ServerPlayNetworking.canSend(player, type); }
        });
        PayloadCatalog.ALL.forEach(FabricBootstrap::register);
        PayloadTypeRegistry.clientboundConfiguration().register(ChatPayloads.Hello.TYPE, ChatPayloads.Hello.CODEC);
        ChaosChatPlus.initialize();
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, (sender, message) -> sender == null ? message : ChatDecorator.decorate(sender, message));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> ChatPlusCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(ChaosChatPlus::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChatServer.stop());
        ServerTickEvents.END_SERVER_TICK.register(ChatServer::tick);
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, ChatPayloads.Hello.TYPE)) handler.disconnect(Lang.tr("mod.required", Platform.get().version()));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ChatServer.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ChatServer.leave(handler.player));
    }
    private static <T extends CustomPacketPayload> void register(PayloadCatalog.Entry<T> entry) {
        if (entry.toServer()) {
            PayloadTypeRegistry.serverboundPlay().register(entry.type(), entry.codec());
            ServerPlayNetworking.registerGlobalReceiver(entry.type(), (payload, context) -> ServerNetwork.receive(payload, context.player()));
        } else PayloadTypeRegistry.clientboundPlay().register(entry.type(), entry.codec());
    }
}
