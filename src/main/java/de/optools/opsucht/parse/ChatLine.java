package de.optools.opsucht.parse;

/**
 * A chat line written by a player.
 *
 * @param name         the sender name as displayed (may be a nick starting with "~")
 * @param nameStart    start index of the name in the plain line
 * @param nameEnd      end index (exclusive)
 * @param messageStart start index of the message content
 */
public record ChatLine(String name, int nameStart, int nameEnd, int messageStart, String plain) {
	public String message() {
		return plain.substring(Math.min(messageStart, plain.length()));
	}

	/** Name usable in commands. Nicknames ("~Nick") are passed without the tilde. */
	public String commandName() {
		return name.startsWith("~") ? name.substring(1) : name;
	}

	public boolean isNick() {
		return name.startsWith("~");
	}
}
