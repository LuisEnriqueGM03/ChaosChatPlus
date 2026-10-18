package com.chaos.chatplus.platform;
import java.util.function.Consumer;
import com.chaos.chatplus.net.CustomPacketPayload;
public final class ClientDispatch {
    private static Consumer<CustomPacketPayload> receiver = payload -> {};
    private ClientDispatch() {}
    public static void install(Consumer<CustomPacketPayload> value) { receiver = value; }
    public static void receive(CustomPacketPayload payload) { receiver.accept(payload); }
}
