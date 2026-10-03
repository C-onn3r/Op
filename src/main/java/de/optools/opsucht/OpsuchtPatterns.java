package de.optools.opsucht;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <b>The single place for OPSUCHT-specific texts.</b>
 *
 * <p>Every server message format the mod reacts to is defined here as a regular expression with named groups.
 * The defaults are written to {@code config/optools/opsucht-patterns.json}; if OPSUCHT changes a message, the
 * pattern can be fixed there (and reloaded with {@code /optools reload}) without a new mod release. When the
 * defaults change in a newer mod version and the user did not mark the file as {@code customized}, the file is
 * replaced automatically.</p>
 *
 * <p>Named groups used:</p>
 * <ul>
 *   <li>job actionbar: {@code job} (optional), {@code level}, {@code xp}, {@code money}, {@code progress} (all optional
 *   except at least one of xp/money)</li>
 *   <li>payments: {@code player}, {@code amount}, optional {@code prefix} (checked against {@link #systemPrefixes})</li>
 *   <li>player chat: {@code name}, {@code message}</li>
 *   <li>auction hint: optional {@code target}</li>
 * </ul>
 */
public final class OpsuchtPatterns {
	/** Bump when the defaults below change. */
	public static final int DEFAULTS_VERSION = 3;

	private static final String NAME = "~?[A-Za-z0-9_]{2,16}";
	private static final String AMOUNT = "[0-9][0-9.,]*(?:\\s?(?:k|K|Mio\\.?|M|Mrd\\.?))?";
	/** Optional system prefix like "OPSUCHT » " or "[Bank] ". */
	private static final String PREFIX = "^(?:\\[?(?<prefix>[^»:\\]]{1,32}?)\\]?\\s*[»:|]\\s*|\\[(?<prefix2>[^\\]]{1,32})\\]\\s*)?";

	public int version = DEFAULTS_VERSION;
	/** Set to true to keep this file even if a mod update ships new defaults. */
	public boolean customized = false;

	/** Server addresses that count as OPSUCHT (substring match on the address you joined). */
	public List<String> serverAddresses = new ArrayList<>(List.of("opsucht.net", "opsucht.de"));

	/** Job names as shown by OPSUCHT (wiki: Minenarbeiter, Holzfäller, Gräber, Jäger, Angler, Farmer, Builder). */
	public List<String> jobNames = new ArrayList<>(List.of(
			"Minenarbeiter", "Holzfäller", "Gräber", "Jäger", "Angler", "Fischer", "Farmer", "Builder"));

	/**
	 * Job progress display (actionbar). The first default follows the format documented for the original OPMOD
	 * ("Level X • XP: Y • $Z • P%"), the second is a more tolerant variant ("+Y XP", "+Z $").
	 */
	public List<String> jobActionbar = new ArrayList<>(List.of(
			"(?<job>\\p{L}+)?[^\\p{L}]*?.*?Level\\s+(?<level>\\d+)\\s*[•·|]\\s*XP[:\\s]+\\+?\\s*(?<xp>[0-9][0-9.,]*)\\s*[•·|]\\s*\\+?\\s*\\$\\s*(?<money>[0-9][0-9.,]*)\\s*[•·|]\\s*(?<progress>[0-9][0-9.,]*)\\s*%",
			"(?:(?<job>\\p{L}+)\\s*)?.*?(?:Level|Lvl\\.?|Lv\\.?)\\s*(?<level>\\d+).*?\\+\\s*(?<xp>[0-9][0-9.,]*)\\s*(?:Job-)?XP.*?\\+\\s*\\$?\\s*(?<money>[0-9][0-9.,]*)\\s*\\$?(?:.*?(?<progress>[0-9][0-9.,]*)\\s*%)?"
	));

	/**
	 * Order-independent field extractors for job progress lines, used when none of the {@link #jobActionbar} patterns
	 * matches. Each pattern's first non-null capture group is the value. Covers the current OPSUCHT format
	 * {@code +2.5 XP · +12.73$ · Holzfäller · Level 58 · [...] · 11.91%}.
	 */
	public String jobFieldXp = "(?<![\\w.,])\\+?\\s*([0-9][0-9.,]*)\\s*(?:Job-?)?XP\\b";
	public String jobFieldMoney = "\\+\\s*(?:\\$\\s*([0-9][0-9.,]*)|([0-9][0-9.,]*)\\s*\\$)";
	public String jobFieldLevel = "(?i)\\b(?:Level|Lvl\\.?|Lv\\.?)\\s*([0-9]+)";
	public String jobFieldProgress = "([0-9][0-9.,]*)\\s*%";
	/** Fallback for job names not (yet) in {@link #jobNames}: the word right before "Level". */
	public String jobFieldNameBeforeLevel = "(\\p{L}[\\p{L}-]{2,24})\\s*[·•|»:\\-]?\\s*(?:Level|Lvl)";

	/** Money you received from another player. */
	public List<String> paymentIncoming = new ArrayList<>(List.of(
			PREFIX + "(?<player>" + NAME + ") hat dir (?:\\$\\s?)?(?<amount>" + AMOUNT + ")\\s?\\$? (?:überwiesen|gezahlt|bezahlt|gesendet|gegeben|geschickt)",
			PREFIX + "Du hast (?:\\$\\s?)?(?<amount>" + AMOUNT + ")\\s?\\$? von (?<player>" + NAME + ") (?:erhalten|bekommen)"
	));

	/** Money you sent to another player (/pay). */
	public List<String> paymentOutgoing = new ArrayList<>(List.of(
			PREFIX + "Du hast (?<player>" + NAME + ") (?:\\$\\s?)?(?<amount>" + AMOUNT + ")\\s?\\$? (?:überwiesen|gezahlt|bezahlt|gesendet|gegeben|geschickt)",
			PREFIX + "Du hast (?:\\$\\s?)?(?<amount>" + AMOUNT + ")\\s?\\$? an (?<player>" + NAME + ") (?:überwiesen|gezahlt|bezahlt|gesendet|gegeben|geschickt)"
	));

	/**
	 * Prefixes that identify genuine system messages. Payment messages with any other prefix (e.g. a player name in
	 * front of "»") are ignored, so fake payment messages written by players are not booked.
	 */
	public List<String> systemPrefixes = new ArrayList<>(List.of(
			"OPSUCHT", "OP", "System", "Server", "Bank", "Money", "Geld", "Pay", "Zahlung", "Jobs", "Job", "Markt", "Info", "RTP", "Teleport", "Farmwelt"));

	/** Public / private chat lines written by players. Used for clickable names and to reject fake system lines. */
	public List<String> playerChat = new ArrayList<>(List.of(
			"^(?:\\[[^\\]]{1,32}\\]\\s*)*(?:[^|»:\\[\\]]{1,24}?\\s*[|┃]\\s*)?(?<name>" + NAME + ")\\s*(?:»|>>|➜|→|:)\\s*(?<message>.+)$",
			"^\\[?(?<name>" + NAME + ")\\s*(?:->|→|➜|»)\\s*(?:mir|dir|Dir|me|Du)\\]?\\s*:?\\s*(?<message>.*)$"
	));

	/** Auction-house references inside a chat message, e.g. "schaut in mein /ah". */
	public String auctionHint = "(?i)(?<![\\w/])/(?:ah|auktionshaus)(?:\\s+(?<target>" + NAME + "))?(?![\\w/])";

	// ---- RTP (random teleport / Biom-Teleport) ----
	/** A line is only considered RTP-related if this matches. */
	public String rtpContext = "(?i)(\\brtp\\b|random\\s*-?\\s*t(?:ele)?p|zufällig\\w*\\s+teleport|teleport|\\bbiom|warteschlange|queue)";
	public String rtpCancelled = "(?i)(abgemeldet|abgebrochen|storniert|verlassen|aus der warteschlange entfernt|nicht mehr in der warteschlange|kein(?:en)? (?:passenden )?(?:ort|platz|biom) gefunden|fehlgeschlagen)";
	public String rtpTeleported = "(?i)(du wurdest\\b.{0,60}?teleportiert|erfolgreich teleportiert|wurdest teleportiert|teleportiert[.!]?$|du bist (?:nun|jetzt) im biom|willkommen im biom)";
	public String rtpCooldown = "(?i)(?:cooldown|abklingzeit|erst wieder|erneut|wieder nutzen|wieder verwenden|noch warten|musst noch)\\D{0,40}?([0-9]+)\\s*(s\\b|sek\\w*|min\\w*|std\\w*|stunde\\w*|h\\b)";
	public String rtpQueued = "(?i)(angemeldet|hinzugefügt|eingetragen|in die warteschlange|zur warteschlange|warteschlange beigetreten|wird gesucht|suche nach|gesucht|du bist (?:jetzt )?in der warteschlange)";
	public String rtpCountdown = "(?i)\\bin\\s+([0-9]+)\\s*(?:s\\b|sek\\.?|sekunden?)";
	public String rtpPosition = "(?i)(?:position|platz|stelle)\\s*:?\\s*#?\\s*([0-9]+)|#([0-9]+)";
	public String rtpBiome = "(?i)biom(?:e)?\\s*[:»\\-]?\\s*[\"'„“»]?(\\p{L}[\\p{L}_\\- ]{1,40}?)[\"'“”«]?\\s*(?:$|[.!,()\\[]|\\b(?:angemeldet|hinzugefügt|eingetragen|wurde|teleport|gefunden|gesucht|in\\s+[0-9]))";

	// ---- compiled ----
	private transient List<Pattern> cJobActionbar;
	private transient List<Pattern> cPaymentIncoming;
	private transient List<Pattern> cPaymentOutgoing;
	private transient List<Pattern> cPlayerChat;
	private transient Pattern cAuctionHint;
	private transient Pattern cFieldXp, cFieldMoney, cFieldLevel, cFieldProgress, cFieldNameBeforeLevel;
	private transient Pattern cRtpContext, cRtpCancelled, cRtpTeleported, cRtpCooldown, cRtpQueued, cRtpCountdown,
			cRtpPosition, cRtpBiome;
	private transient List<String> errors;

	/** Compiles all patterns; invalid ones are skipped and reported via {@link #errors()}. */
	public OpsuchtPatterns compile() {
		errors = new ArrayList<>();
		cJobActionbar = compileAll("jobActionbar", jobActionbar);
		cPaymentIncoming = compileAll("paymentIncoming", paymentIncoming);
		cPaymentOutgoing = compileAll("paymentOutgoing", paymentOutgoing);
		cPlayerChat = compileAll("playerChat", playerChat);
		List<Pattern> ah = compileAll("auctionHint", auctionHint == null ? List.of() : List.of(auctionHint));
		cAuctionHint = ah.isEmpty() ? null : ah.get(0);
		OpsuchtPatterns d = DEFAULTS_SOURCE;
		cFieldXp = one("jobFieldXp", jobFieldXp, d.jobFieldXp);
		cFieldMoney = one("jobFieldMoney", jobFieldMoney, d.jobFieldMoney);
		cFieldLevel = one("jobFieldLevel", jobFieldLevel, d.jobFieldLevel);
		cFieldProgress = one("jobFieldProgress", jobFieldProgress, d.jobFieldProgress);
		cFieldNameBeforeLevel = one("jobFieldNameBeforeLevel", jobFieldNameBeforeLevel, d.jobFieldNameBeforeLevel);
		cRtpContext = one("rtpContext", rtpContext, d.rtpContext);
		cRtpCancelled = one("rtpCancelled", rtpCancelled, d.rtpCancelled);
		cRtpTeleported = one("rtpTeleported", rtpTeleported, d.rtpTeleported);
		cRtpCooldown = one("rtpCooldown", rtpCooldown, d.rtpCooldown);
		cRtpQueued = one("rtpQueued", rtpQueued, d.rtpQueued);
		cRtpCountdown = one("rtpCountdown", rtpCountdown, d.rtpCountdown);
		cRtpPosition = one("rtpPosition", rtpPosition, d.rtpPosition);
		cRtpBiome = one("rtpBiome", rtpBiome, d.rtpBiome);
		if (serverAddresses == null) serverAddresses = new ArrayList<>();
		if (jobNames == null) jobNames = new ArrayList<>();
		if (systemPrefixes == null) systemPrefixes = new ArrayList<>();
		return this;
	}

	private List<Pattern> compileAll(String key, List<String> sources) {
		List<Pattern> out = new ArrayList<>();
		if (sources == null) return out;
		for (String src : sources) {
			try {
				out.add(Pattern.compile(src));
			} catch (PatternSyntaxException e) {
				errors.add(key + ": " + e.getDescription());
			}
		}
		return out;
	}

	/** Compiles a single pattern; falls back to the built-in default if the field is missing or invalid. */
	private Pattern one(String key, String source, String fallback) {
		if (source != null && !source.isBlank()) {
			try {
				return Pattern.compile(source);
			} catch (PatternSyntaxException e) {
				errors.add(key + ": " + e.getDescription());
			}
		}
		return fallback == null ? null : Pattern.compile(fallback);
	}

	/** Uncompiled default instance, used as fallback for missing fields in older files. */
	private static final OpsuchtPatterns DEFAULTS_SOURCE = new OpsuchtPatterns();

	public Pattern fieldXp() {
		return cFieldXp;
	}

	public Pattern fieldMoney() {
		return cFieldMoney;
	}

	public Pattern fieldLevel() {
		return cFieldLevel;
	}

	public Pattern fieldProgress() {
		return cFieldProgress;
	}

	public Pattern fieldNameBeforeLevel() {
		return cFieldNameBeforeLevel;
	}

	public Pattern rtpContext() {
		return cRtpContext;
	}

	public Pattern rtpCancelled() {
		return cRtpCancelled;
	}

	public Pattern rtpTeleported() {
		return cRtpTeleported;
	}

	public Pattern rtpCooldown() {
		return cRtpCooldown;
	}

	public Pattern rtpQueued() {
		return cRtpQueued;
	}

	public Pattern rtpCountdown() {
		return cRtpCountdown;
	}

	public Pattern rtpPosition() {
		return cRtpPosition;
	}

	public Pattern rtpBiome() {
		return cRtpBiome;
	}

	public List<Pattern> jobActionbar() {
		return cJobActionbar;
	}

	public List<Pattern> paymentIncoming() {
		return cPaymentIncoming;
	}

	public List<Pattern> paymentOutgoing() {
		return cPaymentOutgoing;
	}

	public List<Pattern> playerChat() {
		return cPlayerChat;
	}

	public Pattern auctionHint() {
		return cAuctionHint;
	}

	public List<String> errors() {
		return errors == null ? List.of() : errors;
	}

	public boolean isSystemPrefix(String prefix) {
		if (prefix == null) return true;
		String p = prefix.strip().toLowerCase(Locale.ROOT);
		if (p.isEmpty()) return true;
		for (String s : systemPrefixes) {
			if (p.equals(s.toLowerCase(Locale.ROOT))) return true;
		}
		return false;
	}

	/** Returns the canonical job name if {@code candidate} is (case-insensitively) a known job. */
	public String canonicalJob(String candidate) {
		if (candidate == null) return null;
		for (String job : jobNames) {
			if (job.equalsIgnoreCase(candidate.strip())) return job;
		}
		return null;
	}

	/** Finds a known job name anywhere in the line. */
	public String findJobIn(String line) {
		String lower = line.toLowerCase(Locale.ROOT);
		for (String job : jobNames) {
			if (lower.contains(job.toLowerCase(Locale.ROOT))) return job;
		}
		return null;
	}

	public boolean isOpsuchtAddress(String address) {
		if (address == null) return false;
		String a = address.toLowerCase(Locale.ROOT);
		for (String s : serverAddresses) {
			if (!s.isBlank() && a.contains(s.toLowerCase(Locale.ROOT))) return true;
		}
		return false;
	}

	public static OpsuchtPatterns defaults() {
		return new OpsuchtPatterns().compile();
	}
}
