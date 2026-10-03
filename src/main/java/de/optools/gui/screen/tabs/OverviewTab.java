package de.optools.gui.screen.tabs;

import de.optools.finance.FinanceBook;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.HudEditorScreen;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.UiButton;
import de.optools.jobs.JobTracker;
import de.optools.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalDate;
import java.time.ZoneId;

public final class OverviewTab extends TabView {
	private FinanceBook.Summary today, week;
	private long lastCalc;

	public OverviewTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		int by = y + h - 20;
		int bx = x;
		String[] labels = {"HUD anpassen", "Session beenden", "Pause / Weiter", "Einstellungen"};
		Runnable[] actions = {
				() -> Minecraft.getInstance().setScreen(new HudEditorScreen(screen)),
				() -> mod.jobTracker().finishSession(),
				() -> mod.jobTracker().setPaused(!mod.jobTracker().isPaused()),
				() -> screen.switchTab(MainScreen.Tab.SETTINGS)
		};
		int total = 0;
		for (String l : labels) total += UiButton.widthFor(l) + 6;
		boolean fit = total - 6 <= w;
		for (int i = 0; i < labels.length; i++) {
			int bw = fit ? UiButton.widthFor(labels[i]) : col(w, labels.length, 4);
			add(new UiButton(labels[i], i == 0 ? UiButton.Style.PRIMARY : UiButton.Style.SECONDARY, actions[i]))
					.tooltip(fit ? null : labels[i]).bounds(bx, by, bw, 18);
			bx += bw + (fit ? 6 : 4);
		}
	}

	private void recalc() {
		long now = System.currentTimeMillis();
		if (today != null && now - lastCalc < 1000) return;
		lastCalc = now;
		long startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		today = mod.financeBook().summary(startOfDay);
		week = mod.financeBook().summary(now - 7L * 24 * 3600_000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		recalc();
		title(g, "Übersicht", "Willkommen bei OP Tools");
		JobTracker.Snapshot s = mod.jobTracker().snapshot();
		int top = y + 16;
		int cw = col(w, 4, 6);
		int ch = 40;
		String job = s.active() ? (s.job() == null ? JobTracker.UNKNOWN_JOB : s.job()) + (s.level() >= 0 ? " · L" + s.level() : "") : "Keine Session";
		stat(g, x, top, cw, ch, "Session-XP", s.active() ? Fmt.num(s.sessionXp()) : "–", Theme.TEXT, job);
		stat(g, x + (cw + 6), top, cw, ch, "Session-Geld", s.active() ? Fmt.cash(s.sessionMoney()) : "–", Theme.POSITIVE,
				s.active() ? Fmt.duration(s.activeMs()) + " aktiv" : null);
		stat(g, x + (cw + 6) * 2, top, cw, ch, "XP / Stunde", s.active() ? Fmt.num(s.xpPerHour()) : "–", Theme.accent(),
				s.active() ? Fmt.cash(s.moneyPerHour()) + "/h" : null);
		stat(g, x + (cw + 6) * 3, top, cw, ch, "Nächstes Level", s.etaMs() >= 0 ? "~" + Fmt.duration(s.etaMs()) : "–", Theme.INFO,
				!Double.isNaN(s.progress()) ? Fmt.percent(s.progress()) + " erreicht" : null);

		// progress
		int py = top + ch + 8;
		UiDraw.card(g, x, py, w, 28);
		UiDraw.text(g, s.active() && !Double.isNaN(s.progress()) ? "Level-Fortschritt " + job : "Level-Fortschritt", x + 6, py + 5, Theme.MUTED);
		double frac = Double.isNaN(s.progress()) ? 0 : s.progress() / 100.0;
		UiDraw.progressBar(g, x + 6, py + 17, w - 12, 5, frac, Theme.accent(), Theme.withAlpha(0xFFFFFF, 25));
		if (s.paused()) UiDraw.badge(g, "PAUSIERT", x + w - 60, py + 3, Theme.WARNING);

		// finance
		int fy = py + 36;
		int fw = col(w, 3, 6);
		stat(g, x, fy, fw, ch, "Netto heute", Fmt.signedMoney(today.net() + mod.financeBook().pendingJobIncome()),
				today.net() >= 0 ? Theme.POSITIVE : Theme.NEGATIVE, today.count() + " Buchungen");
		stat(g, x + fw + 6, fy, fw, ch, "Netto 7 Tage", Fmt.signedMoney(week.net()), week.net() >= 0 ? Theme.POSITIVE : Theme.NEGATIVE,
				"Ein " + Fmt.cash(week.income()) + " · Aus " + Fmt.cash(week.expense()));
		var market = mod.market();
		stat(g, x + (fw + 6) * 2, fy, fw, ch, "Marktdaten", market.pricesUpdated() > 0 ? market.prices().size() + " Items" : "nicht geladen",
				Theme.TEXT, market.pricesUpdated() > 0 ? "Stand " + Fmt.time(market.pricesUpdated()) : "Tab „Marktpreise“ öffnen");

		// hints
		int iy = fy + ch + 12;
		if (!mod.isActiveServer()) {
			UiDraw.text(g, UiDraw.ellipsize("ℹ Tracking ist aktiv, sobald du mit OPSUCHT verbunden bist (opsucht.net).", w), x, iy, Theme.WARNING);
			iy += 12;
		}
		if (!mod.patterns().errors().isEmpty()) {
			UiDraw.text(g, UiDraw.ellipsize("⚠ " + mod.patterns().errors().size() + " fehlerhafte Einträge in opsucht-patterns.json", w), x, iy, Theme.NEGATIVE);
			iy += 12;
		}
		String key = mod.menuKey().getTranslatedKeyMessage().getString();
		UiDraw.text(g, UiDraw.ellipsize("Tipp: Menü mit [" + key + "] oder /optools öffnen · /optools session neu startet eine neue Session.", w), x, iy, Theme.FAINT);
	}
}
