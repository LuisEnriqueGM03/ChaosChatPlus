package com.chaos.chatplus.net;

import java.io.ByteArrayOutputStream;

/** Rejects excessive allocation, inconsistent fragments, duplicate indices and aggregate byte overflow. */
public final class PartAssembly {
	private final byte[][] parts;
	private final int maxPartBytes, maxBytes;
	private int received, bytes;
	public PartAssembly(int count, int maxParts, int maxPartBytes, int maxBytes) {
		if (count < 1 || count > maxParts) throw new IllegalArgumentException("Invalid fragment count");
		this.parts = new byte[count][]; this.maxPartBytes = maxPartBytes; this.maxBytes = maxBytes;
	}
	public void accept(int index, int count, byte[] data) {
		if (count != parts.length || index < 0 || index >= parts.length || data.length == 0
				|| data.length > maxPartBytes || parts[index] != null || (long) bytes + data.length > maxBytes) {
			throw new IllegalArgumentException("Invalid or excessive fragment");
		}
		parts[index] = data; received++; bytes += data.length;
	}
	public boolean complete() { return received == parts.length; }
	public byte[] join() {
		if (!complete()) throw new IllegalStateException("Incomplete transfer");
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes);
		for (byte[] part : parts) out.writeBytes(part);
		return out.toByteArray();
	}
}
