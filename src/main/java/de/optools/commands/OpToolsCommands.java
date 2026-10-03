package de.optools.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.optools.OpTools;
import de.optools.gui.screen.HudEditorScreen;
import de.optools.gui.screen.MainScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Registers {@code /optools} and all configured shortcuts as client commands. */
public final class OpToolsCommands {
	private OpToolsCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		OpTools mod = OpTools.get();
		dispatcher.register(ClientCommandManager.literal("optools")
				.executes(ctx -> open(MainScreen.Tab.OVERVIEW))
				.then(ClientCommandManager.literal("jobs").executes(ctx -> open(MainScreen.Tab.JOBS)))
				.then(ClientCommandManager.literal("finanzen").executes(ctx -> open(MainScreen.Tab.FINANCE)))
				.then(ClientCommandManager.literal("markt").executes(ctx -> open(MainScreen.Tab.MARKET)))
				.then(ClientCommandManager.literal("shards").executes(ctx -> open(MainScreen.Tab.SHARDS)))
				.then(ClientCommandManager.literal("einstellungen").executes(ctx -> open(MainScreen.Tab.SETTINGS)))
				.then(ClientCommandManager.literal("hud").executes(ctx -> {
					mod.openScreenNextTick(() -> new HudEditorScreen(null));
					return 1;
				}))
				.then(ClientCommandManager.literal("session")
						.then(ClientCommandManager.literal("neu").executes(ctx -> {
							mod.jobTracker().finishSession();
							return feedback(ctx, "Job-Session beendet und gespeichert. Die nächste Aktion startet eine neue.");
						}))
						.then(ClientCommandManager.literal("pause").executes(ctx -> {
							boolean paused = !mod.jobTracker().isPaused();
							mod.jobTracker().setPaused(paused);
							return feedback(ctx, paused ? "Job-Tracking pausiert." : "Job-Tracking fortgesetzt.");
						})))
				.then(ClientCommandManager.literal("reload").executes(ctx -> {
					mod.reloadFiles();
					int errors = mod.patterns().errors().size();
					return feedback(ctx, "Konfiguration, Kurzbefehle und OPSUCHT-Patterns neu geladen"
							+ (errors > 0 ? " (" + errors + " fehlerhafte Patterns, siehe Log)." : ".")
							+ " Neue Kurzbefehle gelten nach dem nächsten Serverbeitritt.");
				}))
				.then(ClientCommandManager.literal("debug")
						.then(ClientCommandManager.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
							String text = StringArgumentType.getString(ctx, "text");
							return feedback(ctx, mod.debugParse(text));
						}))));

		if (!mod.config().modules.commandShortcuts) return;
		for (CommandShortcut shortcut : mod.shortcuts().all()) {
			if (!shortcut.enabled || "optools".equals(shortcut.alias)) continue;
			dispatcher.register(ClientCommandManager.literal(shortcut.alias)
					.executes(ctx -> run(shortcut, ""))
					.then(ClientCommandManager.argument("args", StringArgumentType.greedyString())
							.executes(ctx -> run(shortcut, StringArgumentType.getString(ctx, "args")))));
		}
	}

	private static int open(MainScreen.Tab tab) {
		OpTools.get().openScreenNextTick(() -> new MainScreen(tab));
		return 1;
	}

	private static int run(CommandShortcut shortcut, String args) {
		var player = Minecraft.getInstance().player;
		if (player == null) return 0;
		player.connection.sendCommand(shortcut.resolve(args));
		return 1;
	}

	private static int feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
		ctx.getSource().sendFeedback(Component.literal("[OP Tools] ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
		return 1;
	}
}
