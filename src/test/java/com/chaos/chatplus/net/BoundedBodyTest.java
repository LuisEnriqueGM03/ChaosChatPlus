package com.chaos.chatplus.net;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;

class BoundedBodyTest {
	static class Subscription implements Flow.Subscription {
		boolean cancelled; public void request(long count) {} public void cancel() { cancelled = true; }
	}
	@Test void rejectsStreamingBodyBeyondBudget() {
		var subscriber = new BoundedBody(3); var subscription = new Subscription(); subscriber.onSubscribe(subscription);
		subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2}), ByteBuffer.wrap(new byte[]{3,4})));
		assertTrue(subscription.cancelled); assertTrue(subscriber.getBody().toCompletableFuture().isCompletedExceptionally());
	}
	@Test void acceptsBodyWithinBudget() {
		var subscriber = new BoundedBody(3); subscriber.onSubscribe(new Subscription());
		subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2,3}))); subscriber.onComplete();
		assertArrayEquals(new byte[]{1,2,3}, subscriber.getBody().toCompletableFuture().join());
	}
}
