package de.optools.opsucht.parse;

import java.util.regex.Pattern;

public final class TextUtil {
	private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-orA-FK-ORxX]");

	private TextUtil() {
	}

	/** Removes legacy § formatting codes. */
	public static String stripFormatting(String s) {
		if (s == null) return "";
		return s.indexOf('§') < 0 ? s : FORMATTING.matcher(s).replaceAll("");
	}
}
