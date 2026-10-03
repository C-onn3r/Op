package de.optools.util;

import java.util.Locale;

/**
 * Parses amounts as they appear in OPSUCHT messages, e.g. {@code 1.234,56}, {@code 1,234.56},
 * {@code 12,5}, {@code 2.5k}, {@code 3 Mio}. Pure Java, no Minecraft dependencies.
 */
public final class NumberParser {
	private NumberParser() {
	}

	/** @return parsed value or {@link Double#NaN} if nothing sensible could be parsed. */
	public static double parse(String raw) {
		if (raw == null) return Double.NaN;
		String s = raw.trim().replace("$", "").replace(" ", "").replace(" ", "").replace("'", "");
		if (s.startsWith("+")) s = s.substring(1);
		if (s.isEmpty()) return Double.NaN;

		double multiplier = 1;
		String lower = s.toLowerCase(Locale.ROOT);
		String[][] suffixes = {{"mrd", "1e9"}, {"mio", "1e6"}, {"b", "1e9"}, {"m", "1e6"}, {"k", "1e3"}};
		for (String[] suffix : suffixes) {
			if (lower.endsWith(suffix[0])) {
				multiplier = Double.parseDouble(suffix[1]);
				s = s.substring(0, s.length() - suffix[0].length());
				break;
			}
		}
		if (s.isEmpty()) return Double.NaN;

		boolean negative = s.startsWith("-");
		if (negative) s = s.substring(1);

		int lastDot = s.lastIndexOf('.');
		int lastComma = s.lastIndexOf(',');
		String normalized;
		if (lastDot >= 0 && lastComma >= 0) {
			// Both present: the later one is the decimal separator.
			if (lastComma > lastDot) {
				normalized = s.replace(".", "").replace(',', '.');
			} else {
				normalized = s.replace(",", "");
			}
		} else if (lastComma >= 0) {
			normalized = isThousandsGrouping(s, ',') ? s.replace(",", "") : s.replace(',', '.');
		} else if (lastDot >= 0) {
			normalized = isThousandsGrouping(s, '.') ? s.replace(".", "") : s;
		} else {
			normalized = s;
		}
		try {
			double value = Double.parseDouble(normalized) * multiplier;
			return negative ? -value : value;
		} catch (NumberFormatException e) {
			return Double.NaN;
		}
	}

	/**
	 * A separator is treated as thousands grouping if it occurs more than once, or once with exactly three digits
	 * after it (German style, which OPSUCHT uses: {@code 1.000} = one thousand).
	 */
	private static boolean isThousandsGrouping(String s, char sep) {
		int count = 0;
		for (int i = 0; i < s.length(); i++) if (s.charAt(i) == sep) count++;
		if (count > 1) return true;
		int idx = s.indexOf(sep);
		return s.length() - idx - 1 == 3 && idx > 0;
	}
}
