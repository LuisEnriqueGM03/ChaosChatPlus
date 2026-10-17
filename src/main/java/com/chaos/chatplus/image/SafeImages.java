package com.chaos.chatplus.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Checks headers before allocating decoded pixels. Never uses ImageIO's disk cache. */
public final class SafeImages {
	private SafeImages() {}

	public static void checkDimensions(int width, int height, int maxSide, long maxPixels) throws IOException {
		if (width < 1 || height < 1 || width > maxSide || height > maxSide
				|| (long) width * height > maxPixels) {
			throw new IOException("Image dimensions exceed the allowed budget");
		}
	}

	public static BufferedImage read(byte[] bytes, int maxBytes, int maxSide, long maxPixels,
			String requiredFormat) throws IOException {
		if (bytes.length == 0 || bytes.length > maxBytes) throw new IOException("Image file too large or empty");
		try (ImageInputStream input = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
			var readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) throw new IOException("Unknown image format");
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				if (requiredFormat != null && !requiredFormat.equalsIgnoreCase(reader.getFormatName())) {
					throw new IOException("Unexpected image format");
				}
				checkDimensions(reader.getWidth(0), reader.getHeight(0), maxSide, maxPixels);
				return reader.read(0);
			} finally {
				reader.dispose();
			}
		}
	}
}
