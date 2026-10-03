package de.optools.gui.screen.tabs;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.BarChart;
import de.optools.gui.widget.LineChart;
import de.optools.gui.widget.ScrollList;
import de.optools.gui.widget.UiChips;
import de.optools.jobs.JobStatistics;
import de.optools.storage.model.JobSessionRecord;
import de.optools.util.Fmt;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JobsTab extends TabView {
	private static int metric; // 0 = XP, 1 = Geld
	private LineChart sessionChart;
	private BarChart historyChart;
	private ScrollList<JobSessionRecord> sessionList;
	private ScrollList<JobStatistics.JobSummary> jobList;
	private JobStatistics.Totals totals;
	private long lastUpdate;

	public JobsTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		UiChips chips = add(new UiChips(List.of("XP", "Geld"), metric, i -> {
			metric = i;
			lastUpdate = 0;
		}));
		chips.bounds(x + w - 90, y - 1, 90, 13);
		chips.layout();
		int chipW = 0;
		for (String s : List.of("XP", "Geld")) chipW += UiDraw.font().width(s) + 16;
		chips.bounds(x + w - chipW, y - 1, chipW, 13);
		chips.layout();

		int top = y + 62;
		int chartH = Math.max(70, Math.min(110, (h - 80) / 2));
		int half = col(w, 2, 6);
		sessionChart = add(new LineChart("Aktuelle / letzte Session (kumuliert)")).color(Theme.accent());
		sessionChart.bounds(x, top, half, chartH);
		historyChart = add(new BarChart("Letzte Sessions pro Stunde"));
		historyChart.bounds(x + half + 6, top, half, chartH);

		int listTop = top + chartH + 6;
		int listH = y + h - listTop;
		int leftW = (int) (w * 0.58);
		sessionList = add(new ScrollList<JobSessionRecord>(12, (g, s, rx, ry, rw, rh, hover, mx, my) -> {
			double hours = Math.max(1, s.activeMs) / 3_600_000.0;
			UiDraw.text(g, Fmt.dateTime(s.start), rx + 4, ry + 2, Theme.MUTED);
			UiDraw.text(g, Fmt.duration(s.activeMs), rx + 64, ry + 2, Theme.TEXT);
			UiDraw.textRight(g, Fmt.compact(s.totalXp()), rx + rw - 120, ry + 2, Theme.TEXT);
			UiDraw.textRight(g, Fmt.compact(s.totalMoney()) + " $", rx + rw - 62, ry + 2, Theme.POSITIVE);
			UiDraw.textRight(g, Fmt.compact(metric == 0 ? s.totalXp() / hours : s.totalMoney() / hours), rx + rw - 4, ry + 2, Theme.accent());
			if (hover) UiDraw.tooltip(g, sessionTooltip(s), mx, my);
		})).header(13, g -> {
			int hy = y + listTopOffset() + 3;
			UiDraw.text(g, "Session", x + 5, hy, Theme.FAINT);
			UiDraw.text(g, "Dauer", x + 65, hy, Theme.FAINT);
			UiDraw.textRight(g, "XP", x + leftW - 121, hy, Theme.FAINT);
			UiDraw.textRight(g, "Geld", x + leftW - 63, hy, Theme.FAINT);
			UiDraw.textRight(g, metric == 0 ? "XP/h" : "$/h", x + leftW - 5, hy, Theme.FAINT);
		}).emptyText("Noch keine abgeschlossenen Sessions");
		sessionList.bounds(x, listTop, leftW, listH);

		int rightX = x + leftW + 6, rightW = w - leftW - 6;
		jobList = add(new ScrollList<JobStatistics.JobSummary>(12, (g, j, rx, ry, rw, rh, hover, mx, my) -> {
			UiDraw.text(g, UiDraw.ellipsize(j.job() + (j.lastLevel() >= 0 ? " L" + j.lastLevel() : ""), rw - 70), rx + 4, ry + 2, Theme.TEXT);
			UiDraw.textRight(g, metric == 0 ? Fmt.compact(j.xp()) : Fmt.compact(j.money()) + " $", rx + rw - 4, ry + 2,
					metric == 0 ? Theme.accent() : Theme.POSITIVE);
			if (hover) UiDraw.tooltip(g, List.of(j.job(), "XP: " + Fmt.integer(j.xp()), "Geld: " + Fmt.money(j.money()),
					"Aktionen: " + Fmt.integer(j.gains()), "Ø XP/h: " + Fmt.compact(j.xpPerHour()),
					"Ø $/h: " + Fmt.compact(j.moneyPerHour())), mx, my);
		})).header(13, g -> UiDraw.text(g, "Jobs gesamt", rightX + 5, y + listTopOffset() + 3, Theme.FAINT))
				.emptyText("Noch keine Jobdaten");
		jobList.bounds(rightX, listTop, rightW, listH);
		update();
	}

	private int listTopOffset() {
		int chartH = Math.max(70, Math.min(110, (h - 80) / 2));
		return 62 + chartH + 6;
	}

	private List<String> sessionTooltip(JobSessionRecord s) {
		List<String> lines = new ArrayList<>();
		lines.add(Fmt.dateTime(s.start) + " – " + Fmt.time(s.end));
		s.jobs.forEach((job, t) -> lines.add(job + ": " + Fmt.compact(t.xp) + " XP, " + Fmt.compact(t.money) + " $"
				+ (t.levelStart >= 0 ? " (L" + t.levelStart + (t.levelEnd != t.levelStart ? "→" + t.levelEnd : "") + ")" : "")));
		return lines;
	}

	@Override
	public void tick() {
		if (System.currentTimeMillis() - lastUpdate > 1000) update();
	}

	private void update() {
		lastUpdate = System.currentTimeMillis();
		List<JobSessionRecord> history = mod.dataStore().jobSessions();
		JobSessionRecord current = mod.jobTracker().currentSession();
		List<JobSessionRecord> all = JobStatistics.withCurrent(history, current);
		totals = JobStatistics.totals(all);

		JobSessionRecord chartSession = current != null ? current : (history.isEmpty() ? null : history.get(history.size() - 1));
		List<double[]> pts = new ArrayList<>();
		if (chartSession != null && !chartSession.samples.isEmpty()) {
			double sum = 0;
			long firstMinute = chartSession.samples.get(0).minute;
			pts.add(new double[]{firstMinute - 1, 0});
			for (JobSessionRecord.Sample sample : chartSession.samples) {
				sum += metric == 0 ? sample.xp : sample.money;
				pts.add(new double[]{sample.minute, sum});
			}
		}
		sessionChart.data(pts).format(v -> Fmt.time((long) v * 60_000L), v -> Fmt.compact(v) + (metric == 1 ? " $" : ""))
				.color(metric == 0 ? Theme.accent() : Theme.POSITIVE)
				.emptyText("Starte einen Job auf OPSUCHT, um Daten zu sehen");

		List<BarChart.Bar> bars = new ArrayList<>();
		int from = Math.max(0, history.size() - 15);
		for (JobSessionRecord s : history.subList(from, history.size())) {
			double hours = Math.max(60_000, s.activeMs) / 3_600_000.0;
			bars.add(new BarChart.Bar(Fmt.dateTime(s.start), new double[]{metric == 0 ? s.totalXp() / hours : s.totalMoney() / hours}));
		}
		historyChart.data(bars).series(new String[]{metric == 0 ? "XP/h" : "$/h"},
				new int[]{metric == 0 ? Theme.accent() : Theme.POSITIVE}).format(Fmt::compact);

		List<JobSessionRecord> reversed = new ArrayList<>(history);
		Collections.reverse(reversed);
		sessionList.items(reversed);
		jobList.items(JobStatistics.perJob(all));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		title(g, "Job-Tracker", mod.jobTracker().isPaused() ? "pausiert" : null);
		if (totals == null) return;
		int top = y + 16;
		int cw = col(w, 4, 6);
		stat(g, x, top, cw, 40, "XP gesamt", Fmt.compact(totals.xp()), Theme.TEXT, totals.sessions() + " Sessions");
		stat(g, x + cw + 6, top, cw, 40, "Geld gesamt", Fmt.compact(totals.money()) + " $", Theme.POSITIVE, null);
		stat(g, x + (cw + 6) * 2, top, cw, 40, "Aktive Zeit", Fmt.duration(totals.activeMs()), Theme.TEXT, null);
		stat(g, x + (cw + 6) * 3, top, cw, 40, metric == 0 ? "Beste XP/h" : "Beste $/h",
				Fmt.compact(metric == 0 ? totals.bestXpPerHour() : totals.bestMoneyPerHour()), Theme.accent(), "Sessions ≥ 5 min");
	}
}
