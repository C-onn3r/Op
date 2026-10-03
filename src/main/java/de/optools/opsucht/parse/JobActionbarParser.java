package de.optools.opsucht.parse;

import de.optools.opsucht.OpsuchtPatterns;
import de.optools.util.NumberParser;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns OPSUCHT job progress messages (actionbar) into {@link JobGain}s. */
public final class JobActionbarParser {
	private final Supplier<OpsuchtPatterns> patterns;

	public JobActionbarParser(Supplier<OpsuchtPatterns> patterns) {
		this.patterns = patterns;
	}

	public Optional<JobGain> parse(String message) {
		return parse(message, false);
	}

	/**
	 * @param strict for chat lines: additionally requires a known job name and an XP value, so ordinary chat with a
	 *               "$" amount is never mistaken for job income
	 */
	public Optional<JobGain> parse(String message, boolean strict) {
		String line = TextUtil.stripFormatting(message).strip();
		if (line.isEmpty()) return Optional.empty();
		OpsuchtPatterns p = patterns.get();
		Optional<JobGain> full = parseFullPatterns(p, line);
		return full.isPresent() ? full : parseFields(p, line, strict);
	}

	private Optional<JobGain> parseFullPatterns(OpsuchtPatterns p, String line) {
		for (Pattern pattern : p.jobActionbar()) {
			Matcher m = pattern.matcher(line);
			if (!m.find()) continue;
			double xp = num(group(m, "xp"));
			double money = num(group(m, "money"));
			if (Double.isNaN(xp) && Double.isNaN(money)) continue;
			String job = p.canonicalJob(group(m, "job"));
			if (job == null) job = p.findJobIn(line);
			String levelRaw = group(m, "level");
			int level = -1;
			if (levelRaw != null) {
				try {
					level = Integer.parseInt(levelRaw.strip());
				} catch (NumberFormatException ignored) {
				}
			}
			double progress = num(group(m, "progress"));
			if (!Double.isNaN(progress) && (progress < 0 || progress > 100)) progress = Double.NaN;
			return Optional.of(new JobGain(job, level, Double.isNaN(xp) ? 0 : xp, Double.isNaN(money) ? 0 : money,
					progress, line));
		}
		return Optional.empty();
	}

	/** Order-independent extraction: each value is searched on its own. */
	private Optional<JobGain> parseFields(OpsuchtPatterns p, String line, boolean strict) {
		double xp = num(first(p.fieldXp(), line));
		double money = num(first(p.fieldMoney(), line));
		if (Double.isNaN(xp) && Double.isNaN(money)) return Optional.empty();
		String job = p.findJobIn(line);
		String levelRaw = first(p.fieldLevel(), line);
		if (job == null && levelRaw != null) {
			String candidate = first(p.fieldNameBeforeLevel(), line);
			if (candidate != null && !candidate.equalsIgnoreCase("XP")) job = candidate;
		}
		if (job == null && levelRaw == null) return Optional.empty();
		if (strict && (p.findJobIn(line) == null || Double.isNaN(xp))) return Optional.empty();
		int level = -1;
		if (levelRaw != null) {
			try {
				level = Integer.parseInt(levelRaw.strip());
			} catch (NumberFormatException ignored) {
			}
		}
		double progress = num(first(p.fieldProgress(), line));
		if (!Double.isNaN(progress) && (progress < 0 || progress > 100)) progress = Double.NaN;
		return Optional.of(new JobGain(job, level, Double.isNaN(xp) ? 0 : xp, Double.isNaN(money) ? 0 : money, progress, line));
	}

	/** First non-null capture group of the first match, or null. */
	static String first(Pattern pattern, String line) {
		if (pattern == null) return null;
		Matcher m = pattern.matcher(line);
		if (!m.find()) return null;
		for (int i = 1; i <= m.groupCount(); i++) {
			if (m.group(i) != null) return m.group(i);
		}
		return m.groupCount() == 0 ? m.group() : null;
	}

	static String group(Matcher m, String name) {
		try {
			return m.group(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static double num(String s) {
		return s == null ? Double.NaN : NumberParser.parse(s);
	}
}
