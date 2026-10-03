package de.optools;

import de.optools.config.OpToolsConfig;
import de.optools.jobs.JobTracker;
import de.optools.opsucht.parse.JobGain;
import de.optools.storage.model.JobSessionRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobTrackerTest {
	private long now = 1_700_000_000_000L;
	private final OpToolsConfig.Jobs cfg = new OpToolsConfig.Jobs();
	private final JobTracker tracker = new JobTracker(() -> cfg, () -> now);

	private JobGain gain(int i, double progress) {
		return new JobGain("Holzfäller", 10, 10, 2, progress, "raw " + i);
	}

	@Test
	void ratesAndEta() {
		cfg.idleThresholdSeconds = 20; // gains every 10 s are active time
		for (int i = 0; i < 61; i++) {
			tracker.accept(gain(i, 10 + i * 0.5));
			now += 10_000; // one gain every 10 s
		}
		now -= 10_000;
		JobTracker.Snapshot s = tracker.snapshot();
		assertTrue(s.active());
		assertEquals(610, s.sessionXp(), 1e-6);
		assertEquals(122, s.sessionMoney(), 1e-6);
		// 610 XP (incl. first gain) in 600 s active => 3660 XP/h
		assertEquals(3660, s.xpPerHour(), 1.0);
		// progress 10% -> 40% for 600 XP => 20 XP per %, 60% left => 1200 XP at 3660 XP/h
		assertEquals(1200, s.xpToNextLevel(), 1.0);
		assertEquals(1200 / 3660.0 * 3_600_000, s.etaMs(), 2000);
	}

	@Test
	void afkTimeIsNotCounted() {
		cfg.idleThresholdSeconds = 4;
		tracker.accept(gain(1, 1));
		now += 2_000;
		tracker.accept(gain(2, 1));
		assertFalse(tracker.isIdle());
		now += 60_000; // AFK for a minute
		assertTrue(tracker.isIdle());
		assertEquals(6_000, tracker.snapshot().activeMs()); // 2 s + max. 4 s of the break
		tracker.accept(gain(3, 1));
		assertFalse(tracker.isIdle());
		assertEquals(6_000, tracker.snapshot().activeMs());
	}

	@Test
	void timberBurstCountsEveryPacket() {
		// Timber axe: 17 identical actionbars in the same tick, each one is a paid block
		JobGain g = new JobGain("Holzfäller", 58, 2.5, 12.73, 13.85, "+2,5 XP • +12,73$ • Holzfäller Level 58 • [|] 13,85%");
		for (int i = 0; i < 17; i++) assertTrue(tracker.accept(g));
		assertEquals(17 * 2.5, tracker.snapshot().sessionXp(), 1e-6);
		assertEquals(17 * 12.73, tracker.snapshot().sessionMoney(), 1e-6);
	}

	@Test
	void optionalDuplicateWindow() {
		cfg.duplicateWindowMs = 600;
		JobGain g = gain(1, 5);
		assertTrue(tracker.accept(g));
		now += 100;
		assertFalse(tracker.accept(g));
		now += 5_000;
		assertTrue(tracker.accept(g));
	}

	@Test
	void sessionTimeoutStoresSession() {
		List<JobSessionRecord> finished = new ArrayList<>();
		tracker.addListener(new JobTracker.Listener() {
			@Override
			public void onSessionFinished(JobSessionRecord session) {
				finished.add(session);
			}
		});
		tracker.accept(gain(1, 1));
		now += 1000;
		tracker.accept(gain(2, 2));
		now += cfg.sessionTimeoutMinutes * 60_000L + 1;
		tracker.tick();
		assertEquals(1, finished.size());
		assertEquals(2, finished.get(0).totalGains());
		assertNull(tracker.currentSession());
	}

	@Test
	void idleTimeIsNotCounted() {
		tracker.accept(gain(1, 1));
		now += 10 * 60_000; // 10 min AFK
		tracker.accept(gain(2, 2));
		assertEquals(cfg.idleThresholdSeconds * 1000L, tracker.currentSession().activeMs);
	}
}
