package com.chaos.chatplus.platform;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerNetwork {
    public interface Transport {
        void send(ServerPlayer player, CustomPacketPayload payload);
        boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);
    }
    public record Context(ServerPlayer player) { public MinecraftServer server() { return player.level().getServer(); } }
    private static Transport transport;
    private static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Context>> HANDLERS = new HashMap<>();
    private ServerNetwork() {}
    public static void install(Transport value) { transport = value; }
    public static void send(ServerPlayer player, CustomPacketPayload payload) { transport.send(player, payload); }
    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) { return transport != null && transport.canSend(player, type); }
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type, BiConsumer<T, Context> handler) {
        HANDLERS.put(type, (payload, context) -> { @SuppressWarnings("unchecked") T typed = (T) payload; handler.accept(typed, context); });
    }
    public static void receive(CustomPacketPayload payload, ServerPlayer player) {
        var handler = HANDLERS.get(payload.type());
        if (handler != null) handler.accept(payload, new Context(player));
    }
}
