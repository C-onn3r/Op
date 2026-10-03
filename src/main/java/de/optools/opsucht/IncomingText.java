package de.optools.opsucht;

/** The client paths on which server texts arrive; all of them are routed through {@code OpTools#onIncoming}. */
public final class IncomingText {
	private IncomingText() {
	}

	public enum Source {
		ACTIONBAR,
		TITLE,
		SUBTITLE,
		BOSSBAR,
		SYSTEM_CHAT
	}
}
