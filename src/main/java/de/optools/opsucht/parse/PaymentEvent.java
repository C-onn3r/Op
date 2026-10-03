package de.optools.opsucht.parse;

/** A detected money transfer between the player and somebody else. */
public record PaymentEvent(boolean incoming, String player, double amount, String raw) {
}
