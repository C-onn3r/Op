package de.optools.config;

/** All feature modules that can be toggled individually. */
public enum ModuleId {
	JOB_TRACKER("Job-Tracker", "Wertet die Job-Actionbar aus: XP, Geld, Level, Raten und ETA."),
	FINANCE("Finanzbuch", "Erfasst Einnahmen und Ausgaben aus Zahlungen und Jobs."),
	MARKET("Markt & Shards", "Marktplatzpreise und Rohstoffhändler-Kurse über die offizielle OPSUCHT-API."),
	COMMANDS("Kurzbefehle", "Kurzbefehle wie /cb1 – /cb6 und /farm-Shortcuts."),
	CHAT("Chat-Aktionen", "Anklickbare Spielernamen und /ah-Angebote im Chat."),
	HUD("HUD", "Frei verschiebbare Widgets im Spiel."),
	RTP("RTP-Tracker", "Erkennt Biom-Teleport-/RTP-Meldungen und zeigt den Status an.");

	public final String title;
	public final String description;

	ModuleId(String title, String description) {
		this.title = title;
		this.description = description;
	}
}
