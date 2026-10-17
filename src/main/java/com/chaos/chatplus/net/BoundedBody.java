package com.chaos.chatplus.net;

import java.io.*;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Cancels the HTTP subscription as soon as the actual body crosses the limit, regardless of Content-Length. */
public final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
	private final int limit;
	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private final CompletableFuture<byte[]> result = new CompletableFuture<>();
	private Flow.Subscription subscription;
	public BoundedBody(int limit) { this.limit = limit; }
	public static HttpResponse.BodyHandler<byte[]> handler(int limit) { return info -> new BoundedBody(limit); }
	public CompletionStage<byte[]> getBody() { return result; }
	public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
	public void onNext(List<ByteBuffer> chunks) {
		for (ByteBuffer chunk : chunks) {
			if ((long) bytes.size() + chunk.remaining() > limit) {
				subscription.cancel(); result.completeExceptionally(new IOException("HTTP body exceeds limit")); return;
			}
			byte[] part = new byte[chunk.remaining()]; chunk.get(part); bytes.writeBytes(part);
		}
		subscription.request(1);
	}
	public void onError(Throwable error) { result.completeExceptionally(error); }
	public void onComplete() { result.complete(bytes.toByteArray()); }
}
