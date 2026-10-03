package de.optools.gui.screen.tabs;

import de.optools.commands.CommandShortcut;
import de.optools.config.HudWidgetConfig;
import de.optools.config.ModuleId;
import de.optools.config.OpToolsConfig;
import de.optools.gui.Theme;
import de.optools.gui.screen.HudEditorScreen;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.SettingsPanel;
import de.optools.gui.widget.UiButton;
import de.optools.hud.HudWidget;
import de.optools.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Path;

public final class SettingsTab extends TabView {
	public SettingsTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		OpToolsConfig cfg = mod.config();
		SettingsPanel p = add(new SettingsPanel(mod::saveConfig));
		p.bounds(x, y + 14, w, h - 14);

		p.header("Module");
		for (ModuleId id : ModuleId.values()) {
			p.toggle(id.title, id.description, () -> cfg.modules.isEnabled(id), v -> cfg.modules.set(id, v));
		}
		p.info("Kurzbefehle werden beim nächsten Serverbeitritt neu registriert.", Theme.FAINT);

		p.header("HUD");
		p.action("Widgets frei verschieben und skalieren", "HUD-Editor öffnen", UiButton.Style.PRIMARY,
				() -> Minecraft.getInstance().setScreen(new HudEditorScreen(screen)));
		for (HudWidget widget : mod.hud().widgets()) {
			HudWidgetConfig wc = widget.config();
			p.spacer(4);
			p.info("» " + widget.title(), Theme.accent());
			p.toggle("Anzeigen", null, () -> wc.visible, v -> wc.visible = v);
			p.toggle("Ausblenden ohne Daten", "Kein Widget, solange keine Session läuft", () -> wc.hideWhenIdle, v -> wc.hideWhenIdle = v);
			p.slider("Größe", 0.5, 2.5, 0.05, () -> wc.scale, v -> wc.scale = (float) v, v -> Math.round(v * 100) + " %");
			p.slider("Hintergrund-Deckkraft", 0, 1, 0.05, () -> wc.opacity, v -> wc.opacity = (float) v, v -> Math.round(v * 100) + " %");
			for (HudWidget.LineOption option : widget.lineOptions()) {
				p.toggle("Zeile: " + option.label(), null, () -> wc.lines.contains(option.id()), v -> {
					if (v) wc.lines.add(option.id());
					else wc.lines.remove(option.id());
				});
			}
		}

		p.header("Job-Tracking");
		p.slider("AFK nach (stoppt Zeit & Stundenschnitt)", 1, 20, 1, () -> cfg.jobs.idleThresholdSeconds,
				v -> cfg.jobs.idleThresholdSeconds = (int) v, v -> (int) v + " s");
		p.info("Ohne Job-Aktion länger als diese Zeit gilt man als AFK: die Session bleibt bestehen, aber die Pause "
				+ "zählt nicht in die aktive Zeit und verfälscht XP/h und $/h nicht.", Theme.FAINT);
		p.slider("Session automatisch beenden nach", 1, 120, 1, () -> cfg.jobs.sessionTimeoutMinutes,
				v -> cfg.jobs.sessionTimeoutMinutes = (int) v, v -> (int) v + " min");
		p.slider("Doppelte Actionbar ignorieren innerhalb", 0, 3000, 50, () -> cfg.jobs.duplicateWindowMs,
				v -> cfg.jobs.duplicateWindowMs = (int) v, v -> v < 1 ? "Aus (jedes Paket zählt)" : (int) v + " ms");
		p.cycle("Gewinn-Teiler (falls doppelt gezählt wird)", () -> Fmt.decimal(cfg.jobs.gainDivisor),
				() -> cfg.jobs.gainDivisor = cfg.jobs.gainDivisor >= 2 ? 1 : 2);
		p.slider("Maximal gespeicherte Sessions", 50, 5000, 50, () -> cfg.jobs.maxHistorySessions,
				v -> cfg.jobs.maxHistorySessions = (int) v, v -> String.valueOf((int) v));

		p.header("Finanzbuch");
		p.toggle("Zahlungen erfassen", "/pay an und von Spielern", () -> cfg.finance.trackPayments, v -> cfg.finance.trackPayments = v);
		p.toggle("Jobeinnahmen erfassen", "Werden gebündelt gebucht", () -> cfg.finance.trackJobIncome, v -> cfg.finance.trackJobIncome = v);
		p.slider("Jobeinnahmen bündeln je", 1, 60, 1, () -> cfg.finance.jobIncomeAggregationMinutes,
				v -> cfg.finance.jobIncomeAggregationMinutes = (int) v, v -> (int) v + " min");

		p.header("Chat");
		p.toggle("Spielernamen anklickbar", "Klick bereitet /msg <Name> vor", () -> cfg.chat.clickableNames, v -> cfg.chat.clickableNames = v);
		p.toggle("/msg direkt ausführen", "Statt nur vorzubereiten (Befehl ohne Leerzeichen am Ende)", () -> cfg.chat.runMsgDirectly,
				v -> cfg.chat.runMsgDirectly = v);
		p.toggle("/ah-Angebote anklickbar", "„/ah“ im Chat → /ah <Name>", () -> cfg.chat.auctionLinks, v -> cfg.chat.auctionLinks = v);
		p.toggle("Hinweise beim Überfahren", null, () -> cfg.chat.hoverHints, v -> cfg.chat.hoverHints = v);

		p.header("Darstellung & Allgemein");
		p.cycle("Akzentfarbe", () -> Theme.ACCENT_NAMES[accentIndex(cfg)], () -> {
			int next = (accentIndex(cfg) + 1) % Theme.ACCENTS.length;
			cfg.general.accentColor = Theme.ACCENTS[next];
		});
		p.cycle("Zahlenformat", () -> cfg.general.numberStyle.label, () -> cfg.general.numberStyle =
				cfg.general.numberStyle == OpToolsConfig.NumberStyle.COMPACT ? OpToolsConfig.NumberStyle.FULL : OpToolsConfig.NumberStyle.COMPACT);
		p.toggle("Shard-Kurse im Item-Tooltip", "Rohstoffhändler-Kurs unter passenden Items", () -> cfg.market.tooltipRates,
				v -> cfg.market.tooltipRates = v);
		p.toggle("Nur auf OPSUCHT aktiv", "Parser, HUD und Chat-Aktionen nur auf opsucht.net", () -> cfg.general.onlyOnOpsucht,
				v -> cfg.general.onlyOnOpsucht = v);

		p.header("Kurzbefehle");
		for (CommandShortcut s : mod.shortcuts().all()) {
			p.info("/" + s.alias + "  →  /" + s.resolve("") + (s.description.isEmpty() ? "" : "   (" + s.description + ")"),
					s.enabled ? Theme.TEXT : Theme.FAINT);
		}
		p.info("Weitere Kurzbefehle in shortcuts.json ergänzen, danach /optools reload und neu verbinden.", Theme.FAINT);
		p.action("shortcuts.json", "Öffnen", UiButton.Style.SECONDARY, () -> open(mod.shortcuts().file()));

		p.header("Daten & Speicher");
		p.info("Speicher: " + mod.dataStore().provider().displayName() + " – " + mod.dir().resolve("data"), Theme.MUTED);
		p.info("Eigener OP Tools Server: Coming Soon (ab einer späteren Version, aktuell nicht aktiv).", Theme.WARNING);
		p.action("Datenordner", "Öffnen", UiButton.Style.SECONDARY, () -> open(mod.dir()));
		p.action("Job-Historie (" + mod.dataStore().jobSessions().size() + " Sessions)", "Löschen", UiButton.Style.DANGER,
				() -> confirm("Job-Historie löschen?", () -> {
					mod.jobTracker().discardSession();
					mod.dataStore().clearJobSessions();
					mod.dataStore().saveAsync();
				}));
		p.action("Finanzbuch (" + mod.dataStore().financeEntries().size() + " Buchungen)", "Leeren", UiButton.Style.DANGER,
				() -> confirm("Finanzbuch leeren?", () -> {
					mod.dataStore().clearFinance();
					mod.dataStore().saveAsync();
				}));

		p.header("OPSUCHT-Erkennung");
		p.info("Alle Servertexte (Actionbar, Zahlungen, Chat) stehen als Regex in opsucht-patterns.json. Ändert OPSUCHT ein "
				+ "Format, kann es dort angepasst werden („customized“: true verhindert das Überschreiben bei Updates).", Theme.MUTED);
		if (!mod.patterns().errors().isEmpty()) {
			for (String e : mod.patterns().errors()) p.info("⚠ " + e, Theme.NEGATIVE);
		}
		p.action("opsucht-patterns.json", "Öffnen", UiButton.Style.SECONDARY, () -> open(mod.patternRepository().file()));
		p.action("Dateien neu laden", "Neu laden", UiButton.Style.SECONDARY, () -> {
			mod.reloadFiles();
			screen.refreshTab();
		});
		p.spacer(10);
	}

	private static int accentIndex(OpToolsConfig cfg) {
		for (int i = 0; i < Theme.ACCENTS.length; i++) if (Theme.ACCENTS[i] == cfg.general.accentColor) return i;
		return 0;
	}

	private void open(Path path) {
		Util.getPlatform().openPath(path);
	}

	private void confirm(String title, Runnable action) {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new ConfirmScreen(yes -> {
			if (yes) action.run();
			mc.setScreen(new MainScreen(MainScreen.Tab.SETTINGS));
		}, Component.literal(title), Component.literal("Das kann nicht rückgängig gemacht werden.")));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		title(g, "Einstellungen", "Änderungen werden automatisch gespeichert");
	}
}
