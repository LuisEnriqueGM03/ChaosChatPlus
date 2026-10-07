package com.chaos.chatplus.command;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.chaos.chatplus.Lang;
import com.chaos.chatplus.config.ChatConfig;
import com.chaos.chatplus.emoji.Emoji;
import com.chaos.chatplus.emoji.EmojiRegistry;
import com.chaos.chatplus.gif.GifRegistry;
import com.chaos.chatplus.pack.PackHost;
import com.chaos.chatplus.server.ChatServer;
import com.chaos.chatplus.server.Shots;
import com.chaos.chatplus.text.ChatMarks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * /chatplus reload | pack export | shot view|delete|list
 * /emoji list | pending | approve | deny | remove | enable | alias | ranks
 */
public final class ChatPlusCommand {

	private static final SuggestionProvider<CommandSourceStack> EMOJIS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(EmojiRegistry.all().stream().map(Emoji::name), builder);
	private static final SuggestionProvider<CommandSourceStack> PENDING = (ctx, builder) ->
			SharedSuggestionProvider.suggest(EmojiRegistry.pending().stream().map(p -> p.name), builder);
	private static final SuggestionProvider<CommandSourceStack> DISABLED = (ctx, builder) ->
			SharedSuggestionProvider.suggest(EmojiRegistry.disabled(), builder);
	private static final SuggestionProvider<CommandSourceStack> SHOTS = (ctx, builder) ->
			SharedSuggestionProvider.suggest(Shots.ids(), builder);

	private ChatPlusCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("chatplus")
				.then(Commands.literal("item").then(Commands.literal("view").executes(ctx -> {
					ctx.getSource().sendSuccess(() -> Lang.tr("item.view_hint").withStyle(ChatFormatting.GRAY), false);
					return 1;
				})))
				.then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ChatPlusCommand::reload))
				.then(Commands.literal("pack").requires(s -> s.hasPermission(2))
						.then(Commands.literal("export").executes(ChatPlusCommand::export)))
				.then(Commands.literal("shot")
						.then(Commands.literal("view").then(Commands.argument("id", StringArgumentType.word())
								.executes(ctx -> {
									Shots.view(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
									return 1;
								})))
						.then(Commands.literal("review").requires(s -> s.hasPermission(2))
								.then(Commands.argument("id", StringArgumentType.word()).suggests(SHOTS)
										.executes(ctx -> {
											Shots.review(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
											return 1;
										})))
						.then(Commands.literal("delete").requires(s -> s.hasPermission(2))
								.then(Commands.argument("id", StringArgumentType.word()).suggests(SHOTS)
										.executes(ChatPlusCommand::deleteShot)))
						.then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(ChatPlusCommand::listShots))));

		dispatcher.register(Commands.literal("emoji")
				.executes(ChatPlusCommand::list)
				.then(Commands.literal("list").executes(ChatPlusCommand::list))
				.then(Commands.literal("pending").requires(s -> s.hasPermission(2)).executes(ChatPlusCommand::pending))
				.then(Commands.literal("approve").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(PENDING)
								.executes(ChatPlusCommand::approve)))
				.then(Commands.literal("deny").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(PENDING)
								.executes(ChatPlusCommand::deny)))
				.then(Commands.literal("remove").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(EMOJIS)
								.executes(ChatPlusCommand::remove)))
				.then(Commands.literal("enable").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(DISABLED)
								.executes(ChatPlusCommand::enable)))
				.then(Commands.literal("alias").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(EMOJIS)
								.then(Commands.argument("alias", StringArgumentType.word())
										.executes(ChatPlusCommand::alias))))
				.then(Commands.literal("ranks").requires(s -> s.hasPermission(2))
						.then(Commands.argument("name", StringArgumentType.word()).suggests(EMOJIS)
								.executes(ctx -> ranks(ctx, ""))
								.then(Commands.argument("ranks", StringArgumentType.greedyString())
										.executes(ctx -> ranks(ctx, StringArgumentType.getString(ctx, "ranks")))))));
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		ChatConfig.load();
		EmojiRegistry.load();
		GifRegistry.load();
		PackHost.start(ctx.getSource().getServer());
		ChatServer.resyncAll(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Lang.tr("reloaded", EmojiRegistry.all().size()).withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int export(CommandContext<CommandSourceStack> ctx) {
		try {
			Path out = PackHost.export();
			ctx.getSource().sendSuccess(() -> Lang.tr("pack.exported", out.toString(), PackHost.hash()), false);
			return 1;
		} catch (Exception e) {
			ctx.getSource().sendFailure(Component.literal(e.toString()));
			return 0;
		}
	}

	private static int deleteShot(CommandContext<CommandSourceStack> ctx) {
		String id = StringArgumentType.getString(ctx, "id");
		if (Shots.delete(ctx.getSource().getServer(), id)) {
			ctx.getSource().sendSuccess(() -> Lang.tr("shot.deleted", id).withStyle(ChatFormatting.GREEN), true);
			return 1;
		}
		ctx.getSource().sendFailure(Lang.tr("shot.not_found", id));
		return 0;
	}

	private static int listShots(CommandContext<CommandSourceStack> ctx) {
		List<Shots.Shot> shots = Shots.all().stream().sorted((a, b) -> Long.compare(b.created(), a.created())).toList();
		ctx.getSource().sendSuccess(() -> Lang.tr("shot.list", shots.size()), false);
		for (Shots.Shot s : shots) {
			long min = (System.currentTimeMillis() - s.created()) / 60_000;
			MutableComponent line = Component.literal(" " + s.id()).withStyle(st -> st.withColor(0x7AD7FF)
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Shots.VIEW_COMMAND + s.id())))
					.append(Component.literal("  " + s.ownerName() + "  " + min + " min  " + s.reporters().size() + " ⚑")
							.withStyle(ChatFormatting.GRAY));
			ctx.getSource().sendSuccess(() -> line, false);
		}
		return shots.size();
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = ctx.getSource().getPlayer();
		boolean glyphs = player != null && (PackHost.hasPack(player) || ChatServer.isModded(player));
		MutableComponent out = Lang.tr("emoji.list", EmojiRegistry.all().size()).withStyle(ChatFormatting.GOLD).append("\n");
		boolean first = true;
		for (Emoji e : EmojiRegistry.all()) {
			if (!first) {
				out.append(" ");
			}
			first = false;
			Component hover = Component.literal(e.code() + (e.aliases().isEmpty() ? "" : "\n" + String.join(", ", e.aliases())));
			Component item = glyphs && (e.builtin() || player == null || PackHost.hasPack(player))
					? ChatMarks.emoji(e)
					: Component.literal(e.code()).withStyle(ChatFormatting.GRAY);
			out.append(Component.empty().append(item).withStyle(s -> s
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
					.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, e.code()))));
		}
		ctx.getSource().sendSuccess(() -> out, false);
		return 1;
	}

	private static int pending(CommandContext<CommandSourceStack> ctx) {
		List<EmojiRegistry.Pending> list = EmojiRegistry.pending();
		ctx.getSource().sendSuccess(() -> Lang.tr("emoji.pending_list", list.size()).withStyle(ChatFormatting.GOLD), false);
		for (EmojiRegistry.Pending p : list) {
			MutableComponent line = Component.literal(" :" + p.name + ": ").withStyle(ChatFormatting.WHITE)
					.append(Component.literal("(" + p.uploader + ") ").withStyle(ChatFormatting.GRAY))
					.append(Lang.tr("emoji.approve_button").withStyle(s -> s.withColor(ChatFormatting.GREEN)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/emoji approve " + p.name))))
					.append(" ")
					.append(Lang.tr("emoji.deny_button").withStyle(s -> s.withColor(ChatFormatting.RED)
							.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/emoji deny " + p.name))));
			ctx.getSource().sendSuccess(() -> line, false);
		}
		return list.size();
	}

	private static int approve(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		EmojiRegistry.Pending p = EmojiRegistry.approve(name);
		if (p == null) {
			ctx.getSource().sendFailure(Lang.tr("emoji.error.not_found"));
			return 0;
		}
		ChatServer.resyncAll(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Lang.tr("emoji.approved", ":" + p.name + ":", p.uploader).withStyle(ChatFormatting.GREEN), true);
		ServerPlayer uploader = ctx.getSource().getServer().getPlayerList().getPlayerByName(p.uploader);
		if (uploader != null) {
			uploader.sendSystemMessage(Lang.tr("emoji.your_approved", ":" + p.name + ":").withStyle(ChatFormatting.GREEN));
		}
		return 1;
	}

	private static int deny(CommandContext<CommandSourceStack> ctx) {
		String name = StringArgumentType.getString(ctx, "name");
		EmojiRegistry.Pending p = EmojiRegistry.deny(name);
		if (p == null) {
			ctx.getSource().sendFailure(Lang.tr("emoji.error.not_found"));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Lang.tr("emoji.denied", ":" + p.name + ":").withStyle(ChatFormatting.YELLOW), true);
		ServerPlayer uploader = ctx.getSource().getServer().getPlayerList().getPlayerByName(p.uploader);
		if (uploader != null) {
			uploader.sendSystemMessage(Lang.tr("emoji.your_denied", ":" + p.name + ":").withStyle(ChatFormatting.YELLOW));
		}
		return 1;
	}

	private static int remove(CommandContext<CommandSourceStack> ctx) {
		return result(ctx, EmojiRegistry.remove(StringArgumentType.getString(ctx, "name")), "emoji.removed");
	}

	private static int enable(CommandContext<CommandSourceStack> ctx) {
		return result(ctx, EmojiRegistry.enable(StringArgumentType.getString(ctx, "name")), "emoji.enabled");
	}

	private static int alias(CommandContext<CommandSourceStack> ctx) {
		return result(ctx, EmojiRegistry.addAlias(StringArgumentType.getString(ctx, "name"),
				StringArgumentType.getString(ctx, "alias")), "emoji.alias_added");
	}

	private static int ranks(CommandContext<CommandSourceStack> ctx, String ranks) {
		List<String> list = ranks.isBlank() ? List.of() : Arrays.asList(ranks.toLowerCase(Locale.ROOT).split("[ ,]+"));
		return result(ctx, EmojiRegistry.setRanks(StringArgumentType.getString(ctx, "name"), list), "emoji.ranks_set");
	}

	private static int result(CommandContext<CommandSourceStack> ctx, EmojiRegistry.Result result, String okKey) {
		if (result == EmojiRegistry.Result.OK) {
			ChatServer.resyncAll(ctx.getSource().getServer());
			ctx.getSource().sendSuccess(() -> Lang.tr(okKey).withStyle(ChatFormatting.GREEN), true);
			return 1;
		}
		ctx.getSource().sendFailure(Lang.tr("emoji.error." + result.name().toLowerCase(Locale.ROOT), EmojiRegistry.MAX_UPLOAD_SIZE));
		return 0;
	}
}
