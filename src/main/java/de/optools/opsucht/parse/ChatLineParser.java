package de.optools.opsucht.parse;

import de.optools.opsucht.OpsuchtPatterns;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects chat lines written by players (as opposed to system messages). Works on the plain, unformatted line. */
public final class ChatLineParser {
	private final Supplier<OpsuchtPatterns> patterns;

	public ChatLineParser(Supplier<OpsuchtPatterns> patterns) {
		this.patterns = patterns;
	}

	public Optional<ChatLine> parse(String plain) {
		if (plain == null || plain.isEmpty()) return Optional.empty();
		OpsuchtPatterns p = patterns.get();
		for (Pattern pattern : p.playerChat()) {
			Matcher m = pattern.matcher(plain);
			if (!m.find()) continue;
			String name = JobActionbarParser.group(m, "name");
			if (name == null) continue;
			if (p.isSystemPrefix(name)) return Optional.empty();
			int messageStart = m.end("name");
			try {
				if (m.group("message") != null) messageStart = m.start("message");
			} catch (IllegalArgumentException ignored) {
			}
			return Optional.of(new ChatLine(name, m.start("name"), m.end("name"), messageStart, plain));
		}
		return Optional.empty();
	}
}
