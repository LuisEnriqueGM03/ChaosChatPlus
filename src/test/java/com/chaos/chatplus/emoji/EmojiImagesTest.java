package com.chaos.chatplus.emoji;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class EmojiImagesTest {
	private byte[] image(int width, String format) throws Exception {
		var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(width, 16, BufferedImage.TYPE_INT_RGB), format, out);
		return out.toByteArray();
	}
	@Test void rejectsOversizedUploadsAndNonPngFiles() throws Exception {
		assertFalse(EmojiImages.validUploadSize(image(129, "png")));
		assertFalse(EmojiImages.validUploadSize(image(32, "jpg")));
		assertNull(EmojiImages.normalize(new byte[]{1,2}));
	}
	@Test void normalizesAndKeepsAspectRatio() throws Exception {
		byte[] clean = EmojiImages.normalize(image(64, "png"));
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(clean));
		assertEquals(32, decoded.getWidth()); assertEquals(8, decoded.getHeight());
		assertTrue(EmojiImages.validUploadSize(clean));
	}
}
