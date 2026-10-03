package de.optools.chat;

import de.optools.opsucht.parse.ChatLine;

import java.util.List;

/**
 * A pluggable chat enhancement. Implementations inspect the plain line (and the detected player chat line, if any)
 * and contribute {@link ComponentRewriter.Edit}s. Register new actions in {@link ChatActionRegistry}.
 */
public interface ChatAction {
	String id();

	boolean isEnabled();

	/**
	 * @param plain    the plain chat line
	 * @param chatLine the detected player chat line, or null for system messages
	 * @param edits    output list
	 */
	void collect(String plain, ChatLine chatLine, List<ComponentRewriter.Edit> edits);
}
