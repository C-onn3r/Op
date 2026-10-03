package de.optools.storage;

import de.optools.OpTools;
import de.optools.storage.model.FinanceEntry;
import de.optools.storage.model.JobSessionRecord;
import de.optools.storage.model.SyncRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * In-memory repository for all persisted data. Feature modules read and write through this class; saving happens
 * asynchronously on a single background thread (every 30 s when dirty, and on shutdown/disconnect).
 */
public final class DataStore {
	private final DataProvider provider;
	private final List<JobSessionRecord> jobSessions = new ArrayList<>();
	private final List<FinanceEntry> financeEntries = new ArrayList<>();
	private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "OP-Tools-IO");
		t.setDaemon(true);
		return t;
	});
	private volatile boolean jobsDirty;
	private volatile boolean financeDirty;

	public DataStore(DataProvider provider) {
		this.provider = provider;
	}

	public DataProvider provider() {
		return provider;
	}

	public void load() {
		try {
			synchronized (this) {
				jobSessions.clear();
				jobSessions.addAll(provider.loadJobSessions());
				jobSessions.sort(Comparator.comparingLong(s -> s.start));
				financeEntries.clear();
				financeEntries.addAll(provider.loadFinanceEntries());
				financeEntries.sort(Comparator.comparingLong(e -> e.timestamp));
			}
			OpTools.LOG.info("OP Tools: {} Job-Sessions und {} Buchungen geladen ({})", jobSessions.size(),
					financeEntries.size(), provider.displayName());
		} catch (Exception e) {
			OpTools.LOG.error("Daten konnten nicht geladen werden", e);
		}
		io.scheduleWithFixedDelay(this::saveIfDirty, 30, 30, TimeUnit.SECONDS);
	}

	private void stamp(SyncRecord record) {
		if (record.deviceId == null || record.deviceId.isEmpty()) record.deviceId = provider.deviceId();
	}

	// ---- job sessions ----

	public synchronized List<JobSessionRecord> jobSessions() {
		return jobSessions.stream().filter(s -> !s.deleted).toList();
	}

	public synchronized void addJobSession(JobSessionRecord record, int maxSessions) {
		stamp(record);
		jobSessions.add(record);
		while (jobSessions.size() > Math.max(10, maxSessions)) jobSessions.remove(0);
		jobsDirty = true;
	}

	public synchronized void clearJobSessions() {
		jobSessions.clear();
		jobsDirty = true;
	}

	// ---- finance ----

	public synchronized List<FinanceEntry> financeEntries() {
		return financeEntries.stream().filter(e -> !e.deleted).toList();
	}

	public synchronized List<FinanceEntry> financeEntries(Predicate<FinanceEntry> filter) {
		return financeEntries.stream().filter(e -> !e.deleted).filter(filter).toList();
	}

	public synchronized void addFinanceEntry(FinanceEntry entry, int maxEntries) {
		stamp(entry);
		financeEntries.add(entry);
		while (financeEntries.size() > Math.max(100, maxEntries)) financeEntries.remove(0);
		financeDirty = true;
	}

	public synchronized void deleteFinanceEntry(String id) {
		for (FinanceEntry e : financeEntries) {
			if (e.id.equals(id)) {
				e.deleted = true;
				e.touch();
				financeDirty = true;
			}
		}
	}

	public synchronized void clearFinance() {
		financeEntries.clear();
		financeDirty = true;
	}

	// ---- persistence ----

	public void saveAsync() {
		io.execute(this::saveIfDirty);
	}

	private void saveIfDirty() {
		try {
			if (jobsDirty) {
				List<JobSessionRecord> copy;
				synchronized (this) {
					copy = new ArrayList<>(jobSessions);
					jobsDirty = false;
				}
				provider.saveJobSessions(copy);
			}
			if (financeDirty) {
				List<FinanceEntry> copy;
				synchronized (this) {
					copy = new ArrayList<>(financeEntries);
					financeDirty = false;
				}
				provider.saveFinanceEntries(copy);
			}
		} catch (Exception e) {
			OpTools.LOG.error("Speichern fehlgeschlagen", e);
		}
	}

	/** Blocking save, used on game shutdown. */
	public void shutdown() {
		io.shutdown();
		try {
			io.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
		saveIfDirty();
		provider.close();
	}
}
