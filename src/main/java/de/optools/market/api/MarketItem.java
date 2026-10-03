package de.optools.market.api;

/**
 * Current marketplace state of one material as returned by {@code /market/prices}.
 *
 * @param buy  entry with {@code orderSide = "BUY"} (may be null)
 * @param sell entry with {@code orderSide = "SELL"} (may be null)
 */
public record MarketItem(String material, String category, OrderInfo buy, OrderInfo sell) {
}
