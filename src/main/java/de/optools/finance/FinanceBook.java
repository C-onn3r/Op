package de.optools.finance;

import de.optools.config.OpToolsConfig;
import de.optools.jobs.JobTracker;
import de.optools.opsucht.parse.PaymentEvent;
import de.optools.storage.DataStore;
import de.optools.storage.model.FinanceEntry;
import de.optools.storage.model.JobSessionRecord;
import de.optools.util.Fmt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The finance ledger: books payments and (aggregated) job income into the {@link DataStore} and provides the
 * aggregations the UI needs.
 */
public final class FinanceBook implements JobTracker.Listener {
	private final DataStore store;
	private final Supplier<OpToolsConfig> config;
	private final Map<String, PendingJobIncome> pendingJobIncome = new LinkedHashMap<>();

	public FinanceBook(DataStore store, Supplier<OpToolsConfig> config) {
		this.store = store;
		this.config = config;
	}

	public void onPayment(PaymentEvent event) {
		if (!config.get().finance.trackPayments) return;
		FinanceEntry e = new FinanceEntry();
		e.timestamp = System.currentTimeMillis();
		e.direction = event.incoming() ? FinanceEntry.Direction.INCOME : FinanceEntry.Direction.EXPENSE;
		e.category = FinanceEntry.Category.PAYMENT;
		e.amount = event.amount();
		e.counterparty = event.player();
		e.description = event.incoming() ? "Zahlung von " + event.player() : "Zahlung an " + event.player();
		e.raw = event.raw();
		store.addFinanceEntry(e, config.get().finance.maxEntries);
	}

	// ---- job income (aggregated) ----

	@Override
	public void onGain(String job, double xp, double money, long timestamp) {
		if (!config.get().modules.finance || !config.get().finance.trackJobIncome || money <= 0) return;
		PendingJobIncome pending = pendingJobIncome.get(job);
		long window = Math.max(1, config.get().finance.jobIncomeAggregationMinutes) * 60_000L;
		if (pending != null && timestamp - pending.start >= window) {
			book(job, pending);
			pending = null;
		}
		if (pending == null) {
			pending = new PendingJobIncome(timestamp);
			pendingJobIncome.put(job, pending);
		}
		pending.amount += money;
		pending.actions++;
		pending.last = timestamp;
	}

	@Override
	public void onSessionFinished(JobSessionRecord session) {
		flushJobIncome();
	}

	/** Books all pending job income (on session end, disconnect, shutdown). */
	public void flushJobIncome() {
		pendingJobIncome.forEach(this::book);
		pendingJobIncome.clear();
	}

	private void book(String job, PendingJobIncome pending) {
		if (pending.amount <= 0) return;
		FinanceEntry e = new FinanceEntry();
		e.timestamp = pending.last;
		e.direction = FinanceEntry.Direction.INCOME;
		e.category = FinanceEntry.Category.JOB;
		e.amount = pending.amount;
		e.counterparty = job;
		e.description = job + " · " + pending.actions + " Aktionen (" + Fmt.time(pending.start) + "–" + Fmt.time(pending.last) + ")";
		store.addFinanceEntry(e, config.get().finance.maxEntries);
	}

	/** Pending, not yet booked job income (shown as "läuft" in the UI). */
	public double pendingJobIncome() {
		return pendingJobIncome.values().stream().mapToDouble(p -> p.amount).sum();
	}

	// ---- aggregations ----

	public List<FinanceEntry> entries() {
		return store.financeEntries();
	}

	public record Summary(double income, double expense, double net, int count) {
	}

	public Summary summary(long sinceMillis) {
		double in = 0, out = 0;
		int count = 0;
		for (FinanceEntry e : store.financeEntries(x -> x.timestamp >= sinceMillis)) {
			if (e.direction == FinanceEntry.Direction.INCOME) in += e.amount;
			else out += e.amount;
			count++;
		}
		return new Summary(in, out, in - out, count);
	}

	public record Day(LocalDate date, double income, double expense) {
	}

	/** Income/expense per day for the last {@code days} days (oldest first). */
	public List<Day> daily(int days) {
		ZoneId zone = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zone);
		LocalDate first = today.minusDays(days - 1L);
		Map<LocalDate, double[]> map = new LinkedHashMap<>();
		for (int i = 0; i < days; i++) map.put(first.plusDays(i), new double[2]);
		long since = first.atStartOfDay(zone).toInstant().toEpochMilli();
		for (FinanceEntry e : store.financeEntries(x -> x.timestamp >= since)) {
			LocalDate d = Instant.ofEpochMilli(e.timestamp).atZone(zone).toLocalDate();
			double[] v = map.get(d);
			if (v == null) continue;
			if (e.direction == FinanceEntry.Direction.INCOME) v[0] += e.amount;
			else v[1] += e.amount;
		}
		List<Day> out = new ArrayList<>();
		map.forEach((d, v) -> out.add(new Day(d, v[0], v[1])));
		return out;
	}

	/** Cumulative net development as (timestamp, value) points. */
	public List<double[]> cumulativeNet(long sinceMillis) {
		List<double[]> points = new ArrayList<>();
		double sum = 0;
		for (FinanceEntry e : store.financeEntries(x -> x.timestamp >= sinceMillis)) {
			sum += e.signedAmount();
			points.add(new double[]{e.timestamp, sum});
		}
		return points;
	}

	private static final class PendingJobIncome {
		final long start;
		long last;
		double amount;
		int actions;

		PendingJobIncome(long start) {
			this.start = start;
			this.last = start;
		}
	}
}
