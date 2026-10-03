package de.optools.hud;

import de.optools.OpTools;
import de.optools.config.ModuleId;
import de.optools.gui.Theme;
import de.optools.rtp.RtpTracker;
import de.optools.util.Fmt;

import java.util.ArrayList;
import java.util.List;

/** RTP / Biom-Teleport status. Only visible while something is going on. */
public final class RtpHudWidget extends HudWidget {
	private static final List<LineOption> OPTIONS = List.of(
			new LineOption("status", "Status", true),
			new LineOption("biome", "Biom", true),
			new LineOption("position", "Warteschlangen-Position", true),
			new LineOption("time", "Wartezeit / Countdown", true));

	@Override
	public String id() {
		return "rtp";
	}

	@Override
	public String title() {
		return "RTP / Biom-Teleport";
	}

	@Override
	public ModuleId module() {
		return ModuleId.RTP;
	}

	@Override
	public List<LineOption> lineOptions() {
		return OPTIONS;
	}

	/** Top right by default, so it never covers the job widget. */
	@Override
	protected double defaultX() {
		return 1.0;
	}

	@Override
	protected double defaultY() {
		return 0.18;
	}

	@Override
	public List<HudRow> rows(boolean preview) {
		RtpTracker.Snapshot s = OpTools.get().rtpTracker().snapshot();
		long now = System.currentTimeMillis();
		if (s.status() == RtpTracker.Status.IDLE) {
			if (!preview) return List.of();
			s = new RtpTracker.Snapshot(RtpTracker.Status.QUEUED, "Dschungel", 2, now - 42_000, 0, 0, "");
		}
		int color = switch (s.status()) {
			case TELEPORTED -> Theme.POSITIVE;
			case CANCELLED -> Theme.NEGATIVE;
			case COOLDOWN -> Theme.WARNING;
			default -> Theme.accent();
		};
		List<HudRow> rows = new ArrayList<>();
		if (shows("status")) rows.add(HudRow.text("Status", s.status().label, color));
		if (shows("biome") && s.biome() != null) rows.add(HudRow.text("Biom", s.biome(), Theme.TEXT));
		if (shows("position") && s.position() >= 0) rows.add(HudRow.text("Position", "#" + s.position(), Theme.TEXT));
		if (shows("time")) {
			switch (s.status()) {
				case COUNTDOWN -> rows.add(HudRow.text("Teleport in", Fmt.duration(Math.max(0, s.countdownEndsAt() - now)), Theme.INFO));
				case COOLDOWN -> rows.add(HudRow.text("Wieder frei in", Fmt.duration(Math.max(0, s.cooldownEndsAt() - now)), Theme.WARNING));
				default -> rows.add(HudRow.text(s.status() == RtpTracker.Status.QUEUED ? "Wartet seit" : "Vor",
						Fmt.duration(Math.max(0, now - s.since())), Theme.MUTED));
			}
		}
		return rows;
	}
}
