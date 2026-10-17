package com.chaos.chatplus.server;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.pack.FontSync;
import net.minecraft.server.MinecraftServer;

/** One bounded background job per server; filesystem scans and image conversion never run inside tick(). */
public final class ResourceReload {
	private static ExecutorService worker;
	private static long generation;
	private static final AtomicBoolean POLLING = new AtomicBoolean();
	private ResourceReload() {}

	public static void start() {
		stop();
        synchronized (ResourceReload.class) {
		worker = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "ChatPlus resources"); thread.setDaemon(true); return thread;
		});
        }
	}

	public static synchronized void poll(MinecraftServer server) {
		if (worker == null || !POLLING.compareAndSet(false, true)) return;
		long session = generation;
		worker.submit(() -> {
			try {
				if (GifRegistry.changed()) GifRegistry.load();
				int before = EmojiRegistry.version();
				if (EmojiRegistry.changed()) EmojiRegistry.load();
				if (EmojiRegistry.version() != before) {
					FontSync.rebuild();
					server.execute(() -> { if (active(session)) ChatServer.resyncPrepared(server); });
				}
			} catch (Exception e) {
				ChaosChatPlus.LOGGER.warn("No se pudieron recargar los recursos; se reintentará", e);
			} finally {
				if (active(session)) POLLING.set(false);
			}
		});
	}

	private static synchronized boolean active(long session) { return session == generation && worker != null; }

    public static void stop() {
        ExecutorService previous;
        synchronized (ResourceReload.class) {
            generation++;
            previous = worker;
            worker = null;
            POLLING.set(false);
        }
        if (previous == null) return;
        previous.shutdownNow();
        try {
            if (!previous.awaitTermination(10, TimeUnit.SECONDS)) {
                ChaosChatPlus.LOGGER.warn("El trabajador de recursos aún se está cerrando");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
