package de.optools.opsucht.parse;

import de.optools.opsucht.OpsuchtPatterns;
import de.optools.util.NumberParser;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects incoming and outgoing payments in system messages. Lines whose prefix is not a known system prefix
 * (e.g. "SomePlayer » Du hast ... überwiesen") are rejected to avoid booking fake messages.
 */
public final class PaymentParser {
	private final Supplier<OpsuchtPatterns> patterns;

	public PaymentParser(Supplier<OpsuchtPatterns> patterns) {
		this.patterns = patterns;
	}

	public Optional<PaymentEvent> parse(String message) {
		String line = TextUtil.stripFormatting(message).strip();
		if (line.isEmpty()) return Optional.empty();
		OpsuchtPatterns p = patterns.get();
		Optional<PaymentEvent> in = match(p, p.paymentIncoming(), line, true);
		return in.isPresent() ? in : match(p, p.paymentOutgoing(), line, false);
	}

	private static Optional<PaymentEvent> match(OpsuchtPatterns p, List<Pattern> list, String line, boolean incoming) {
		for (Pattern pattern : list) {
			Matcher m = pattern.matcher(line);
			if (!m.find()) continue;
			String prefix = JobActionbarParser.group(m, "prefix");
			if (prefix == null) prefix = JobActionbarParser.group(m, "prefix2");
			if (!p.isSystemPrefix(prefix)) continue;
			String player = JobActionbarParser.group(m, "player");
			double amount = NumberParser.parse(JobActionbarParser.group(m, "amount"));
			if (player == null || Double.isNaN(amount) || amount <= 0) continue;
			return Optional.of(new PaymentEvent(incoming, player, amount, line));
		}
		return Optional.empty();
	}
}
