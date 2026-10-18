package com.chaos.chatplus.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {
    @Test void everyMessagePreservesItsFieldsOnTheLegacyWireFormat() throws Exception {
        List<CustomPacketPayload> samples=List.of(
            new ChatPayloads.ServerInfo(new byte[]{1,2}),new ChatPayloads.EmojiImage("probe",new byte[]{3,4}),
            new ChatPayloads.SelfPrefix(Optional.of(Component.literal("VIP")),Optional.empty()),
            new ChatPayloads.ShotData("shot1","Luis",new byte[]{5,6}),new ChatPayloads.UploadEmoji("new_emoji",new byte[]{7,8}),
            new ChatPayloads.ShareItem(13,"Pepe"),new ChatPayloads.FontPart(2,4,new byte[]{9,10}),
            new ChatPayloads.ShotPart(1,3,new byte[]{11,12},"Pepe"),new ChatPayloads.RequestShot("shot2"),
            new ChatPayloads.GifResults(new byte[]{13,14}),new ChatPayloads.GifData("local:probe",new byte[]{15,16}),
            new ChatPayloads.GifSearch("cat:anime"),new ChatPayloads.GifRequest("local:other"),
            new ChatPayloads.SendGif("local:gif","metadata","Pepe"),new ChatPayloads.ReportShot("shot3"));
        assertEquals(PayloadCatalog.ALL.size(),samples.size());
        assertEquals(samples.size(),samples.stream().map(CustomPacketPayload::type).distinct().count());
        for(CustomPacketPayload sample:samples) roundTrip(sample);
    }
    private static <T extends CustomPacketPayload> void decode(PayloadCatalog.Entry<T> entry,CustomPacketPayload sample,FriendlyByteBuf buffer) throws Exception {
        T decoded=entry.codec().decode(buffer);
        assertEquals(sample.type(),decoded.type());
        for(var component:sample.getClass().getRecordComponents())
            assertTrue(Objects.deepEquals(component.getAccessor().invoke(sample),component.getAccessor().invoke(decoded)),sample.type()+" field "+component.getName());
        assertEquals(0,buffer.readableBytes());
    }
    private static void roundTrip(CustomPacketPayload sample) throws Exception {
        FriendlyByteBuf buffer=PayloadCatalog.encode(sample);
        try { decode(PayloadCatalog.ALL.stream().filter(e->e.type().equals(sample.type())).findFirst().orElseThrow(),sample,buffer); }
        finally { buffer.release(); }
    }
    @Test void oversizedArrayLengthIsRejectedBeforeAllocatingItsContents() {
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try { buffer.writeVarInt(4_000_001);assertThrows(RuntimeException.class,()->ChatPayloads.ServerInfo.CODEC.decode(buffer)); }
        finally { buffer.release(); }
    }
    @Test void oversizedOrTruncatedScreenshotIdsAreRejected() {
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf("x".repeat(33));assertThrows(RuntimeException.class,()->ChatPayloads.RequestShot.CODEC.decode(buffer));
            buffer.clear();buffer.writeVarInt(10);buffer.writeByte('a');assertThrows(RuntimeException.class,()->ChatPayloads.RequestShot.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
}
