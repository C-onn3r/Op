package de.optools.hud;

import de.optools.OpTools;
import de.optools.config.ModuleId;
import de.optools.gui.Theme;
import de.optools.jobs.JobTracker;
import de.optools.util.Fmt;

import java.util.ArrayList;
import java.util.List;

/** The job tracker HUD widget. */
public final class JobHudWidget extends HudWidget {
	public static final String JOB = "job";
	public static final String PROGRESS = "progress";
	public static final String SESSION_XP = "session_xp";
	public static final String SESSION_MONEY = "session_money";
	public static final String XP_PER_HOUR = "xp_per_hour";
	public static final String MONEY_PER_HOUR = "money_per_hour";
	public static final String ETA = "eta";
	public static final String XP_TO_NEXT = "xp_to_next";
	public static final String SESSION_TIME = "session_time";

	private static final List<LineOption> OPTIONS = List.of(
			new LineOption(JOB, "Job & Level", true),
			new LineOption(PROGRESS, "Level-Fortschritt", true),
			new LineOption(SESSION_XP, "Session-XP", true),
			new LineOption(SESSION_MONEY, "Session-Geld", true),
			new LineOption(XP_PER_HOUR, "XP pro Stunde", true),
			new LineOption(MONEY_PER_HOUR, "Geld pro Stunde", true),
			new LineOption(ETA, "ETA nächstes Level", true),
			new LineOption(XP_TO_NEXT, "XP bis Level (geschätzt)", false),
			new LineOption(SESSION_TIME, "Aktive Sessionzeit", true));

	@Override
	public String id() {
		return "jobs";
	}

	@Override
	public String title() {
		return "Job-Tracker";
	}

	@Override
	public ModuleId module() {
		return ModuleId.JOB_TRACKER;
	}

	@Override
	public List<LineOption> lineOptions() {
		return OPTIONS;
	}

	@Override
	public String headerStatus() {
		JobTracker tracker = OpTools.get().jobTracker();
		if (tracker.isPaused()) return "pausiert";
		return tracker.isIdle() ? "AFK" : null;
	}

	@Override
	public List<HudRow> rows(boolean preview) {
		JobTracker.Snapshot s = OpTools.get().jobTracker().snapshot();
		if (!s.active()) {
			if (!preview) return List.of();
			s = new JobTracker.Snapshot(true, "Holzfäller", 42, 63.5, 18_250, 9_420.5, 21_900, 11_304, 18_250,
					9_420.5, 21_900, 23 * 60_000L, 8_400, 50 * 60_000L, 55 * 60_000L, 1234, false);
		}
		List<HudRow> rows = new ArrayList<>();
		int accent = Theme.accent();
		if (shows(JOB)) {
			String job = s.job() == null ? JobTracker.UNKNOWN_JOB : s.job();
			rows.add(HudRow.text(job, s.level() >= 0 ? "Level " + s.level() : "", accent));
		}
		if (shows(PROGRESS) && !Double.isNaN(s.progress())) {
			rows.add(HudRow.bar(Fmt.percent(s.progress()), s.progress() / 100.0));
		}
		if (shows(SESSION_XP)) rows.add(HudRow.text("XP", "+" + Fmt.num(s.sessionXp()), Theme.TEXT));
		if (shows(SESSION_MONEY)) rows.add(HudRow.text("Geld", "+" + Fmt.cash(s.sessionMoney()), Theme.POSITIVE));
		if (shows(XP_PER_HOUR)) rows.add(HudRow.text("XP/h", Fmt.num(s.xpPerHour()), Theme.TEXT));
		if (shows(MONEY_PER_HOUR)) rows.add(HudRow.text("$/h", Fmt.cash(s.moneyPerHour()), Theme.POSITIVE));
		if (shows(ETA)) rows.add(HudRow.text("Level-Up", s.etaMs() >= 0 ? "~" + Fmt.duration(s.etaMs()) : "–", Theme.INFO));
		if (shows(XP_TO_NEXT)) {
			rows.add(HudRow.text("Rest-XP", Double.isNaN(s.xpToNextLevel()) ? "–" : "~" + Fmt.num(s.xpToNextLevel()), Theme.TEXT));
		}
		if (shows(SESSION_TIME)) rows.add(HudRow.text("Zeit", Fmt.duration(s.activeMs()), Theme.MUTED));
		return rows;
	}
}
