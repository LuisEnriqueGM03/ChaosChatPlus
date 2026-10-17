package com.chaos.chatplus.client;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.client.ui.GifPanel;
import com.chaos.chatplus.net.ChatPayloads;


public class ChaosChatPlusClient  {

	/** false con -Dchaoschatplus.forceVanilla=true: el cliente se porta como uno sin el mod (para probar). */
	public static boolean ACTIVE = true;
	/** Con Chaos Ranks en el cliente se pueden dibujar sus etiquetas (sus fuentes vienen en su jar). */
	public static final boolean CHAOS_RANKS = com.chaos.chatplus.platform.Platform.get().loaded("chaosranks");

	public static void initialize() {
		if (Boolean.getBoolean("chaoschatplus.forceVanilla")) {
			ACTIVE = false;
			ChaosChatPlus.LOGGER.info("Chaos ChatPlus en modo vanilla (forceVanilla): el cliente no usa el mod");
			return;
		}
		ClientPrefs.load();
		ClientNetwork.registerGlobalReceiver(ChatPayloads.FontPart.TYPE, (payload, context) ->
				context.client().execute(() -> FontInjector.accept(payload.part(), payload.parts(), payload.data())));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.ServerInfo.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setInfo(payload.data())));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.EmojiImage.TYPE, (payload, context) ->
				context.client().execute(() -> EmojiTextures.accept(payload.name(), payload.png())));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.SelfPrefix.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setPrefix(payload.prefix().orElse(null), payload.fallback().orElse(null))));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.ShotData.TYPE, (payload, context) ->
				context.client().execute(() -> ShotCache.accept(payload)));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.GifResults.TYPE, (payload, context) ->
				context.client().execute(() -> GifPanel.accept(payload.data())));
		ClientNetwork.registerGlobalReceiver(ChatPayloads.GifData.TYPE, (payload, context) ->
				context.client().execute(() -> GifCache.accept(payload)));
    }
    public static void disconnect() {
        GifCache.clear(); FontInjector.clear(); GifPanel.reset(); ClientState.reset(); EmojiTextures.clear(); ShotCache.clear();
    }
    public static void startTick(net.minecraft.client.Minecraft client) { if (ACTIVE) ShotCapture.startTick(client); }
    public static void endTick(net.minecraft.client.Minecraft client) {
        if (SelfTest.enabled()) SelfTest.tick(client);
        if (ACTIVE) { ShotCapture.tick(client); GifCache.tick(); }

	}
}
