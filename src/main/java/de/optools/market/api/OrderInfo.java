package de.optools.market.api;

/** One {@code orderSide} entry of {@code /market/prices}. */
public record OrderInfo(String orderSide, int activeOrders, double price) {
}
