package de.optools.jobs;

import de.optools.config.OpToolsConfig;
import de.optools.opsucht.parse.JobGain;
import de.optools.storage.model.JobSessionRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Session-based job tracking: accumulates parsed {@link JobGain}s, computes rates and level ETAs and hands finished
 * sessions to its listeners. Contains no Minecraft code (unit-testable); everything runs on the client thread.
 */
public final class JobTracker {
	public static final String UNKNOWN_JOB = "Job";
	private static final long MIN_RATE_WINDOW_MS = 60_000;

	public interface Listener {
		default void onGain(String job, double xp, double money, long timestamp) {
		}

		default void onSessionFinished(JobSessionRecord session) {
		}
	}

	private final Supplier<OpToolsConfig.Jobs> config;
	private final LongSupplier clock;
	private final List<Listener> listeners = new ArrayList<>();
	private final Map<String, LevelState> levels = new HashMap<>();

	private JobSessionRecord session;
	private long lastGainAt;
	private String lastRaw;
	private long lastRawAt;
	private String currentJob;
	private boolean paused;

	public JobTracker(Supplier<OpToolsConfig.Jobs> config, LongSupplier clock) {
		this.config = config;
		this.clock = clock;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	/** @return true if the gain was counted (false for duplicates / paused). */
	public boolean accept(JobGain gain) {
		long now = clock.getAsLong();
		if (paused) return false;
		OpToolsConfig.Jobs cfg = config.get();
		if (cfg.duplicateWindowMs > 0 && gain.raw().equals(lastRaw) && now - lastRawAt <= cfg.duplicateWindowMs) {
			lastRawAt = now;
			return false;
		}
		lastRaw = gain.raw();
		lastRawAt = now;
		tick();

		String job = gain.job() != null ? gain.job() : (currentJob != null ? currentJob : UNKNOWN_JOB);
		currentJob = job;
		double divisor = cfg.gainDivisor > 0 ? cfg.gainDivisor : 1.0;
		double xp = gain.xp() / divisor;
		double money = gain.money() / divisor;

		if (session == null) {
			session = new JobSessionRecord();
			session.start = now;
			session.end = now;
		} else {
			session.activeMs += Math.min(now - lastGainAt, cfg.idleThresholdSeconds * 1000L);
		}
		lastGainAt = now;
		session.end = now;

		JobSessionRecord.JobTotals totals = session.jobs.computeIfAbsent(job, k -> new JobSessionRecord.JobTotals());
		totals.xp += xp;
		totals.money += money;
		totals.gains++;
		if (gain.level() >= 0) {
			if (totals.levelStart < 0) totals.levelStart = gain.level();
			totals.levelEnd = gain.level();
		}
		if (gain.hasProgress()) totals.progressEnd = gain.progress();

		long minute = now / 60_000L;
		List<JobSessionRecord.Sample> samples = session.samples;
		JobSessionRecord.Sample sample = samples.isEmpty() ? null : samples.get(samples.size() - 1);
		if (sample == null || sample.minute != minute) {
			sample = new JobSessionRecord.Sample(minute);
			samples.add(sample);
		}
		sample.xp += xp;
		sample.money += money;

		updateLevel(job, gain, xp, now);
		for (Listener l : listeners) l.onGain(job, xp, money, now);
		return true;
	}

	private void updateLevel(String job, JobGain gain, double xp, long now) {
		if (!gain.hasProgress()) return;
		LevelState state = levels.get(job);
		if (state == null || (gain.level() >= 0 && gain.level() != state.level) || gain.progress() < state.progress - 0.001) {
			state = new LevelState();
			state.level = gain.level();
			state.startProgress = gain.progress();
			state.startTime = now;
			levels.put(job, state);
		} else {
			state.xpSinceStart += xp;
		}
		state.progress = gain.progress();
		state.lastUpdate = now;
	}

	/** Closes the session after the configured inactivity. Call regularly (client tick). */
	public void tick() {
		if (session == null) return;
		long now = clock.getAsLong();
		if (now - lastGainAt > config.get().sessionTimeoutMinutes * 60_000L) finishSession();
	}

	/** Ends the current session and stores it (if it contains anything). */
	public void finishSession() {
		if (session == null) return;
		JobSessionRecord finished = session;
		session = null;
		lastRaw = null;
		levels.clear();
		if (finished.totalGains() > 0) {
			for (Listener l : listeners) l.onSessionFinished(finished);
		}
	}

	/** Discards the running session without storing it. */
	public void discardSession() {
		session = null;
		lastRaw = null;
		levels.clear();
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public boolean isPaused() {
		return paused;
	}

	public JobSessionRecord currentSession() {
		return session;
	}

	public String currentJob() {
		return currentJob;
	}

	/** Effective duration used for hourly rates. */
	private double rateHours() {
		if (session == null) return 0;
		long active = session.activeMs;
		long sinceLast = Math.min(clock.getAsLong() - lastGainAt, config.get().idleThresholdSeconds * 1000L);
		return Math.max(active + Math.max(0, sinceLast), MIN_RATE_WINDOW_MS) / 3_600_000.0;
	}

	public Snapshot snapshot() {
		if (session == null) return Snapshot.EMPTY;
		long now = clock.getAsLong();
		double hours = rateHours();
		String job = currentJob;
		JobSessionRecord.JobTotals jobTotals = job == null ? null : session.jobs.get(job);
		LevelState level = job == null ? null : levels.get(job);

		double jobXpPerHour = jobTotals == null ? 0 : jobTotals.xp / hours;
		long eta = -1;
		double xpToNext = Double.NaN;
		if (level != null && level.progress < 100) {
			double delta = level.progress - level.startProgress;
			if (delta > 0.0001 && level.xpSinceStart > 0) {
				xpToNext = level.xpSinceStart * (100 - level.progress) / delta;
				if (jobXpPerHour > 0) eta = (long) (xpToNext / jobXpPerHour * 3_600_000.0);
			}
		}
		long activeMs = session.activeMs + Math.max(0, Math.min(now - lastGainAt, config.get().idleThresholdSeconds * 1000L));
		return new Snapshot(true, job,
				level != null ? level.level : (jobTotals != null ? jobTotals.levelEnd : -1),
				level != null ? level.progress : (jobTotals != null ? jobTotals.progressEnd : Double.NaN),
				session.totalXp(), session.totalMoney(), session.totalXp() / hours, session.totalMoney() / hours,
				jobTotals == null ? 0 : jobTotals.xp, jobTotals == null ? 0 : jobTotals.money, jobXpPerHour,
				eta, xpToNext, activeMs, now - session.start, session.totalGains(), paused);
	}

	private static final class LevelState {
		int level;
		double startProgress;
		double progress;
		long startTime;
		long lastUpdate;
		double xpSinceStart;
	}

	/** Immutable view for HUD and screens. */
	public record Snapshot(boolean active, String job, int level, double progress,
						   double sessionXp, double sessionMoney, double xpPerHour, double moneyPerHour,
						   double jobXp, double jobMoney, double jobXpPerHour,
						   long etaMs, double xpToNextLevel, long activeMs, long wallMs, int gains, boolean paused) {
		public static final Snapshot EMPTY = new Snapshot(false, null, -1, Double.NaN, 0, 0, 0, 0, 0, 0, 0, -1,
				Double.NaN, 0, 0, 0, false);
	}
}
