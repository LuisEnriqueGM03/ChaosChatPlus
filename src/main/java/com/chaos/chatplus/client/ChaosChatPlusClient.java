package com.chaos.chatplus.client;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.client.ui.GifPanel;
import com.chaos.chatplus.net.ChatPayloads;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class ChaosChatPlusClient implements ClientModInitializer {

	/** false con -Dchaoschatplus.forceVanilla=true: el cliente se porta como uno sin el mod (para probar). */
	public static boolean ACTIVE = true;
	/** Con Chaos Ranks en el cliente se pueden dibujar sus etiquetas (sus fuentes vienen en su jar). */
	public static final boolean CHAOS_RANKS = FabricLoader.getInstance().isModLoaded("chaosranks");

	@Override
	public void onInitializeClient() {
		if (SelfTest.enabled()) {
			SelfTest.register();
		}
		if (Boolean.getBoolean("chaoschatplus.forceVanilla")) {
			ACTIVE = false;
			ChaosChatPlus.LOGGER.info("Chaos ChatPlus en modo vanilla (forceVanilla): el cliente no usa el mod");
			return;
		}
		ClientPrefs.load();
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.ServerInfo.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setInfo(payload.data())));
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.EmojiImage.TYPE, (payload, context) ->
				context.client().execute(() -> EmojiTextures.accept(payload.name(), payload.png())));
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.SelfPrefix.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setPrefix(payload.prefix().orElse(null), payload.fallback().orElse(null))));
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.ShotData.TYPE, (payload, context) ->
				context.client().execute(() -> ShotCache.accept(payload)));
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.GifResults.TYPE, (payload, context) ->
				context.client().execute(() -> GifPanel.accept(payload.data())));
		ClientPlayNetworking.registerGlobalReceiver(ChatPayloads.GifData.TYPE, (payload, context) ->
				context.client().execute(() -> GifCache.accept(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			GifCache.clear();
			GifPanel.reset();
			ClientState.reset();
			EmojiTextures.clear();
			ShotCache.clear();
		}));
		ClientTickEvents.START_CLIENT_TICK.register(ShotCapture::startTick);
		ClientTickEvents.END_CLIENT_TICK.register(ShotCapture::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> GifCache.tick());
		ClientTickEvents.END_CLIENT_TICK.register(client -> AnimePrefetch.tick());
	}
}
