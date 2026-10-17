package com.chaos.chatplus.net;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class PayloadCatalog {
    public record Entry<T extends CustomPacketPayload>(Class<T> javaType, CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, boolean toServer) {}
    private PayloadCatalog() {}
    public static final List<Entry<?>> ALL = List.of(
        new Entry<>(ChatPayloads.ServerInfo.class, ChatPayloads.ServerInfo.TYPE, ChatPayloads.ServerInfo.CODEC, false),
        new Entry<>(ChatPayloads.EmojiImage.class, ChatPayloads.EmojiImage.TYPE, ChatPayloads.EmojiImage.CODEC, false),
        new Entry<>(ChatPayloads.SelfPrefix.class, ChatPayloads.SelfPrefix.TYPE, ChatPayloads.SelfPrefix.CODEC, false),
        new Entry<>(ChatPayloads.ShotData.class, ChatPayloads.ShotData.TYPE, ChatPayloads.ShotData.CODEC, false),
        new Entry<>(ChatPayloads.UploadEmoji.class, ChatPayloads.UploadEmoji.TYPE, ChatPayloads.UploadEmoji.CODEC, true),
        new Entry<>(ChatPayloads.ShareItem.class, ChatPayloads.ShareItem.TYPE, ChatPayloads.ShareItem.CODEC, true),
        new Entry<>(ChatPayloads.FontPart.class, ChatPayloads.FontPart.TYPE, ChatPayloads.FontPart.CODEC, false),
        new Entry<>(ChatPayloads.ShotPart.class, ChatPayloads.ShotPart.TYPE, ChatPayloads.ShotPart.CODEC, true),
        new Entry<>(ChatPayloads.RequestShot.class, ChatPayloads.RequestShot.TYPE, ChatPayloads.RequestShot.CODEC, true),
        new Entry<>(ChatPayloads.GifResults.class, ChatPayloads.GifResults.TYPE, ChatPayloads.GifResults.CODEC, false),
        new Entry<>(ChatPayloads.GifData.class, ChatPayloads.GifData.TYPE, ChatPayloads.GifData.CODEC, false),
        new Entry<>(ChatPayloads.GifSearch.class, ChatPayloads.GifSearch.TYPE, ChatPayloads.GifSearch.CODEC, true),
        new Entry<>(ChatPayloads.GifRequest.class, ChatPayloads.GifRequest.TYPE, ChatPayloads.GifRequest.CODEC, true),
        new Entry<>(ChatPayloads.SendGif.class, ChatPayloads.SendGif.TYPE, ChatPayloads.SendGif.CODEC, true),
        new Entry<>(ChatPayloads.ReportShot.class, ChatPayloads.ReportShot.TYPE, ChatPayloads.ReportShot.CODEC, true));
}
