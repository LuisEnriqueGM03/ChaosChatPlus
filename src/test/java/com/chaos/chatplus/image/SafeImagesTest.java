package com.chaos.chatplus.image;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class SafeImagesTest {
	private byte[] png() throws Exception {
		var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(32, 16, BufferedImage.TYPE_INT_ARGB), "png", out);
		return out.toByteArray();
	}
	@Test void acceptsOrdinaryPng() throws Exception {
		assertEquals(32, SafeImages.read(png(), 32000, 128, 16384, "png").getWidth());
	}
	@Test void rejectsOversizedHeaderBeforeDecoding() throws Exception {
		byte[] image = png(); ByteBuffer.wrap(image).putInt(16, 100000).putInt(20, 100000);
		CRC32 crc = new CRC32(); crc.update(image, 12, 17); ByteBuffer.wrap(image).putInt(29, (int) crc.getValue());
		IOException e = assertThrows(IOException.class, () -> SafeImages.read(image, 32000, 128, 16384, "png"));
		assertTrue(e.getMessage().contains("dimensions"));
	}
	@Test void enforcesCompressedAndPixelBudgets() throws Exception {
		assertThrows(IOException.class, () -> SafeImages.read(png(), 1, 128, 16384, "png"));
		assertThrows(IOException.class, () -> SafeImages.checkDimensions(100, 100, 128, 9999));
		assertThrows(IOException.class, () -> SafeImages.checkDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 1_000_000_000_000L));
	}
	@Test void rejectsInvalidAndWrongFormat() throws Exception {
		assertThrows(IOException.class, () -> SafeImages.read(new byte[]{1,2,3}, 100, 128, 16384, null));
		assertThrows(IOException.class, () -> SafeImages.read(png(), 32000, 128, 16384, "jpeg"));
	}
}
