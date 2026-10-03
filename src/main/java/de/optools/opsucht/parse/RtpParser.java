package de.optools.opsucht.parse;

import de.optools.opsucht.OpsuchtPatterns;
import de.optools.util.NumberParser;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Detects RTP / Biom-Teleport messages. Instead of one hard-coded sentence, a line must look RTP-related
 * ({@code rtpContext}) and is then classified by keyword patterns; biome, queue position and seconds are extracted
 * independently. All patterns live in {@link OpsuchtPatterns}.
 */
public final class RtpParser {
	private final Supplier<OpsuchtPatterns> patterns;

	public RtpParser(Supplier<OpsuchtPatterns> patterns) {
		this.patterns = patterns;
	}

	public Optional<RtpEvent> parse(String message) {
		String line = TextUtil.stripFormatting(message).strip();
		if (line.isEmpty()) return Optional.empty();
		OpsuchtPatterns p = patterns.get();
		if (!finds(p.rtpContext(), line)) return Optional.empty();

		String biome = cleanBiome(JobActionbarParser.first(p.rtpBiome(), line));
		int position = (int) parseNum(JobActionbarParser.first(p.rtpPosition(), line));

		RtpEvent.Type type;
		long seconds = -1;
		// "Du wirst in 5 Sekunden teleportiert" is a countdown, not a finished teleport
		long countdown = (long) parseNum(JobActionbarParser.first(p.rtpCountdown(), line));
		if (finds(p.rtpCancelled(), line)) {
			type = RtpEvent.Type.CANCELLED;
		} else if (countdown < 0 && finds(p.rtpTeleported(), line)) {
			type = RtpEvent.Type.TELEPORTED;
		} else if ((seconds = cooldownSeconds(p.rtpCooldown(), line)) >= 0) {
			type = RtpEvent.Type.COOLDOWN;
		} else if (finds(p.rtpQueued(), line)) {
			type = RtpEvent.Type.QUEUED;
			seconds = countdown;
		} else if (countdown >= 0) {
			type = RtpEvent.Type.COUNTDOWN;
			seconds = countdown;
		} else if (biome != null || position >= 0) {
			type = RtpEvent.Type.INFO;
		} else {
			return Optional.empty();
		}
		return Optional.of(new RtpEvent(type, biome, position, seconds, line));
	}

	private static boolean finds(Pattern pattern, String line) {
		return pattern != null && pattern.matcher(line).find();
	}

	private static double parseNum(String s) {
		if (s == null) return -1;
		double v = NumberParser.parse(s);
		return Double.isNaN(v) ? -1 : v;
	}

	private static long cooldownSeconds(Pattern pattern, String line) {
		if (pattern == null) return -1;
		var m = pattern.matcher(line);
		if (!m.find() || m.groupCount() < 2) return -1;
		double n = parseNum(m.group(1));
		if (n < 0) return -1;
		String unit = m.group(2).toLowerCase(Locale.ROOT);
		if (unit.startsWith("min")) return (long) (n * 60);
		if (unit.startsWith("std") || unit.startsWith("stunde") || unit.equals("h")) return (long) (n * 3600);
		return (long) n;
	}

	private static String cleanBiome(String biome) {
		if (biome == null) return null;
		String b = biome.strip();
		if (b.length() < 2) return null;
		String lower = b.toLowerCase(Locale.ROOT);
		// words that follow "Biom" but are not a biome name
		for (String stop : new String[]{"teleport", "auswahl", "menü", "suche", "warteschlange"}) {
			if (lower.startsWith(stop)) return null;
		}
		return b;
	}
}
