package de.optools.market.api;

/** One bucket of {@code /market/history/{material}} (marketplace trades, not auction house). */
public record HistoryPoint(double avgPrice, double minPrice, double maxPrice, long items, long transactions,
						   String timestamp) {
}
