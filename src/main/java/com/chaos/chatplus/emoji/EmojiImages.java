package com.chaos.chatplus.emoji;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.chaos.chatplus.image.SafeImages;

/** Validation and normalization shared by uploads and files installed by the administrator. */
public final class EmojiImages {
	public static final int MAX_FILE_BYTES = 8 * 1024 * 1024;
	private EmojiImages() {}

	public static byte[] normalize(byte[] data) {
		try {
			BufferedImage img = SafeImages.read(data, MAX_FILE_BYTES, 1024, 1024L * 1024, "png");
			double scale = Math.min(1.0, 32.0 / Math.max(img.getWidth(), img.getHeight()));
			int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
			int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
			BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = out.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(img, 0, 0, w, h, null);
			g.dispose();
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ImageIO.write(out, "png", bytes);
			return bytes.toByteArray();
		} catch (Exception e) { return null; }
	}

	public static boolean validUploadSize(byte[] data) {
		try {
			return SafeImages.read(data, 30 * 1024, 128, 128L * 128, "png") != null;
		} catch (Exception e) { return false; }
	}
}
