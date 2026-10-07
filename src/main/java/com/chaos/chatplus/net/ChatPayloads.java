package com.chaos.chatplus.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.chaos.chatplus.ChaosChatPlus;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Paquetes del mod. Lo grande viaja como JSON comprimido con gzip; las capturas suben en trozos porque del cliente
 * al servidor solo caben 32 KB por paquete.
 */
public final class ChatPayloads {

	/** Límite de un paquete del cliente al servidor (menos un margen para la cabecera). */
	public static final int MAX_C2S = 32_000;
	/** Tamaño de cada trozo de una captura. */
	public static final int SHOT_PART = 30_000;
	private static final int MAX_S2C = 4_000_000;

	private ChatPayloads() {}

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
			return new String(in.readNBytes(4 * 1024 * 1024), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Servidor -> cliente: qué está permitido y la lista de emojis (al entrar y cada vez que cambia). */
	public record ServerInfo(byte[] data) implements CustomPacketPayload {

		public static final Type<ServerInfo> TYPE = new Type<>(ChaosChatPlus.id("server_info"));
		public static final StreamCodec<ByteBuf, ServerInfo> CODEC =
				ByteBufCodecs.byteArray(MAX_S2C).map(ServerInfo::new, ServerInfo::data);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: imagen de un emoji propio del servidor. Nombre "*" y sin datos = borrar todos. */
	public record EmojiImage(String name, byte[] png) implements CustomPacketPayload {

		public static final Type<EmojiImage> TYPE = new Type<>(ChaosChatPlus.id("emoji_image"));
		public static final StreamCodec<ByteBuf, EmojiImage> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), EmojiImage::name,
				ByteBufCodecs.byteArray(MAX_S2C), EmojiImage::png,
				EmojiImage::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/**
	 * Servidor -> cliente: cómo saldrá tu nombre en el chat ("ETIQUETA Nombre »"), para la barra de escribir.
	 * {@code fallback} es lo mismo en texto plano para quien no tenga las fuentes de Chaos Ranks.
	 */
	public record SelfPrefix(Optional<Component> prefix, Optional<Component> fallback) implements CustomPacketPayload {

		public static final Type<SelfPrefix> TYPE = new Type<>(ChaosChatPlus.id("self_prefix"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelfPrefix> CODEC = StreamCodec.composite(
				ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, SelfPrefix::prefix,
				ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC, SelfPrefix::fallback,
				SelfPrefix::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: una captura (JPEG). Sin datos = se borró o se ocultó. */
	public record ShotData(String id, String owner, byte[] jpeg) implements CustomPacketPayload {

		public static final Type<ShotData> TYPE = new Type<>(ChaosChatPlus.id("shot_data"));
		public static final StreamCodec<ByteBuf, ShotData> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(32), ShotData::id,
				ByteBufCodecs.stringUtf8(32), ShotData::owner,
				ByteBufCodecs.byteArray(MAX_S2C), ShotData::jpeg,
				ShotData::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: subir un emoji propio (PNG). */
	public record UploadEmoji(String name, byte[] png) implements CustomPacketPayload {

		public static final Type<UploadEmoji> TYPE = new Type<>(ChaosChatPlus.id("upload_emoji"));
		public static final StreamCodec<ByteBuf, UploadEmoji> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(64), UploadEmoji::name,
				ByteBufCodecs.byteArray(MAX_C2S), UploadEmoji::png,
				UploadEmoji::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: mostrar en el chat el ítem de ese hueco del inventario (target = jugador, "" = todos). */
	public record ShareItem(int slot, String target) implements CustomPacketPayload {

		public static final Type<ShareItem> TYPE = new Type<>(ChaosChatPlus.id("share_item"));
		public static final StreamCodec<ByteBuf, ShareItem> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, ShareItem::slot,
				ByteBufCodecs.stringUtf8(16), ShareItem::target,
				ShareItem::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: un trozo de la captura que se está enviando (part empieza en 0). */
	/** Trozo de las fuentes del chat (emojis añadidos por el servidor y etiquetas de rangos). */
	public record FontPart(int part, int parts, byte[] data) implements CustomPacketPayload {

		public static final Type<FontPart> TYPE = new Type<>(ChaosChatPlus.id("font_part"));
		public static final StreamCodec<ByteBuf, FontPart> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, FontPart::part,
				ByteBufCodecs.VAR_INT, FontPart::parts,
				ByteBufCodecs.byteArray(MAX_S2C), FontPart::data,
				FontPart::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Durante la conexión: el servidor comprueba con él que el cliente tiene el mod (si no, lo echa). */
	public record Hello() implements CustomPacketPayload {

		public static final Hello INSTANCE = new Hello();
		public static final Type<Hello> TYPE = new Type<>(ChaosChatPlus.id("hello"));
		public static final StreamCodec<ByteBuf, Hello> CODEC = StreamCodec.unit(INSTANCE);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record ShotPart(int part, int parts, byte[] data, String target) implements CustomPacketPayload {

		public static final Type<ShotPart> TYPE = new Type<>(ChaosChatPlus.id("shot_part"));
		public static final StreamCodec<ByteBuf, ShotPart> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, ShotPart::part,
				ByteBufCodecs.VAR_INT, ShotPart::parts,
				ByteBufCodecs.byteArray(MAX_C2S), ShotPart::data,
				ByteBufCodecs.stringUtf8(16), ShotPart::target,
				ShotPart::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: pedir una captura para verla. */
	public record RequestShot(String id) implements CustomPacketPayload {

		public static final Type<RequestShot> TYPE = new Type<>(ChaosChatPlus.id("request_shot"));
		public static final StreamCodec<ByteBuf, RequestShot> CODEC =
				ByteBufCodecs.stringUtf8(32).map(RequestShot::new, RequestShot::id);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: resultados de una búsqueda de GIFs (JSON con gzip). */
	public record GifResults(byte[] data) implements CustomPacketPayload {

		public static final Type<GifResults> TYPE = new Type<>(ChaosChatPlus.id("gif_results"));
		public static final StreamCodec<ByteBuf, GifResults> CODEC =
				ByteBufCodecs.byteArray(MAX_S2C).map(GifResults::new, GifResults::data);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Servidor -> cliente: los bytes de un GIF propio del servidor. Sin datos = no existe. */
	public record GifData(String id, byte[] data) implements CustomPacketPayload {

		public static final Type<GifData> TYPE = new Type<>(ChaosChatPlus.id("gif_data"));
		public static final StreamCodec<ByteBuf, GifData> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(256), GifData::id,
				ByteBufCodecs.byteArray(MAX_S2C), GifData::data,
				GifData::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: buscar GIFs (vacío = los destacados). */
	public record GifSearch(String query) implements CustomPacketPayload {

		public static final Type<GifSearch> TYPE = new Type<>(ChaosChatPlus.id("gif_search"));
		public static final StreamCodec<ByteBuf, GifSearch> CODEC =
				ByteBufCodecs.stringUtf8(64).map(GifSearch::new, GifSearch::query);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: pedir los bytes de un GIF propio. */
	public record GifRequest(String id) implements CustomPacketPayload {

		public static final Type<GifRequest> TYPE = new Type<>(ChaosChatPlus.id("gif_request"));
		public static final StreamCodec<ByteBuf, GifRequest> CODEC =
				ByteBufCodecs.stringUtf8(256).map(GifRequest::new, GifRequest::id);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: mandar un GIF al chat (target = jugador para mandarlo en privado, "" = todos). */
	public record SendGif(String id, String target) implements CustomPacketPayload {

		public static final Type<SendGif> TYPE = new Type<>(ChaosChatPlus.id("send_gif"));
		public static final StreamCodec<ByteBuf, SendGif> CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(256), SendGif::id,
				ByteBufCodecs.stringUtf8(16), SendGif::target,
				SendGif::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Cliente -> servidor: reportar una captura. */
	public record ReportShot(String id) implements CustomPacketPayload {

		public static final Type<ReportShot> TYPE = new Type<>(ChaosChatPlus.id("report_shot"));
		public static final StreamCodec<ByteBuf, ReportShot> CODEC =
				ByteBufCodecs.stringUtf8(32).map(ReportShot::new, ReportShot::id);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
