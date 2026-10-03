package de.optools.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain, Gson-serialised configuration. Only data, no logic — new fields get sane defaults automatically,
 * so older config files keep working after an update.
 */
public final class OpToolsConfig {
	public static final int CURRENT_VERSION = 2;

	public int configVersion = CURRENT_VERSION;

	/** First-start wizard has been completed. */
	public boolean setupDone = false;

	public Storage storage = new Storage();
	public Modules modules = new Modules();
	public General general = new General();
	public Jobs jobs = new Jobs();
	public Finance finance = new Finance();
	public Market market = new Market();
	public Chat chat = new Chat();
	public Map<String, HudWidgetConfig> hud = new LinkedHashMap<>();

	public static final class Storage {
		/** Which {@link de.optools.storage.DataProvider} to use. In 0.1 Alpha only LOCAL is functional. */
		public DataMode mode = DataMode.LOCAL;
		/** Address of a self-hosted OP Tools server (prepared for later versions, not used yet). */
		public String remoteAddress = "";
	}

	public enum DataMode {
		LOCAL,
		/** Coming soon – self-hosted OP Tools backend. */
		REMOTE
	}

	public static final class Modules {
		public boolean jobTracker = true;
		public boolean finance = true;
		public boolean market = true;
		public boolean commandShortcuts = true;
		public boolean chatActions = true;
		public boolean hud = true;
		public boolean rtp = true;

		public boolean isEnabled(ModuleId id) {
			return switch (id) {
				case RTP -> rtp;
				case JOB_TRACKER -> jobTracker;
				case FINANCE -> finance;
				case MARKET -> market;
				case COMMANDS -> commandShortcuts;
				case CHAT -> chatActions;
				case HUD -> hud;
			};
		}

		public void set(ModuleId id, boolean enabled) {
			switch (id) {
				case RTP -> rtp = enabled;
				case JOB_TRACKER -> jobTracker = enabled;
				case FINANCE -> finance = enabled;
				case MARKET -> market = enabled;
				case COMMANDS -> commandShortcuts = enabled;
				case CHAT -> chatActions = enabled;
				case HUD -> hud = enabled;
			}
		}
	}

	public static final class General {
		/** Only parse messages / run server-specific features while connected to OPSUCHT. */
		public boolean onlyOnOpsucht = true;
		/** Accent colour of the UI (RGB). */
		public int accentColor = 0x7C5CFF;
		/** Write all received actionbar/title/bossbar/system texts to config/optools/debug (troubleshooting). */
		public boolean debugLogIncoming = false;
	}

	public static final class Jobs {
		/** Gaps longer than this do not count as active time (AFK, menus, ...). */
		public int idleThresholdSeconds = 60;
		/** After this much inactivity the running session is closed and stored in the history. */
		public int sessionTimeoutMinutes = 15;
		/**
		 * Identical actionbar texts within this window are treated as a re-send, not a new gain. 0 = count every
		 * packet: OPSUCHT sends one actionbar per paid block (Timber axes send many identical ones in one tick).
		 */
		public int duplicateWindowMs = 0;
		/** Each parsed gain is divided by this value (safety valve if the server sends each gain twice). */
		public double gainDivisor = 1.0;
		/** Maximum stored sessions in the history. */
		public int maxHistorySessions = 500;
	}

	public static final class Finance {
		/** Book job income into the finance book. */
		public boolean trackJobIncome = true;
		/** Job income is aggregated into one ledger entry per job and interval. */
		public int jobIncomeAggregationMinutes = 10;
		public boolean trackPayments = true;
		public int maxEntries = 20000;
	}

	public static final class Market {
		/** Refresh interval while the market view is open (API cache TTL is 60 s). */
		public int refreshSeconds = 60;
		public String apiBaseUrl = "https://api.opsucht.net";
		/** Show Rohstoffhändler rates in item tooltips. */
		public boolean tooltipRates = true;
	}

	public static final class Chat {
		public boolean clickableNames = true;
		/** true: /msg is executed directly... false: only prepared in the chat input (safer, default). */
		public boolean runMsgDirectly = false;
		public String nameClickCommand = "/msg {name} ";
		public boolean auctionLinks = true;
		public boolean hoverHints = true;
	}
}
