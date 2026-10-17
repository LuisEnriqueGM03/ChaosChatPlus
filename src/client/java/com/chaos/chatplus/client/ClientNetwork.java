package com.chaos.chatplus.client;

import java.util.*;
import java.util.function.BiConsumer;
import com.chaos.chatplus.platform.ClientDispatch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientNetwork {
    public interface Transport {
        void send(CustomPacketPayload payload);
        boolean canSend(CustomPacketPayload.Type<?> type);
    }
    public record Context() { public Minecraft client() { return Minecraft.getInstance(); } }
    private static Transport transport;
    private static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Context>> HANDLERS = new HashMap<>();
    private ClientNetwork() {}
    public static void install(Transport value) { transport = value; ClientDispatch.install(ClientNetwork::receive); }
    public static void send(CustomPacketPayload payload) { transport.send(payload); }
    public static boolean canSend(CustomPacketPayload.Type<?> type) { return transport != null && transport.canSend(type); }
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type, BiConsumer<T, Context> handler) {
        HANDLERS.put(type, (payload, context) -> { @SuppressWarnings("unchecked") T typed = (T) payload; handler.accept(typed, context); });
    }
    private static void receive(CustomPacketPayload payload) {
        var handler = HANDLERS.get(payload.type());
        if (handler != null) handler.accept(payload, new Context());
    }
}
