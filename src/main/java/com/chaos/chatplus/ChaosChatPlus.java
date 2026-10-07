package com.chaos.chatplus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chaos.chatplus.command.ChatPlusCommand;
import com.chaos.chatplus.compat.ChaosRanksCompat;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.pack.PackHost;
import com.chaos.chatplus.server.ChatDecorator;
import com.chaos.chatplus.server.ChatServer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;

public class ChaosChatPlus implements ModInitializer {

	public static final String MOD_ID = "chaoschatplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(ChatPayloads.ServerInfo.TYPE, ChatPayloads.ServerInfo.CODEC);
		PayloadTypeRegistry.playS2C().register(ChatPayloads.EmojiImage.TYPE, ChatPayloads.EmojiImage.CODEC);
		PayloadTypeRegistry.playS2C().register(ChatPayloads.SelfPrefix.TYPE, ChatPayloads.SelfPrefix.CODEC);
		PayloadTypeRegistry.playS2C().register(ChatPayloads.ShotData.TYPE, ChatPayloads.ShotData.CODEC);
		PayloadTypeRegistry.playS2C().register(ChatPayloads.GifResults.TYPE, ChatPayloads.GifResults.CODEC);
		PayloadTypeRegistry.playS2C().register(ChatPayloads.GifData.TYPE, ChatPayloads.GifData.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.GifSearch.TYPE, ChatPayloads.GifSearch.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.GifRequest.TYPE, ChatPayloads.GifRequest.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.SendGif.TYPE, ChatPayloads.SendGif.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.UploadEmoji.TYPE, ChatPayloads.UploadEmoji.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.ShareItem.TYPE, ChatPayloads.ShareItem.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.ShotPart.TYPE, ChatPayloads.ShotPart.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.RequestShot.TYPE, ChatPayloads.RequestShot.CODEC);
		PayloadTypeRegistry.playC2S().register(ChatPayloads.ReportShot.TYPE, ChatPayloads.ReportShot.CODEC);

		ChatServer.registerReceivers();
		ChatDecorator.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				ChatPlusCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ChatConfig.load();
			EmojiRegistry.load();
			GifRegistry.load();
			PackHost.start(server);
			ChaosRanksCompat.cleanOrphanTeams(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PackHost.stop();
			ChatServer.stop();
		});
		ServerTickEvents.END_SERVER_TICK.register(ChatServer::tick);
		ServerConfigurationConnectionEvents.CONFIGURE.register(PackHost::configure);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ChatServer.join(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ChatServer.leave(handler.player));
	}
}
