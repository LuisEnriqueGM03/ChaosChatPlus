package com.chaos.chatplus.net;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.*;
import java.util.function.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.chaos.chatplus.ChaosChatPlus;
public final class ChatPayloads {
 public static final int MAX_C2S=32_000, SHOT_PART=30_000;
 private ChatPayloads() {}
 private static <T> PacketCodec<T> codec(BiConsumer<FriendlyByteBuf,T> writer,Function<FriendlyByteBuf,T> reader) {
  return new PacketCodec<>() { public void encode(FriendlyByteBuf b,T p){writer.accept(b,p);} public T decode(FriendlyByteBuf b){return reader.apply(b);} };
 }
 private static Optional<Component> component(FriendlyByteBuf b) {return b.readBoolean()?Optional.of(b.readComponent()):Optional.empty();}
 private static void component(FriendlyByteBuf b,Optional<Component> c) {b.writeBoolean(c.isPresent());c.ifPresent(b::writeComponent);}
	public static byte[] gzip(String json) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
			out.write(json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

	public static String gunzip(byte[] data) {
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
			// Tope de 4 MB descomprimidos: que nadie mande una bomba de gzip.
			byte[] decoded = in.readNBytes(4 * 1024 * 1024 + 1);
			if (decoded.length > 4 * 1024 * 1024) throw new IOException("Decompressed payload too large");
			return new String(decoded, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

 public record ServerInfo(byte[] data) implements CustomPacketPayload {
  public static final Type<ServerInfo> TYPE=new Type<>(ChaosChatPlus.id("server_info"));
  public static final PacketCodec<ServerInfo> CODEC=codec((b,p)->{b.writeByteArray(p.data());},b->new ServerInfo(b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record EmojiImage(String name, byte[] png) implements CustomPacketPayload {
  public static final Type<EmojiImage> TYPE=new Type<>(ChaosChatPlus.id("emoji_image"));
  public static final PacketCodec<EmojiImage> CODEC=codec((b,p)->{b.writeUtf(p.name(),64);b.writeByteArray(p.png());},b->new EmojiImage(b.readUtf(64), b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record SelfPrefix(Optional<Component> prefix, Optional<Component> fallback) implements CustomPacketPayload {
  public static final Type<SelfPrefix> TYPE=new Type<>(ChaosChatPlus.id("self_prefix"));
  public static final PacketCodec<SelfPrefix> CODEC=codec((b,p)->{component(b,p.prefix());component(b,p.fallback());},b->new SelfPrefix(component(b), component(b)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record ShotData(String id, String owner, byte[] jpeg) implements CustomPacketPayload {
  public static final Type<ShotData> TYPE=new Type<>(ChaosChatPlus.id("shot_data"));
  public static final PacketCodec<ShotData> CODEC=codec((b,p)->{b.writeUtf(p.id(),32);b.writeUtf(p.owner(),32);b.writeByteArray(p.jpeg());},b->new ShotData(b.readUtf(32), b.readUtf(32), b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record UploadEmoji(String name, byte[] png) implements CustomPacketPayload {
  public static final Type<UploadEmoji> TYPE=new Type<>(ChaosChatPlus.id("upload_emoji"));
  public static final PacketCodec<UploadEmoji> CODEC=codec((b,p)->{b.writeUtf(p.name(),64);b.writeByteArray(p.png());},b->new UploadEmoji(b.readUtf(64), b.readByteArray(32000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record ShareItem(int slot, String target) implements CustomPacketPayload {
  public static final Type<ShareItem> TYPE=new Type<>(ChaosChatPlus.id("share_item"));
  public static final PacketCodec<ShareItem> CODEC=codec((b,p)->{b.writeVarInt(p.slot());b.writeUtf(p.target(),16);},b->new ShareItem(b.readVarInt(), b.readUtf(16)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record FontPart(int part, int parts, byte[] data) implements CustomPacketPayload {
  public static final Type<FontPart> TYPE=new Type<>(ChaosChatPlus.id("font_part"));
  public static final PacketCodec<FontPart> CODEC=codec((b,p)->{b.writeVarInt(p.part());b.writeVarInt(p.parts());b.writeByteArray(p.data());},b->new FontPart(b.readVarInt(), b.readVarInt(), b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Hello() implements CustomPacketPayload {
  public static final Type<Hello> TYPE=new Type<>(ChaosChatPlus.id("hello"));
  public static final PacketCodec<Hello> CODEC=codec((b,p)->{},b->new Hello());
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
  public static final Hello INSTANCE=new Hello();
 }
 public record ShotPart(int part, int parts, byte[] data, String target) implements CustomPacketPayload {
  public static final Type<ShotPart> TYPE=new Type<>(ChaosChatPlus.id("shot_part"));
  public static final PacketCodec<ShotPart> CODEC=codec((b,p)->{b.writeVarInt(p.part());b.writeVarInt(p.parts());b.writeByteArray(p.data());b.writeUtf(p.target(),16);},b->new ShotPart(b.readVarInt(), b.readVarInt(), b.readByteArray(32000), b.readUtf(16)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record RequestShot(String id) implements CustomPacketPayload {
  public static final Type<RequestShot> TYPE=new Type<>(ChaosChatPlus.id("request_shot"));
  public static final PacketCodec<RequestShot> CODEC=codec((b,p)->{b.writeUtf(p.id(),32);},b->new RequestShot(b.readUtf(32)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record GifResults(byte[] data) implements CustomPacketPayload {
  public static final Type<GifResults> TYPE=new Type<>(ChaosChatPlus.id("gif_results"));
  public static final PacketCodec<GifResults> CODEC=codec((b,p)->{b.writeByteArray(p.data());},b->new GifResults(b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record GifData(String id, byte[] data) implements CustomPacketPayload {
  public static final Type<GifData> TYPE=new Type<>(ChaosChatPlus.id("gif_data"));
  public static final PacketCodec<GifData> CODEC=codec((b,p)->{b.writeUtf(p.id(),256);b.writeByteArray(p.data());},b->new GifData(b.readUtf(256), b.readByteArray(4000000)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record GifSearch(String query) implements CustomPacketPayload {
  public static final Type<GifSearch> TYPE=new Type<>(ChaosChatPlus.id("gif_search"));
  public static final PacketCodec<GifSearch> CODEC=codec((b,p)->{b.writeUtf(p.query(),64);},b->new GifSearch(b.readUtf(64)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record GifRequest(String id) implements CustomPacketPayload {
  public static final Type<GifRequest> TYPE=new Type<>(ChaosChatPlus.id("gif_request"));
  public static final PacketCodec<GifRequest> CODEC=codec((b,p)->{b.writeUtf(p.id(),256);},b->new GifRequest(b.readUtf(256)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record SendGif(String id, String info, String target) implements CustomPacketPayload {
  public static final Type<SendGif> TYPE=new Type<>(ChaosChatPlus.id("send_gif"));
  public static final PacketCodec<SendGif> CODEC=codec((b,p)->{b.writeUtf(p.id(),256);b.writeUtf(p.info(),4096);b.writeUtf(p.target(),16);},b->new SendGif(b.readUtf(256), b.readUtf(4096), b.readUtf(16)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record ReportShot(String id) implements CustomPacketPayload {
  public static final Type<ReportShot> TYPE=new Type<>(ChaosChatPlus.id("report_shot"));
  public static final PacketCodec<ReportShot> CODEC=codec((b,p)->{b.writeUtf(p.id(),32);},b->new ReportShot(b.readUtf(32)));
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
}
