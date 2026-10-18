package com.chaos.chatplus.neoforge;
import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.net.*;
import com.chaos.chatplus.platform.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class NeoNetwork {
 public static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder.named(ChaosChatPlus.id("main"))
  .networkProtocolVersion(()->"1201-1").clientAcceptedVersions("1201-1"::equals).serverAcceptedVersions("1201-1"::equals).simpleChannel();
 private static int index;
 public static void install() {ServerNetwork.install(new ServerNetwork.Transport() {
  public void send(ServerPlayer player,CustomPacketPayload payload){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),payload);}
  public boolean canSend(ServerPlayer player,CustomPacketPayload.Type<?> type){return CHANNEL.isRemotePresent(player.connection.connection);}
 });}
 public static void register(){PayloadCatalog.ALL.forEach(NeoNetwork::register);}
 private static <T extends CustomPacketPayload> void register(PayloadCatalog.Entry<T> entry) {
  CHANNEL.messageBuilder(entry.javaType(),index++,entry.toServer()?NetworkDirection.PLAY_TO_SERVER:NetworkDirection.PLAY_TO_CLIENT)
   .encoder((p,b)->entry.codec().encode(b,p)).decoder(entry.codec()::decode)
   .consumerMainThread((payload,context)->{if(entry.toServer()){var player=context.get().getSender();if(player!=null)ServerNetwork.receive(payload,player);}else ClientDispatch.receive(payload);}).add();
 }
}
