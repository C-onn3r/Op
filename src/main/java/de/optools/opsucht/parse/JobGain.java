package de.optools.opsucht.parse;

/**
 * One parsed job reward. Missing values are {@code -1} (level), {@link Double#NaN} (progress) or {@code 0}
 * (xp/money).
 *
 * @param job      canonical job name, or {@code null} if the message did not contain one
 * @param level    current job level or -1
 * @param xp       XP gained by this action
 * @param money    money gained by this action
 * @param progress progress towards the next level in percent, or NaN
 * @param raw      the plain message (used for de-duplication)
 */
public record JobGain(String job, int level, double xp, double money, double progress, String raw) {
	public boolean hasProgress() {
		return !Double.isNaN(progress);
	}
}
