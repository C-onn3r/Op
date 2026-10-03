package de.optools.rtp;

import de.optools.opsucht.parse.RtpEvent;

import java.util.function.LongSupplier;

/** Keeps the current RTP / Biom-Teleport status. Pure Java, runs on the client thread. */
public final class RtpTracker {
	/** Final states stay visible this long before the status falls back to idle. */
	private static final long FINAL_STATE_VISIBLE_MS = 30_000;

	public enum Status {
		IDLE("Bereit"),
		QUEUED("Angemeldet"),
		COUNTDOWN("Teleport läuft"),
		TELEPORTED("Teleportiert"),
		CANCELLED("Abgebrochen"),
		COOLDOWN("Abklingzeit");

		public final String label;

		Status(String label) {
			this.label = label;
		}
	}

	public record Snapshot(Status status, String biome, int position, long since, long countdownEndsAt,
						   long cooldownEndsAt, String lastMessage) {
	}

	private final LongSupplier clock;
	private Status status = Status.IDLE;
	private String biome;
	private int position = -1;
	private long since;
	private long countdownEndsAt;
	private long cooldownEndsAt;
	private String lastMessage;
	private String lastRaw;
	private long lastRawAt;

	public RtpTracker(LongSupplier clock) {
		this.clock = clock;
	}

	public void accept(RtpEvent e) {
		long now = clock.getAsLong();
		// the same message is often shown in chat and actionbar at once
		if (e.raw().equals(lastRaw) && now - lastRawAt < 1500) return;
		lastRaw = e.raw();
		lastRawAt = now;
		lastMessage = e.raw();
		if (e.biome() != null) biome = e.biome();
		if (e.position() >= 0) position = e.position();
		switch (e.type()) {
			case QUEUED -> {
				if (status != Status.QUEUED) since = now;
				status = Status.QUEUED;
				if (e.seconds() > 0) countdownEndsAt = now + e.seconds() * 1000;
			}
			case COUNTDOWN -> {
				if (status != Status.COUNTDOWN) since = now;
				status = Status.COUNTDOWN;
				countdownEndsAt = now + Math.max(0, e.seconds()) * 1000;
			}
			case TELEPORTED -> {
				status = Status.TELEPORTED;
				since = now;
				position = -1;
			}
			case CANCELLED -> {
				status = Status.CANCELLED;
				since = now;
				position = -1;
			}
			case COOLDOWN -> {
				status = Status.COOLDOWN;
				since = now;
				cooldownEndsAt = now + Math.max(0, e.seconds()) * 1000;
			}
			case INFO -> {
				if (status == Status.IDLE) {
					status = Status.QUEUED;
					since = now;
				}
			}
		}
	}

	public void tick() {
		long now = clock.getAsLong();
		switch (status) {
			case TELEPORTED, CANCELLED -> {
				if (now - since > FINAL_STATE_VISIBLE_MS) reset();
			}
			case COOLDOWN -> {
				if (now > cooldownEndsAt) reset();
			}
			case COUNTDOWN -> {
				// no confirmation message arrived: give up after a while
				if (now > countdownEndsAt + 60_000) reset();
			}
			default -> {
			}
		}
	}

	public void reset() {
		status = Status.IDLE;
		biome = null;
		position = -1;
		countdownEndsAt = 0;
		cooldownEndsAt = 0;
		since = clock.getAsLong();
	}

	public Snapshot snapshot() {
		return new Snapshot(status, biome, position, since, countdownEndsAt, cooldownEndsAt, lastMessage);
	}
}
