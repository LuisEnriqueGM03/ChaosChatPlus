package com.chaos.chatplus.gif;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class GifDecoderTest {
	byte[] gif() throws Exception {
		var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB), "gif", out);
		return out.toByteArray();
	}
	@Test void decodesNormalGif() throws Exception {
		var frames = GifDecoder.decode(gif(), 16);
		assertEquals(1, frames.images().size()); assertEquals(16, frames.images().get(0).getWidth());
	}
	@Test void rejectsHugeLogicalCanvasBeforeAllocating() throws Exception {
		byte[] data = gif(); data[6]=(byte)255; data[7]=(byte)255; data[8]=(byte)255; data[9]=(byte)255;
		assertThrows(IOException.class, () -> GifDecoder.decode(data, 128));
	}
	@Test void rejectsCorruptData() { assertThrows(Exception.class, () -> GifDecoder.decode(new byte[]{1,2,3}, 128)); }
}
