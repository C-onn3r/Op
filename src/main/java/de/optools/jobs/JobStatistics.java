package de.optools.jobs;

import de.optools.storage.model.JobSessionRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregations over the stored session history (plus the running session). */
public final class JobStatistics {
	private JobStatistics() {
	}

	public record JobSummary(String job, double xp, double money, int gains, long activeMs, int lastLevel) {
		public double xpPerHour() {
			return activeMs <= 0 ? 0 : xp / (activeMs / 3_600_000.0);
		}

		public double moneyPerHour() {
			return activeMs <= 0 ? 0 : money / (activeMs / 3_600_000.0);
		}
	}

	public record Totals(double xp, double money, long activeMs, int sessions, double bestXpPerHour,
						 double bestMoneyPerHour) {
	}

	public static List<JobSessionRecord> withCurrent(List<JobSessionRecord> history, JobSessionRecord current) {
		List<JobSessionRecord> all = new ArrayList<>(history);
		if (current != null) all.add(current);
		return all;
	}

	public static Totals totals(List<JobSessionRecord> sessions) {
		double xp = 0, money = 0, bestXp = 0, bestMoney = 0;
		long active = 0;
		for (JobSessionRecord s : sessions) {
			xp += s.totalXp();
			money += s.totalMoney();
			active += s.activeMs;
			if (s.activeMs >= 5 * 60_000) {
				double h = s.activeMs / 3_600_000.0;
				bestXp = Math.max(bestXp, s.totalXp() / h);
				bestMoney = Math.max(bestMoney, s.totalMoney() / h);
			}
		}
		return new Totals(xp, money, active, sessions.size(), bestXp, bestMoney);
	}

	/** Per-job totals. Active time of a session is split proportionally to the gains of each job. */
	public static List<JobSummary> perJob(List<JobSessionRecord> sessions) {
		Map<String, double[]> acc = new LinkedHashMap<>();
		Map<String, Integer> lastLevel = new LinkedHashMap<>();
		for (JobSessionRecord s : sessions) {
			int gains = Math.max(1, s.totalGains());
			for (Map.Entry<String, JobSessionRecord.JobTotals> e : s.jobs.entrySet()) {
				double[] a = acc.computeIfAbsent(e.getKey(), k -> new double[4]);
				JobSessionRecord.JobTotals t = e.getValue();
				a[0] += t.xp;
				a[1] += t.money;
				a[2] += t.gains;
				a[3] += s.activeMs * (t.gains / (double) gains);
				if (t.levelEnd >= 0) lastLevel.put(e.getKey(), t.levelEnd);
			}
		}
		List<JobSummary> out = new ArrayList<>();
		acc.forEach((job, a) -> out.add(new JobSummary(job, a[0], a[1], (int) a[2], (long) a[3],
				lastLevel.getOrDefault(job, -1))));
		out.sort((x, y) -> Double.compare(y.xp, x.xp));
		return out;
	}
}
