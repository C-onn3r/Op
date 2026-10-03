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
		String line = TextUtil.stripFormatting(message).strip();
		if (line.isEmpty()) return Optional.empty();
		OpsuchtPatterns p = patterns.get();
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
