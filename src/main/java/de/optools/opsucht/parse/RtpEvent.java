package de.optools.opsucht.parse;

/**
 * A parsed RTP / Biom-Teleport message.
 *
 * @param type     what happened
 * @param biome    biome name if the message contains one, else null
 * @param position queue position or -1
 * @param seconds  countdown/cooldown in seconds or -1
 * @param raw      plain message
 */
public record RtpEvent(Type type, String biome, int position, long seconds, String raw) {
	public enum Type {
		/** Signed up / added to a biome queue or search. */
		QUEUED,
		/** Teleport starts in n seconds. */
		COUNTDOWN,
		TELEPORTED,
		CANCELLED,
		COOLDOWN,
		/** RTP-related message without a clear state change (e.g. queue position update). */
		INFO
	}
}
