package de.optools.market.api;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entry of {@code /merchant/rates} (Rohstoffhändler). {@code source} is either a plain item id
 * ({@code diamond_block}) or an item with components ({@code minecraft:paper[custom_name=...]}).
 *
 * @param itemId      item id without components, e.g. {@code minecraft:paper}
 * @param displayName custom name extracted from the components, or a prettified item id
 * @param target      target currency, e.g. {@code opshards} or {@code redcoins}
 */
public record MerchantRate(String source, String itemId, String displayName, String target, double base,
						   double exchangeRate) {
	private static final Pattern TEXT = Pattern.compile("text:\\s*\"([^\"]+)\"");

	public static MerchantRate of(String source, String target, double base, double rate) {
		String id = source;
		int bracket = id.indexOf('[');
		if (bracket >= 0) id = id.substring(0, bracket);
		if (!id.contains(":")) id = "minecraft:" + id;
		String name = null;
		Matcher m = TEXT.matcher(source);
		while (m.find()) {
			if (!m.group(1).isBlank()) {
				name = m.group(1);
				break;
			}
		}
		if (name == null) name = prettify(id.substring(id.indexOf(':') + 1));
		return new MerchantRate(source, id, name, target, base, rate);
	}

	/** Relative change of the current rate compared to the base rate, in percent. */
	public double changePercent() {
		if (base == 0 || Double.isNaN(base) || Double.isNaN(exchangeRate)) return 0;
		return (exchangeRate - base) / base * 100.0;
	}

	public String targetLabel() {
		return switch (target.toLowerCase(Locale.ROOT)) {
			case "opshards" -> "OPShards";
			case "redcoins" -> "Redcoins";
			default -> prettify(target);
		};
	}

	public static String prettify(String id) {
		StringBuilder sb = new StringBuilder();
		for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
			if (part.isEmpty()) continue;
			if (!sb.isEmpty()) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}
}
