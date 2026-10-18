package com.chaos.chatplus.client;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.client.screen.*;
import com.chaos.chatplus.client.ui.ChatBar;
import com.chaos.chatplus.client.ui.GifPanel;
import com.chaos.chatplus.gif.GifInfo;
import com.chaos.chatplus.gif.GifServer;
import com.chaos.chatplus.mixin.client.ChatComponentAccessor;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.server.ItemShare;
import com.chaos.chatplus.server.Shots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import java.nio.file.*;
import java.util.*;

/** Opt-in development test against an isolated dedicated server; never runs during normal play. */
public final class RuntimeTest {
    private static final boolean ENABLED = Boolean.getBoolean("chaoschatplus.runtimeTest");
    private static final Path DIR = Path.of(System.getProperty("chaoschatplus.runtimeDir", "."));
    private static final long START = System.currentTimeMillis();
    private static long entered = START, lastTick;
    private static int phase, checks;
    private static boolean finished, joined;
    private static boolean attemptedConnection;
    private static long lastWaitingLog;
    private static String role = "unknown", publicShot, privateShot;
    private static GifInfo chosen, privateGif;
    private static final Set<String> shots = new LinkedHashSet<>();
    private static final Map<String, GifInfo> gifs = new LinkedHashMap<>();
    private static boolean item, publicText, privateText, emoji, privateItem, privateEmoji;

    private RuntimeTest() {}

    public static void tick(Minecraft mc) {
        if (!ENABLED || finished || System.currentTimeMillis() - lastTick < 100) return;
        lastTick = System.currentTimeMillis();
        try {
            if (lastTick - START > 600_000 || lastTick - entered > 180_000)
                throw new IllegalStateException("Runtime test timed out at phase " + phase + ", screen=" + mc.screen);
            if (mc.player == null || mc.level == null || mc.getConnection() == null) {
                if (joined) throw new IllegalStateException("Disconnected before runtime test completed: " + mc.screen);
                if (mc.screen != null && lastTick - lastWaitingLog > 15_000) {
                    lastWaitingLog = lastTick;
                    ChaosChatPlus.LOGGER.info("[RuntimeTest] waiting on {}", mc.screen.getClass().getName());
                    shot(mc, "startup");
                }
                if (!attemptedConnection && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
                    attemptedConnection = true;
                    int port = DIR.toString().contains("neoforge") ? 25633 : DIR.toString().contains("forge") ? 25632 : 25631;
                    String address = "127.0.0.1:" + port;
                    net.minecraft.client.gui.screens.ConnectScreen.startConnecting(mc.screen, mc,
                            net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),
                            new net.minecraft.client.multiplayer.ServerData("Runtime", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
                }
                if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))
                    mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                return;
            }
            joined = true;
            role = mc.player.getGameProfile().getName();
            mc.options.pauseOnLostFocus = false;
            observe(mc);
            if (phase == 0) {
                if (!ClientState.serverMod() || ClientState.emojis().size() < 197 || mc.getConnection().getOnlinePlayers().size() < 3) return;
                check(ClientState.items() && ClientState.shots() && ClientState.gifs(), "feature permissions synchronized");
                check(!ChaosChatPlusClient.CHAOS_RANKS || ClientState.prefix() != null, "optional ranks integration state valid");
                check(!((Set<?>) field(FontInjector.class, null, "INJECTED")).isEmpty(), "server font injected");
                mark("ready", "ok"); next();
            } else if (role.equals("Luis")) sender(mc);
            else if (role.equals("Pepe")) recipient(mc);
            else observer(mc);
        } catch (Throwable error) {
            finished = true;
            ChaosChatPlus.LOGGER.error("[RuntimeTest] FAIL {} phase {}", role, phase, error);
            try { mark("FAIL", error.toString()); } catch (Exception ignored) {}
            mc.stop();
        }
    }

    private static void sender(Minecraft mc) throws Exception {
        switch (phase) {
            case 1 -> {
                if (!exists("Pepe.ready") || !exists("Audit.ready") || !mc.player.hasPermissions(2)) return;
                mc.player.connection.sendCommand("give @s minecraft:diamond_sword");
                mc.player.connection.sendChat("RUNTIME_PUBLIC @Pepe :fuego: :corazon:");
                next();
            }
            case 2 -> {
                if (!exists("Pepe.public") || !exists("Audit.public") || mc.player.getInventory().getItem(0).isEmpty()) return;
                check(emoji, "public emoji rendered with server codepoint");
                ClientNetwork.send(new ChatPayloads.ShareItem(0, ""));
                mc.setScreen(new ChatScreen(":fue")); next();
            }
            case 3 -> {
                if (elapsed() < 1500 || !exists("Pepe.item")) return;
                ChatBar bar = bar(mc);
                EditBox input = (EditBox) field(ChatBar.class, bar, "input");
                check(mc.screen.keyPressed(258, 0, 0), "Tab handles emoji autocomplete");
                check(!input.getValue().equals(":fue"), "autocomplete inserts emoji");
                mc.screen.charTyped('ñ', 0);
                check(input.getValue().contains("ñ"), "Unicode typing works");
                bar.togglePicker(); next();
            }
            case 4 -> {
                if (elapsed() < 1500) return;
                check(field(ChatBar.class, bar(mc), "picker") != null, "emoji picker opens");
                shot(mc, "emojis");
                bar(mc).openGifsForTest(); next();
            }
            case 5 -> {
                List<?> list = (List<?>) field(GifPanel.class, null, "local");
                if (list.size() < 2 || elapsed() < 2000) return;
                chosen = (GifInfo) list.get(0); privateGif = (GifInfo) list.get(1);
                GifCache.Gif loaded = GifCache.get(chosen);
                if (!loaded.ready) return;
                check(loaded.frames.size() > 1, "GIF animation decoded to multiple textures");
                shot(mc, "gifs");
                mark("publicGif", chosen.id()); mark("privateGif", privateGif.id());
                ClientNetwork.send(new ChatPayloads.SendGif(chosen.id(), "", ""));
                mc.setScreen(null); next();
            }
            case 6 -> {
                if (!exists("Pepe.gif") || !exists("Audit.gif")) return;
                ShotCapture.start(); ShotCapture.scrollZoom(1);
                check(ShotCapture.cameraMode() && ShotCapture.zoom() > 1, "camera and zoom active");
                next();
            }
            case 7 -> {
                if (elapsed() < 1500) return;
                shot(mc, "camera"); ShotCapture.shoot(); next();
            }
            case 8 -> {
                if (!(mc.screen instanceof ShotSendScreen screen) || elapsed() < 1500) return;
                check(!ShotCapture.cameraMode(), "camera restores after capture");
                shot(mc, "preview"); screen.sendForTest(); next();
            }
            case 9 -> {
                if (!exists("Pepe.shot") || !exists("Audit.shot")) return;
                publicShot = read("Pepe.shot");
                if (ShotCache.get(publicShot) == null) return;
                check(ShotCache.get(publicShot).jpeg().length > 0, "own screenshot roundtrip");
                mc.setScreen(new ShotViewerScreen(publicShot, new ChatScreen(""), false)); next();
            }
            case 10 -> {
                if (elapsed() < 1500) return;
                shot(mc, "viewer"); mc.setScreen(null);
                // Exercise the actual private-mode ChatScreen input conversion and Enter handling.
                ClientState.setDmTarget("Pepe");
                mc.setScreen(new ChatScreen("RUNTIME_PRIVATE :corazon:"));
                check(mc.screen.keyPressed(257, 0, 0), "Enter submits private-mode chat");
                ClientNetwork.send(new ChatPayloads.ShareItem(0, "Pepe"));
                ClientNetwork.send(new ChatPayloads.SendGif(privateGif.id(), "", "Pepe"));
                next();
            }
            case 11 -> {
                if (!exists("Pepe.private")) return;
                mc.setScreen(new ChatScreen("")); bar(mc).dmPickerForTest(); next();
            }
            case 12 -> {
                if (elapsed() < 1500) return;
                shot(mc, "private_picker");
                ShotCapture.start(); ShotCapture.shoot(); next();
            }
            case 13 -> {
                if (!(mc.screen instanceof ShotSendScreen screen) || elapsed() < 1000) return;
                screen.sendForTest(); next();
            }
            case 14 -> {
                if (!exists("Pepe.privateShot") || !exists("Audit.denied")) return;
                privateShot = read("Pepe.privateShot");
                ClientState.setDmTarget(null);
                // Upload a real PNG through the normal C2S handler; OP approval resynchronizes everyone.
                byte[] png;
                try (var in = RuntimeTest.class.getResourceAsStream("/assets/chaoschatplus/textures/emoji/fuego.png")) {
                    if (in == null) throw new IllegalStateException("Missing built-in PNG");
                    png = in.readAllBytes();
                }
                ClientNetwork.send(new ChatPayloads.UploadEmoji("runtime_probe", png)); next();
            }
            case 15 -> {
                if (ClientState.emoji("runtime_probe") == null || EmojiTextures.custom("runtime_probe") == null
                        || !exists("Pepe.upload") || !exists("Audit.upload")) return;
                check(ClientState.emoji("runtime_probe") != null, "emoji upload and approval roundtrip");
                mc.player.connection.sendCommand("chatplus reload"); mark("reload", "requested"); next();
            }
            case 16 -> {
                if (ClientState.emoji("runtime_added") == null || !exists("Pepe.reload") || !exists("Audit.reload")) return;
                check(EmojiTextures.custom("runtime_added") != null, "directory reload distributes custom texture");
                mc.setScreen(new ChatScreen("Prueba :runtime_probe: :runtime_added:")); next();
            }
            case 17 -> {
                if (elapsed() < 1500 || !exists("Pepe.report")) return;
                shot(mc, "custom_emojis");
                mc.player.connection.sendCommand("chatplus shot delete " + publicShot); next();
            }
            case 18 -> {
                if (!ShotCache.gone(publicShot) || !exists("Pepe.deleted") || !exists("Audit.deleted")) return;
                check(ShotCache.gone(publicShot), "moderator deletion revokes sender cache");
                ClientNetwork.send(new ChatPayloads.ShotPart(0, 1, new byte[] {1,2,3}, ""));
                ClientNetwork.send(new ChatPayloads.RequestShot("missing1")); next();
            }
            case 19 -> {
                if (elapsed() < 2500 || !ShotCache.gone("missing1")) return;
                if (ShotCache.get(privateShot) == null) return;
                check(mc.getConnection() != null && ClientState.serverMod(), "invalid image and missing ID handled without disconnect");
                check(ShotCache.get(privateShot) != null, "private screenshot remains available to owner");
                mark("done", "PASS " + checks); next();
            }
            case 20 -> finish(mc);
            default -> throw new IllegalStateException("Unknown sender phase " + phase);
        }
    }

    private static void recipient(Minecraft mc) throws Exception {
        if (publicText && emoji && !exists("Pepe.public")) { check(true, "public chat, mention and emoji received"); mark("public", "ok"); }
        if (item && !exists("Pepe.item")) { check(true, "shared item click and hover received"); mark("item", "ok"); }
        if (exists("Luis.publicGif") && readyGif(read("Luis.publicGif")) && !exists("Pepe.gif")) {
            check(true, "public GIF received and animated"); mark("gif", "ok");
        }
        if (shots.size() >= 1 && !exists("Pepe.shot")) {
            String id = shots.iterator().next();
            if (ShotCache.get(id) != null) { publicShot = id; check(true, "public screenshot downloaded"); mark("shot", id); }
        }
        if (privateText && privateEmoji && privateItem && exists("Luis.privateGif") && readyGif(read("Luis.privateGif")) && !exists("Pepe.private")) {
            check(true, "private text, emoji, item and GIF received"); mark("private", "ok");
        }
        if (shots.size() >= 2 && !exists("Pepe.privateShot")) {
            String id = shots.stream().filter(s -> !s.equals(publicShot)).findFirst().orElseThrow();
            if (ShotCache.get(id) != null) {
                privateShot = id; check(true, "private screenshot downloaded by recipient"); mark("privateShot", id);
            }
        }
        if (ClientState.emoji("runtime_probe") != null && EmojiTextures.custom("runtime_probe") != null) markOnce("upload");
        if (ClientState.emoji("runtime_added") != null && EmojiTextures.custom("runtime_added") != null) markOnce("reload");
        if (exists("Audit.denied") && privateShot != null && !exists("Pepe.report")) {
            check(ShotCache.get(publicShot) != null, "public screenshot still available before moderation");
            ClientNetwork.send(new ChatPayloads.ReportShot(publicShot)); mark("report", "ok");
        }
        if (publicShot != null && ShotCache.gone(publicShot) && !exists("Pepe.deleted")) {
            check(true, "moderation revokes recipient cache"); mark("deleted", "ok");
        }
        if (exists("Luis.done") && !exists("Pepe.done")) { mark("done", "PASS " + checks); }
        finish(mc);
    }

    private static void observer(Minecraft mc) throws Exception {
        if (publicText && emoji) markOnce("public");
        if (exists("Luis.publicGif") && readyGif(read("Luis.publicGif"))) markOnce("gif");
        if (!shots.isEmpty() && !exists("Audit.shot")) {
            String id = shots.iterator().next();
            if (ShotCache.get(id) != null) { publicShot = id; check(true, "third player receives public screenshot"); mark("shot", id); }
        }
        if (exists("Pepe.privateShot") && !exists("Audit.denied")) {
            String id = read("Pepe.privateShot");
            ClientNetwork.send(new ChatPayloads.RequestShot(id));
            ClientNetwork.send(new ChatPayloads.ReportShot(id));
            if (!ShotCache.gone(id)) return;
            check(!privateText && !privateItem, "private text and items hidden from third player");
            check(!gifs.containsKey(read("Luis.privateGif")), "private GIF hidden from third player");
            check(!shots.contains(id), "private screenshot link hidden from third player");
            check(ShotCache.gone(id), "forged private screenshot request denied");
            mark("denied", "ok");
        }
        if (ClientState.emoji("runtime_probe") != null && EmojiTextures.custom("runtime_probe") != null) markOnce("upload");
        if (ClientState.emoji("runtime_added") != null && EmojiTextures.custom("runtime_added") != null) markOnce("reload");
        if (publicShot != null && ShotCache.gone(publicShot)) markOnce("deleted");
        if (exists("Luis.done") && !exists("Audit.done")) {
            check(!privateText && !privateItem && shots.size() == 1, "private data remains isolated through entire session");
            mark("done", "PASS " + checks);
        }
        finish(mc);
    }

    private static void observe(Minecraft mc) {
        for (var line : ((ChatComponentAccessor) mc.gui.getChat()).chaoschatplus$lines()) {
            StringBuilder text = new StringBuilder();
            final boolean[] lineItem = {false};
            final boolean[] lineEmoji = {false};
            line.content().accept((index, style, cp) -> {
                text.appendCodePoint(cp);
                if (ClientState.emojiByCodepoint(cp) != null) { emoji = true; lineEmoji[0] = true; }
                ClickEvent click = style.getClickEvent();
                if (click != null && click.getValue().startsWith(Shots.VIEW_COMMAND)) shots.add(click.getValue().substring(Shots.VIEW_COMMAND.length()));
                if (click != null && click.getValue().equals(ItemShare.VIEW_COMMAND) && style.getHoverEvent() != null) { item = true; lineItem[0] = true; }
                String insertion = style.getInsertion();
                if (insertion != null && insertion.startsWith(GifServer.MARK)) {
                    GifInfo info = GifInfo.decode(insertion.substring(GifServer.MARK.length()));
                    if (info != null) gifs.put(info.id(), info);
                }
                return true;
            });
            publicText |= text.toString().contains("RUNTIME_PUBLIC");
            privateText |= text.toString().contains("RUNTIME_PRIVATE");
            privateEmoji |= text.toString().contains("RUNTIME_PRIVATE") && lineEmoji[0];
            privateItem |= lineItem[0] && text.toString().contains("✉");
        }
    }

    private static boolean readyGif(String id) {
        GifInfo info = gifs.get(id);
        return info != null && GifCache.get(info).ready && GifCache.get(info).frames.size() > 1;
    }
    private static ChatBar bar(Minecraft mc) {
        if (!(mc.screen instanceof SelfTest.BarAccess access) || access.chaoschatplus$bar() == null)
            throw new IllegalStateException("Chat bar missing on " + mc.screen);
        return access.chaoschatplus$bar();
    }
    private static Object field(Class<?> type, Object owner, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
    private static void next() { phase++; entered = System.currentTimeMillis(); ChaosChatPlus.LOGGER.info("[RuntimeTest] {} phase {}", role, phase); }
    private static long elapsed() { return System.currentTimeMillis() - entered; }
    private static boolean exists(String name) { return Files.exists(DIR.resolve(name)); }
    private static String read(String name) throws Exception { return Files.readString(DIR.resolve(name)).strip(); }
    private static void mark(String name, String value) throws Exception { Files.createDirectories(DIR); Files.writeString(DIR.resolve(role + "." + name), value); }
    private static void markOnce(String name) throws Exception { if (!exists(role + "." + name)) { check(true, name + " synchronized"); mark(name, "ok"); } }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException(label);
        checks++; ChaosChatPlus.LOGGER.info("[RuntimeTest] PASS {}: {}", role, label);
    }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, "runtime_" + name + ".png", mc.getMainRenderTarget(), msg -> {}); }
    private static void finish(Minecraft mc) throws Exception {
        if (exists("Luis.done") && exists("Pepe.done") && exists("Audit.done")) {
            finished = true; ChaosChatPlus.LOGGER.info("[RuntimeTest] COMPLETE {}: {} checks", role, checks); mc.stop();
        }
    }
}
