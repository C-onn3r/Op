package de.optools.storage.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A finished (or currently running) job-tracking session. */
public final class JobSessionRecord extends SyncRecord {
	public long start;
	public long end;
	/** Time with actual job activity (gaps above the idle threshold are excluded). */
	public long activeMs;
	public Map<String, JobTotals> jobs = new LinkedHashMap<>();
	/** One sample per active minute, used for the graphs. */
	public List<Sample> samples = new ArrayList<>();

	public double totalXp() {
		return jobs.values().stream().mapToDouble(j -> j.xp).sum();
	}

	public double totalMoney() {
		return jobs.values().stream().mapToDouble(j -> j.money).sum();
	}

	public int totalGains() {
		return jobs.values().stream().mapToInt(j -> j.gains).sum();
	}

	public static final class JobTotals {
		public double xp;
		public double money;
		public int gains;
		public int levelStart = -1;
		public int levelEnd = -1;
		public double progressEnd = -1;
	}

	/** Aggregated activity of one wall-clock minute. */
	public static final class Sample {
		public long minute;
		public double xp;
		public double money;

		public Sample() {
		}

		public Sample(long minute) {
			this.minute = minute;
		}
	}
}
