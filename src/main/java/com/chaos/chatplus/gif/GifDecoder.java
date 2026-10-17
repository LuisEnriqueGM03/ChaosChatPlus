package com.chaos.chatplus.gif;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Decodifica un GIF animado a frames completos (compone cada frame sobre el lienzo, respetando posición, retardo y
 * "disposal"), reducidos a como mucho {@code maxSize} px.
 */
public final class GifDecoder {

	public record Frames(List<BufferedImage> images, int[] delays) {}

	private static final int MAX_FRAMES = 120;

	private GifDecoder() {}

	public static Frames decode(byte[] data, int maxSize) throws Exception {
		try (ImageInputStream in = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, false);
				// Algunos GIFs hacen fallar al lector de Java al contar o leer frames: se usa lo que se pudo leer.
				int count;
				try {
					count = Math.min(MAX_FRAMES, reader.getNumImages(true));
				} catch (Exception e) {
					count = MAX_FRAMES;
				}
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				IIOMetadata stream = reader.getStreamMetadata();
				if (stream != null) {
					Node screen = child(stream.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
					if (screen != null) {
						width = Math.max(width, attr(screen, "logicalScreenWidth", width));
						height = Math.max(height, attr(screen, "logicalScreenHeight", height));
					}
				}
				com.chaos.chatplus.image.SafeImages.checkDimensions(width, height, 2048, 4_194_304);
				if (maxSize < 1 || count < 1) throw new java.io.IOException("Empty GIF or invalid target size");
				// Bound all retained frames, including the unscaled path used by the server shrinker.
				if ((long) Math.min(width, maxSize) * Math.min(height, maxSize) * count > 16_777_216L) {
					throw new java.io.IOException("GIF exceeds decoded frame budget");
				}
				BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				List<BufferedImage> frames = new ArrayList<>();
				int[] delays = new int[count];
				for (int i = 0; i < count; i++) {
					BufferedImage frame;
					javax.imageio.metadata.IIOMetadata frameMeta;
					try {
						com.chaos.chatplus.image.SafeImages.checkDimensions(reader.getWidth(i), reader.getHeight(i), 2048, 4_194_304);
						frame = reader.read(i);
						frameMeta = reader.getImageMetadata(i);
					} catch (Exception e) {
						if (frames.isEmpty()) {
							throw e;
						}
						delays = java.util.Arrays.copyOf(delays, frames.size());
						break;
					}
					Node tree = frameMeta.getAsTree("javax_imageio_gif_image_1.0");
					Node desc = child(tree, "ImageDescriptor");
					Node gce = child(tree, "GraphicControlExtension");
					int x = desc == null ? 0 : attr(desc, "imageLeftPosition", 0);
					int y = desc == null ? 0 : attr(desc, "imageTopPosition", 0);
					int delay = gce == null ? 10 : attr(gce, "delayTime", 10);
					String disposal = gce == null ? "none" : text(gce, "disposalMethod", "none");
					delays[i] = Math.max(20, delay * 10);

					BufferedImage previous = disposal.equals("restoreToPrevious") ? copy(canvas) : null;
					Graphics2D g = canvas.createGraphics();
					g.drawImage(frame, x, y, null);
					g.dispose();
					frames.add(scale(copy(canvas), maxSize));

					if (disposal.equals("restoreToBackgroundColor")) {
						Graphics2D clear = canvas.createGraphics();
						clear.setComposite(java.awt.AlphaComposite.Clear);
						clear.fillRect(x, y, frame.getWidth(), frame.getHeight());
						clear.dispose();
					} else if (previous != null) {
						canvas = previous;
					}
				}
				return new Frames(frames, delays);
			} finally {
				reader.dispose();
			}
		}
	}

	private static BufferedImage copy(BufferedImage img) {
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return out;
	}

	private static BufferedImage scale(BufferedImage img, int maxSize) {
		int w = img.getWidth();
		int h = img.getHeight();
		if (w <= maxSize && h <= maxSize) {
			return img;
		}
		double s = (double) maxSize / Math.max(w, h);
		int nw = Math.max(1, (int) Math.round(w * s));
		int nh = Math.max(1, (int) Math.round(h * s));
		BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, nw, nh, null);
		g.dispose();
		return out;
	}

	private static Node child(Node node, String name) {
		for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
			if (c.getNodeName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	private static int attr(Node node, String name, int def) {
		try {
			return Integer.parseInt(text(node, name, String.valueOf(def)));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String text(Node node, String name, String def) {
		NamedNodeMap attrs = node.getAttributes();
		Node a = attrs == null ? null : attrs.getNamedItem(name);
		return a == null ? def : a.getNodeValue();
	}
}
