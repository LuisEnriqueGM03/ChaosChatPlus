package com.chaos.chatplus.compat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.chaos.chatplus.ChaosChatPlus;

import com.chaos.chatplus.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Integración opcional con Chaos Ranks (sin depender de él al compilar): rango de cada jugador, su etiqueta y su
 * formato de chat. Si Chaos Ranks no está, todo devuelve null o vacío y el mod funciona igual.
 */
public final class ChaosRanksCompat {

	/** Lo que se necesita del rango de un jugador. */
	public record Rank(String id, String label, int bgColor, Component badge, Style nameStyle, String separator,
			int separatorColor) {}

	private static boolean present;
	private static Method rankOf;
	private static Method ranks;
	private static Method build;
	private static Method nameStyle;
	private static Field id;
	private static Field label;
	private static Field bgColor;
	private static Field separator;
	private static Field separatorColor;

	static {
		if (Platform.get().loaded("chaosranks")) {
			try {
				Class<?> store = Class.forName("com.chaos.ranks.rank.RankStore");
				Class<?> def = Class.forName("com.chaos.ranks.rank.RankDef");
				Class<?> renderer = Class.forName("com.chaos.ranks.badge.BadgeRenderer");
				rankOf = store.getMethod("rankOf", ServerPlayer.class);
				ranks = store.getMethod("ranks");
				build = renderer.getMethod("build", def);
				nameStyle = def.getMethod("nameStyle");
				id = def.getField("id");
				label = def.getField("label");
				bgColor = def.getField("bgColor");
				separator = def.getField("separator");
				separatorColor = def.getField("separatorColor");
				present = true;
				ChaosChatPlus.LOGGER.info("Chaos Ranks detectado: etiqueta en la barra del chat, permisos y @rango activos");
			} catch (ReflectiveOperationException | LinkageError e) {
				ChaosChatPlus.LOGGER.warn("Chaos Ranks está instalado pero no se pudo enlazar (¿versión distinta?)", e);
			}
		}
	}

	private ChaosRanksCompat() {}

	public static boolean present() {
		return present;
	}

	/**
	 * Sin Chaos Ranks: borra los equipos "chaosrank_*" que dejó guardados en el mundo, para que sus etiquetas no salgan
	 * como cuadros vacíos en los nombres.
	 */
	public static void cleanOrphanTeams(MinecraftServer server) {
		if (present || Platform.get().loaded("chaosranks")) {
			return;
		}
		ServerScoreboard board = server.getScoreboard();
		List<PlayerTeam> orphans = board.getPlayerTeams().stream()
				.filter(t -> t.getName().startsWith("chaosrank_")).toList();
		orphans.forEach(board::removePlayerTeam);
		if (!orphans.isEmpty()) {
			ChaosChatPlus.LOGGER.info("Chaos Ranks no está instalado: borrados {} equipos chaosrank_* que quedaban en el mundo",
					orphans.size());
		}
	}

	/**
	 * Fuentes y texturas de Chaos Ranks (assets/chaosranks/font y textures/font) para meterlas en el pack de los
	 * clientes vanilla. Ruta dentro del zip → contenido, en orden estable. Vacío si Chaos Ranks no está.
	 */
	public static Map<String, byte[]> clientAssets() {
		Map<String, byte[]> out = new TreeMap<>();
		if (!present) {
			return out;
		}
		{
			for (String dir : new String[] {"assets/chaosranks/font", "assets/chaosranks/textures/font"}) {
				Platform.get().resource("chaosranks", dir).ifPresent(root -> {
					try (Stream<Path> files = Files.walk(root)) {
						for (Path p : files.filter(Files::isRegularFile).toList()) {
							String rel = dir + "/" + root.relativize(p).toString().replace(java.io.File.separatorChar, '/');
							out.put(rel, Files.readAllBytes(p));
						}
					} catch (IOException e) {
						ChaosChatPlus.LOGGER.warn("No se pudieron leer las fuentes de Chaos Ranks", e);
					}
				});
			}
		}
		return out;
	}

	@Nullable
	public static Rank rankOf(ServerPlayer player) {
		if (!present) {
			return null;
		}
		try {
			return toRank(rankOf.invoke(null, player));
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	@Nullable
	public static String rankId(ServerPlayer player) {
		Rank rank = rankOf(player);
		return rank == null ? null : rank.id();
	}

	public static List<Rank> ranks() {
		List<Rank> out = new ArrayList<>();
		if (!present) {
			return out;
		}
		try {
			for (Object def : (Collection<?>) ranks.invoke(null)) {
				Rank rank = toRank(def);
				if (rank != null) {
					out.add(rank);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			ChaosChatPlus.LOGGER.debug("No se pudieron leer los rangos", e);
		}
		return out;
	}

	@Nullable
	private static Rank toRank(@Nullable Object def) throws ReflectiveOperationException {
		if (def == null) {
			return null;
		}
		Component badge = (Component) build.invoke(null, def);
		return new Rank((String) id.get(def), (String) label.get(def), bgColor.getInt(def), badge,
				(Style) nameStyle.invoke(def), (String) separator.get(def), separatorColor.getInt(def));
	}
}
