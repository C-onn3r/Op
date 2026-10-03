package de.optools.chat;

import de.optools.OpTools;
import de.optools.opsucht.parse.ChatLine;
import de.optools.opsucht.parse.ChatLineParser;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Holds all {@link ChatAction}s and applies them to incoming chat lines. */
public final class ChatActionRegistry {
	private final List<ChatAction> actions = new ArrayList<>();
	private final ChatLineParser chatLineParser;
	private final BooleanSupplier active;

	public ChatActionRegistry(ChatLineParser chatLineParser, BooleanSupplier active) {
		this.chatLineParser = chatLineParser;
		this.active = active;
	}

	public void register(ChatAction action) {
		actions.add(action);
	}

	public List<ChatAction> actions() {
		return List.copyOf(actions);
	}

	public Component process(Component message) {
		if (message == null || !active.getAsBoolean()) return message;
		try {
			ComponentRewriter rewriter = new ComponentRewriter(message);
			String plain = rewriter.plain();
			ChatLine line = chatLineParser.parse(plain).orElse(null);
			List<ComponentRewriter.Edit> edits = new ArrayList<>();
			for (ChatAction action : actions) {
				if (action.isEnabled()) action.collect(plain, line, edits);
			}
			return edits.isEmpty() ? message : rewriter.apply(edits);
		} catch (RuntimeException e) {
			OpTools.LOG.debug("Chat-Aktion fehlgeschlagen", e);
			return message;
		}
	}
}
