package com.chaos.chatplus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.pack.FontSync;
import com.chaos.chatplus.server.*;
import com.chaos.chatplus.compat.ChaosRanksCompat;

/** Common lifecycle. Every loader installs services before invoking it. */
public final class ChaosChatPlus {
    public static final String MOD_ID = "chaoschatplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
    public static void initialize() { ChatServer.registerReceivers(); }
    public static void serverStarted(MinecraftServer server) {
        if (!ChatConfig.load()) throw new IllegalStateException("Corrige config/chaoschatplus/config.json antes de iniciar el servidor");
        ResourceReload.start(); EmojiRegistry.load(); GifRegistry.load(); FontSync.rebuild();
        ChaosRanksCompat.cleanOrphanTeams(server);
    }
}
