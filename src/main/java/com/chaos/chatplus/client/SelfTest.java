package com.chaos.chatplus.client;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;
import com.chaos.chatplus.client.screen.ItemPickerScreen;
import com.chaos.chatplus.client.screen.ShotSendScreen;
import com.chaos.chatplus.client.screen.ShotViewerScreen;
import com.chaos.chatplus.client.ui.ChatBar;
import com.chaos.chatplus.mixin.client.ChatComponentAccessor;
import com.chaos.chatplus.net.ChatPayloads;
import com.chaos.chatplus.server.Shots;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;

/**
 * Autoprueba visual, solo en desarrollo (-Dchaoschatplus.selftest=true): con el servidor de pruebas en marcha,
 * "Luis" (con el mod) y "Pepe" (vanilla) siguen un guion y dejan capturas en run*\/screenshots/selftest_*.png.
 */
public final class SelfTest {

	/** Lo implementa ChatScreen (mixin) para que la prueba pueda tocar la barra. */
	public interface BarAccess {
		@Nullable
		ChatBar chaoschatplus$bar();
	}

	private static int ticks = -1;

	private SelfTest() {}

	public static boolean enabled() {
		return Boolean.getBoolean("chaoschatplus.selftest");
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.getConnection() == null) {
			return;
		}
		mc.options.pauseOnLostFocus = false;
		ticks++;
		if (mc.player.getGameProfile().getName().equals("Luis")) {
			luis(mc);
		} else {
			pepe(mc);
		}
	}

	private static void luis(Minecraft mc) {
		switch (ticks) {
			case 30 -> mc.player.connection.sendCommand(
					"give @s minecraft:diamond_sword[minecraft:enchantments={levels:{\"minecraft:sharpness\":5}},minecraft:custom_name='\"Excalibur\"']");
			case 40 -> mc.player.connection.sendCommand("time set day");
			case 60 -> mc.player.connection.sendChat("Hola a todos :chaos: :fuego: :risa: 😂 ❤️ mirad [item]");
			case 100 -> shot("01_chat_cerrado");
			case 120 -> mc.setScreen(new ChatScreen("Probando :cor"));
			case 160 -> shot("02_autocompletado");
			case 180 -> mc.setScreen(new ChatScreen("@P"));
			case 200 -> shot("03_mencion_autocompletado");
			case 220 -> {
				mc.setScreen(new ChatScreen(""));
			}
			case 225 -> {
				if (mc.screen instanceof BarAccess b && b.chaoschatplus$bar() != null) {
					b.chaoschatplus$bar().togglePicker();
				}
			}
			case 260 -> shot("04_selector_emojis");
			case 280 -> mc.setScreen(new ItemPickerScreen(new ChatScreen("")));
			case 320 -> shot("05_mostrar_item");
			case 330 -> {
				ClientPlayNetworking.send(new ChatPayloads.ShareItem(0));
				mc.setScreen(null);
			}
			case 380 -> ShotCapture.start();
			case 400 -> shot("06a_modo_camara");
			case 410 -> ShotCapture.shoot();
			case 430 -> shot("06_enviar_captura");
			case 440 -> {
				if (mc.screen instanceof ShotSendScreen s) {
					s.sendForTest();
				}
			}
			case 520 -> mc.setScreen(new ChatScreen(""));
			case 560 -> shot("07_chat_abierto");
			case 580 -> {
				String id = lastShotId(mc);
				ChaosChatPlus.LOGGER.info("[SelfTest] última captura: {}", id);
				if (id != null) {
					mc.setScreen(new ShotViewerScreen(id, new ChatScreen(""), false));
				}
			}
			case 640 -> shot("08_visor_captura");
			case 660 -> mc.setScreen(new ChatScreen("hola :diablo: que tal :fuego: "));
			case 700 -> shot("09_emoji_en_barra");
			case 720 -> mc.setScreen(new ChatScreen("a".repeat(256)));
			case 740 -> shot("09b_limite");
			case 760 -> {
				String id = lastShotId(mc);
				if (id != null) {
					mc.setScreen(new ShotViewerScreen(id, new ChatScreen(""), true));
				}
			}
			case 800 -> shot("09c_revision");
			case 820 -> {
				mc.setScreen(null);
				mc.player.connection.sendCommand("kill");
			}
			case 850 -> {
				mc.player.respawn();
				mc.setScreen(null);
			}
			case 870 -> ClientPlayNetworking.send(new ChatPayloads.SendGif("local:creeper_baile"));
			case 920 -> mc.setScreen(new ChatScreen(""));
			case 960 -> shot("11_muerte_y_gif");
			case 980 -> {
				if (mc.screen instanceof BarAccess b && b.chaoschatplus$bar() != null) {
					b.chaoschatplus$bar().openGifsForTest();
				}
			}
			case 1040 -> shot("12_pestana_gifs");
			case 1060 -> mc.setScreen(null);
			case 1300 -> mc.setScreen(new ChatScreen(""));
			case 1340 -> shot("10_mencion_recibida");
			case 1400 -> mc.stop();
			default -> { }
		}
	}

	private static void pepe(Minecraft mc) {
		switch (ticks) {
			case 200 -> mc.player.connection.sendChat("@Luis mira esto :corazon: :fuego: (desde vanilla)");
			case 260 -> mc.player.connection.sendChat("@everyone esto no debería sonar");
			case 500 -> mc.setScreen(new ChatScreen(""));
			case 540 -> shot("pepe_chat");
			case 600 -> mc.stop();
			default -> { }
		}
	}

	@Nullable
	private static String lastShotId(Minecraft mc) {
		List<GuiMessage.Line> lines = ((ChatComponentAccessor) mc.gui.getChat()).chaoschatplus$lines();
		AtomicReference<String> found = new AtomicReference<>();
		for (GuiMessage.Line line : lines) {
			line.content().accept((index, style, codepoint) -> {
				ClickEvent click = style.getClickEvent();
				if (found.get() == null && click != null && click.getValue().startsWith(Shots.VIEW_COMMAND)) {
					found.set(click.getValue().substring(Shots.VIEW_COMMAND.length()));
				}
				return true;
			});
			if (found.get() != null) {
				return found.get();
			}
		}
		return null;
	}

	private static void shot(String name) {
		Minecraft mc = Minecraft.getInstance();
		Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(), msg -> { });
		ChaosChatPlus.LOGGER.info("[SelfTest] captura {}", name);
	}
}
