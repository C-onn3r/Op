package de.optools.storage.model;

/** One booking in the finance ledger. Amount is always positive; {@link #direction} decides the sign. */
public final class FinanceEntry extends SyncRecord {
	public long timestamp;
	public Direction direction = Direction.INCOME;
	public Category category = Category.OTHER;
	public double amount;
	/** Other player for payments, job name for job income. */
	public String counterparty = "";
	public String description = "";
	/** The raw server message the entry was created from (empty for aggregated entries). */
	public String raw = "";

	public double signedAmount() {
		return direction == Direction.INCOME ? amount : -amount;
	}

	public enum Direction {
		INCOME, EXPENSE
	}

	public enum Category {
		PAYMENT("Zahlung"),
		JOB("Job"),
		OTHER("Sonstiges");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}
}
