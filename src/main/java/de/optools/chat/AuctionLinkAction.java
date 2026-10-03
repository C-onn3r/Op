package de.optools.chat;

import de.optools.config.OpToolsConfig;
import de.optools.opsucht.OpsuchtPatterns;
import de.optools.opsucht.parse.ChatLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns auction-house references in player messages ("schaut in mein /ah") into a clickable {@code /ah <Name>}.
 * An explicit target ("/ah Steve") is respected.
 */
public final class AuctionLinkAction implements ChatAction {
	private final Supplier<OpToolsConfig.Chat> config;
	private final Supplier<OpsuchtPatterns> patterns;

	public AuctionLinkAction(Supplier<OpToolsConfig.Chat> config, Supplier<OpsuchtPatterns> patterns) {
		this.config = config;
		this.patterns = patterns;
	}

	@Override
	public String id() {
		return "auction-link";
	}

	@Override
	public boolean isEnabled() {
		return config.get().auctionLinks;
	}

	@Override
	public void collect(String plain, ChatLine line, List<ComponentRewriter.Edit> edits) {
		if (line == null) return;
		Pattern pattern = patterns.get().auctionHint();
		if (pattern == null) return;
		Matcher m = pattern.matcher(plain);
		m.region(Math.min(line.messageStart(), plain.length()), plain.length());
		while (m.find()) {
			String target = null;
			try {
				target = m.group("target");
			} catch (IllegalArgumentException ignored) {
			}
			if (target == null) target = line.commandName();
			if (target.startsWith("~")) target = target.substring(1);
			String command = "/ah " + target;
			Component hover = Component.literal("OP Tools: ").withStyle(ChatFormatting.DARK_PURPLE)
					.append(Component.literal("Klicken öffnet " + command).withStyle(ChatFormatting.GRAY));
			boolean hints = config.get().hoverHints;
			edits.add(new ComponentRewriter.Edit(m.start(), m.end(), style -> {
				var s = style.withClickEvent(new ClickEvent.RunCommand(command)).withUnderlined(true);
				return hints ? s.withHoverEvent(new HoverEvent.ShowText(hover)) : s;
			}));
		}
	}
}
