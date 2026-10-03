package de.optools.chat;

import de.optools.config.OpToolsConfig;
import de.optools.opsucht.parse.ChatLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;
import java.util.function.Supplier;

/** Makes the sender name of chat lines clickable: prepares (or runs) {@code /msg <Name>}. */
public final class PlayerNameAction implements ChatAction {
	private final Supplier<OpToolsConfig.Chat> config;

	public PlayerNameAction(Supplier<OpToolsConfig.Chat> config) {
		this.config = config;
	}

	@Override
	public String id() {
		return "player-name";
	}

	@Override
	public boolean isEnabled() {
		return config.get().clickableNames;
	}

	@Override
	public void collect(String plain, ChatLine line, List<ComponentRewriter.Edit> edits) {
		if (line == null) return;
		OpToolsConfig.Chat cfg = config.get();
		String command = cfg.nameClickCommand == null || cfg.nameClickCommand.isBlank() ? "/msg {name} " : cfg.nameClickCommand;
		command = command.replace("{name}", line.commandName());
		boolean run = cfg.runMsgDirectly && !command.endsWith(" ");
		ClickEvent click = run ? new ClickEvent.RunCommand(command.strip()) : new ClickEvent.SuggestCommand(command);
		Component hover = Component.literal("OP Tools: ").withStyle(ChatFormatting.DARK_PURPLE)
				.append(Component.literal((run ? "Ausführen: " : "Vorbereiten: ") + command.strip()).withStyle(ChatFormatting.GRAY));
		if (line.isNick()) {
			hover = hover.copy().append(Component.literal("\nNickname – echter Name evtl. per /realname").withStyle(ChatFormatting.DARK_GRAY));
		}
		Component finalHover = hover;
		boolean hoverHints = cfg.hoverHints;
		edits.add(new ComponentRewriter.Edit(line.nameStart(), line.nameEnd(), style -> {
			var s = style.withClickEvent(click);
			return hoverHints ? s.withHoverEvent(new HoverEvent.ShowText(finalHover)) : s;
		}));
	}
}
