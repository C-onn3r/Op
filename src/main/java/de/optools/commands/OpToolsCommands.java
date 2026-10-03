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
import de.optools.opsucht.IncomingText;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

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
				.then(ClientCommandManager.literal("quellen").executes(ctx -> sources(ctx)))
				.then(ClientCommandManager.literal("debuglog").executes(OpToolsCommands::toggleDebugLog))
				.then(ClientCommandManager.literal("rtp").executes(ctx -> {
					var s = mod.rtpTracker().snapshot();
					return feedback(ctx, "RTP-Status: " + s.status().label + (s.biome() != null ? " · Biom " + s.biome() : "")
							+ (s.position() >= 0 ? " · Position #" + s.position() : "")
							+ (s.lastMessage() != null ? " · letzte Meldung: " + s.lastMessage() : ""));
				}))
				.then(ClientCommandManager.literal("reload").executes(ctx -> {
					mod.reloadFiles();
					int errors = mod.patterns().errors().size();
					return feedback(ctx, "Konfiguration, Kurzbefehle und OPSUCHT-Patterns neu geladen"
							+ (errors > 0 ? " (" + errors + " fehlerhafte Patterns, siehe Log)." : ".")
							+ " Neue Kurzbefehle gelten nach dem nächsten Serverbeitritt.");
				}))
				.then(ClientCommandManager.literal("debug")
						.executes(OpToolsCommands::toggleDebugLog)
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

	private static int toggleDebugLog(CommandContext<FabricClientCommandSource> ctx) {
		OpTools mod = OpTools.get();
		var general = mod.config().general;
		general.debugLogIncoming = !general.debugLogIncoming;
		mod.saveConfig();
		return feedback(ctx, general.debugLogIncoming
				? "Debug-Log AN – alle Actionbar-/Titel-/Bossbar-/Systemtexte werden nach "
				+ mod.incoming().logFile() + " geschrieben. Nochmal /optools debug zum Ausschalten."
				: "Debug-Log AUS. Tipp: /optools debug <Text> testet, wie ein Text erkannt wird.");
	}

	/** Lists the last texts received on every client path, each clickable to copy. */
	private static int sources(CommandContext<FabricClientCommandSource> ctx) {
		OpTools mod = OpTools.get();
		ctx.getSource().sendFeedback(Component.literal("[OP Tools] ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal("Zuletzt empfangene Servertexte (Klick = kopieren)"
						+ (mod.isActiveServer() ? "" : " – Server wird NICHT als OPSUCHT erkannt!"))
						.withStyle(mod.isActiveServer() ? ChatFormatting.GRAY : ChatFormatting.RED)));
		for (IncomingText.Source source : IncomingText.Source.values()) {
			List<IncomingText.Entry> entries = mod.incoming().recent(source);
			int from = Math.max(0, entries.size() - 3);
			MutableComponent line = Component.literal(" " + source.label + ": ").withStyle(ChatFormatting.DARK_AQUA);
			if (entries.isEmpty()) line.append(Component.literal("–").withStyle(ChatFormatting.DARK_GRAY));
			ctx.getSource().sendFeedback(line);
			for (IncomingText.Entry e : entries.subList(from, entries.size())) {
				String text = e.plain().length() > 120 ? e.plain().substring(0, 120) + "…" : e.plain();
				ctx.getSource().sendFeedback(Component.literal("   " + text).withStyle(style -> style.withColor(ChatFormatting.WHITE)
						.withClickEvent(new ClickEvent.CopyToClipboard(e.plain()))
						.withHoverEvent(new HoverEvent.ShowText(Component.literal("Klicken zum Kopieren")))));
			}
		}
		return 1;
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
