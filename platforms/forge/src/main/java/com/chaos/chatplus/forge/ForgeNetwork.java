package com.chaos.chatplus.forge;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.PayloadCatalog;
import com.chaos.chatplus.platform.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.*;

public final class ForgeNetwork {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(ChaosChatPlus.id("main")).networkProtocolVersion(1).simpleChannel();
    public static void install() {
        ServerNetwork.install(new ServerNetwork.Transport() {
            public void send(ServerPlayer player, CustomPacketPayload payload) { CHANNEL.send(payload, PacketDistributor.PLAYER.with(player)); }
            public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) { return CHANNEL.isRemotePresent(player.connection.getConnection()); }
        });
    }
    public static void register() { PayloadCatalog.ALL.forEach(ForgeNetwork::register); CHANNEL.build(); }
    private static <T extends CustomPacketPayload> void register(PayloadCatalog.Entry<T> entry) {
        CHANNEL.messageBuilder(entry.javaType(), entry.toServer() ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> entry.codec().encode(buffer, payload))
                .decoder(buffer -> entry.codec().decode(buffer))
                .consumerMainThread((payload, context) -> {
                    if (entry.toServer()) {
                        ServerPlayer player = context.getSender();
                        if (player != null) ServerNetwork.receive(payload, player);
                    } else ClientDispatch.receive(payload);
                }).add();
    }
}
