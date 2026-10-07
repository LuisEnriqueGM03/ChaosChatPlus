package com.chaos.chatplus.gif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Ajusta un GIF para el chat: como mucho 128 px de lado y 60 frames (si tiene más, se salta 1 de cada 2 y se suman
 * los retardos) y, si aún pesa más de 300 KB, a 96 px. Si ya cumple, se devuelve tal cual.
 */
public final class GifShrinker {

	public static final int MAX_SIDE = 128;
	public static final int SMALL_SIDE = 96;
	public static final int MAX_FRAMES = 60;
	public static final int MAX_BYTES = 300 * 1024;

	private GifShrinker() {}

	public static byte[] shrink(byte[] original) throws Exception {
		GifDecoder.Frames full = GifDecoder.decode(original, Integer.MAX_VALUE);
		int w = full.images().get(0).getWidth();
		int h = full.images().get(0).getHeight();
		if (w <= MAX_SIDE && h <= MAX_SIDE && full.images().size() <= MAX_FRAMES && original.length <= MAX_BYTES) {
			return original;
		}
		byte[] out = encode(reduce(GifDecoder.decode(original, MAX_SIDE)));
		if (out.length > MAX_BYTES) {
			out = encode(reduce(GifDecoder.decode(original, SMALL_SIDE)));
		}
		return out;
	}

	/** Quita frames hasta que queden como mucho 60, sumando el retardo de los que se saltan. */
	private static GifDecoder.Frames reduce(GifDecoder.Frames frames) {
		List<BufferedImage> images = frames.images();
		int[] delays = frames.delays();
		while (images.size() > MAX_FRAMES) {
			List<BufferedImage> kept = new ArrayList<>();
			int[] merged = new int[(images.size() + 1) / 2];
			for (int i = 0; i < images.size(); i += 2) {
				kept.add(images.get(i));
				merged[i / 2] = delays[i] + (i + 1 < delays.length ? delays[i + 1] : 0);
			}
			images = kept;
			delays = merged;
		}
		return new GifDecoder.Frames(images, delays);
	}

	/** Codifica los frames (ARGB, ya compuestos) como GIF animado en bucle. */
	static byte[] encode(GifDecoder.Frames frames) throws Exception {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(out);
			writer.prepareWriteSequence(null);
			for (int i = 0; i < frames.images().size(); i++) {
				BufferedImage img = frames.images().get(i);
				ImageWriteParam param = writer.getDefaultWriteParam();
				IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param);
				String format = meta.getNativeMetadataFormatName();
				IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
				IIOMetadataNode gce = child(root, "GraphicControlExtension");
				gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
				gce.setAttribute("userInputFlag", "FALSE");
				gce.setAttribute("transparentColorFlag", gce.getAttribute("transparentColorFlag"));
				gce.setAttribute("delayTime", String.valueOf(Math.max(2, frames.delays()[i] / 10)));
				if (i == 0) {
					IIOMetadataNode apps = child(root, "ApplicationExtensions");
					IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
					app.setAttribute("applicationID", "NETSCAPE");
					app.setAttribute("authenticationCode", "2.0");
					app.setUserObject(new byte[] {1, 0, 0}); // bucle infinito
					apps.appendChild(app);
				}
				meta.setFromTree(format, root);
				writer.writeToSequence(new IIOImage(img, null, meta), param);
			}
			writer.endWriteSequence();
		} finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	private static IIOMetadataNode child(IIOMetadataNode root, String name) {
		for (int i = 0; i < root.getLength(); i++) {
			if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
				return (IIOMetadataNode) root.item(i);
			}
		}
		IIOMetadataNode node = new IIOMetadataNode(name);
		root.appendChild(node);
		return node;
	}
}
