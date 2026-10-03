package de.optools.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display formatting (German locale, like OPSUCHT itself). */
public final class Fmt {
	private static final DecimalFormatSymbols DE = DecimalFormatSymbols.getInstance(Locale.GERMANY);
	private static final ThreadLocal<DecimalFormat> MONEY = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00", DE));
	private static final ThreadLocal<DecimalFormat> INT = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0", DE));
	private static final ThreadLocal<DecimalFormat> TWO = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.00", DE));
	private static final ThreadLocal<DecimalFormat> ONE = ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.0", DE));
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yy").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

	private Fmt() {
	}

	public static String money(double v) {
		return MONEY.get().format(v) + " $";
	}

	public static String signedMoney(double v) {
		return (v > 0 ? "+" : "") + money(v);
	}

	/** Compact form for HUD/graphs: 1,2k / 3,4 Mio. */
	public static String compact(double v) {
		double a = Math.abs(v);
		if (a >= 1e9) return ONE.get().format(v / 1e9) + " Mrd";
		if (a >= 1e6) return ONE.get().format(v / 1e6) + " Mio";
		if (a >= 1e4) return ONE.get().format(v / 1e3) + "k";
		if (a >= 100) return INT.get().format(v);
		return ONE.get().format(v);
	}

	public static String integer(double v) {
		return INT.get().format(v);
	}

	public static String decimal(double v) {
		return ONE.get().format(v);
	}

	public static String decimal2(double v) {
		return TWO.get().format(v);
	}

	public static String percent(double v) {
		return ONE.get().format(v) + " %";
	}

	public static String duration(long millis) {
		if (millis < 0) return "–";
		long totalSec = millis / 1000;
		long h = totalSec / 3600, m = (totalSec % 3600) / 60, s = totalSec % 60;
		if (h > 99) return (h / 24) + "d " + (h % 24) + "h";
		if (h > 0) return String.format(Locale.ROOT, "%dh %02dm", h, m);
		return String.format(Locale.ROOT, "%dm %02ds", m, s);
	}

	public static String dateTime(long epochMillis) {
		return DATE_TIME.format(Instant.ofEpochMilli(epochMillis));
	}

	public static String date(long epochMillis) {
		return DATE.format(Instant.ofEpochMilli(epochMillis));
	}

	public static String time(long epochMillis) {
		return TIME.format(Instant.ofEpochMilli(epochMillis));
	}
}
