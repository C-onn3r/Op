package de.optools.gui.screen.tabs;

import de.optools.finance.FinanceBook;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.BarChart;
import de.optools.gui.widget.LineChart;
import de.optools.gui.widget.ScrollList;
import de.optools.gui.widget.UiButton;
import de.optools.gui.widget.UiChips;
import de.optools.storage.model.FinanceEntry;
import de.optools.util.Fmt;
import net.minecraft.client.gui.GuiGraphics;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class FinanceTab extends TabView {
	private static final List<String> FILTERS = List.of("Alle", "Einnahmen", "Ausgaben", "Zahlungen", "Jobs");
	private static int filter;

	private LineChart netChart;
	private BarChart dailyChart;
	private ScrollList<FinanceEntry> list;
	private UiButton deleteButton;
	private FinanceBook.Summary today, week, month, all;
	private long lastUpdate;

	public FinanceTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		int top = y + 62;
		int chartH = Math.max(70, Math.min(105, (h - 90) / 2));
		int half = col(w, 2, 6);
		netChart = add(new LineChart("Nettoentwicklung (30 Tage)"));
		netChart.bounds(x, top, half, chartH);
		dailyChart = add(new BarChart("Einnahmen & Ausgaben pro Tag"));
		dailyChart.bounds(x + half + 6, top, half, chartH);
		dailyChart.series(new String[]{"Ein", "Aus"}, new int[]{Theme.POSITIVE, Theme.NEGATIVE}).format(Fmt::compact);

		int chipsY = top + chartH + 6;
		UiChips chips = add(new UiChips(FILTERS, filter, i -> {
			filter = i;
			lastUpdate = 0;
		}));
		chips.bounds(x, chipsY, w - 120, 13);
		int chipsH = chips.layout();

		deleteButton = add(new UiButton("Eintrag löschen", UiButton.Style.DANGER, () -> {
			FinanceEntry sel = list.selected();
			if (sel != null) {
				mod.dataStore().deleteFinanceEntry(sel.id);
				mod.dataStore().saveAsync();
				list.select(null);
				lastUpdate = 0;
			}
		}).tooltip("Entfernt falsch erkannte Buchungen"));
		int dw = UiButton.widthFor("Eintrag löschen");
		deleteButton.bounds(x + w - dw, chipsY - 1, dw, 14);

		int listTop = chipsY + chipsH + 5;
		list = add(new ScrollList<FinanceEntry>(12, (g, e, rx, ry, rw, rh, hover, mx, my) -> {
			UiDraw.text(g, Fmt.dateTime(e.timestamp), rx + 4, ry + 2, Theme.MUTED);
			int catColor = e.category == FinanceEntry.Category.JOB ? Theme.accent() : e.category == FinanceEntry.Category.PAYMENT ? Theme.INFO : Theme.MUTED;
			boolean wide = rw > 300;
			if (wide) UiDraw.text(g, e.category.label, rx + 66, ry + 2, catColor);
			String amount = Fmt.signedMoney(e.signedAmount());
			int aw = UiDraw.font().width(amount);
			int dx = wide ? 112 : 66;
			UiDraw.text(g, UiDraw.ellipsize(e.description, rw - dx - 8 - aw), rx + dx, ry + 2, wide ? Theme.TEXT : catColor);
			UiDraw.textRight(g, amount, rx + rw - 4, ry + 2, e.direction == FinanceEntry.Direction.INCOME ? Theme.POSITIVE : Theme.NEGATIVE);
			if (hover && !e.raw.isEmpty()) UiDraw.tooltip(g, List.of(e.description, "Nachricht: " + e.raw), mx, my);
		})).emptyText("Noch keine Buchungen – Zahlungen und Jobeinnahmen werden automatisch erfasst");
		list.bounds(x, listTop, w, y + h - listTop);
		update();
	}

	@Override
	public void tick() {
		deleteButton.enabled = list.selected() != null;
		if (System.currentTimeMillis() - lastUpdate > 2000) update();
	}

	private void update() {
		lastUpdate = System.currentTimeMillis();
		FinanceBook book = mod.financeBook();
		long now = System.currentTimeMillis();
		long startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
		today = book.summary(startOfDay);
		week = book.summary(now - 7L * 86_400_000);
		month = book.summary(now - 30L * 86_400_000);
		all = book.summary(0);

		netChart.data(book.cumulativeNet(now - 30L * 86_400_000))
				.format(v -> Fmt.dateTime((long) v), Fmt::compact).color(Theme.POSITIVE)
				.emptyText("Noch zu wenige Buchungen");

		DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.");
		List<BarChart.Bar> bars = new ArrayList<>();
		for (FinanceBook.Day d : book.daily(14)) bars.add(new BarChart.Bar(df.format(d.date()), new double[]{d.income(), -d.expense()}));
		dailyChart.data(bars);

		Predicate<FinanceEntry> p = switch (filter) {
			case 1 -> e -> e.direction == FinanceEntry.Direction.INCOME;
			case 2 -> e -> e.direction == FinanceEntry.Direction.EXPENSE;
			case 3 -> e -> e.category == FinanceEntry.Category.PAYMENT;
			case 4 -> e -> e.category == FinanceEntry.Category.JOB;
			default -> e -> true;
		};
		List<FinanceEntry> entries = new ArrayList<>(mod.dataStore().financeEntries(p));
		java.util.Collections.reverse(entries);
		FinanceEntry selected = list.selected();
		list.items(entries);
		if (selected != null && !entries.contains(selected)) list.select(null);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		double pending = mod.financeBook().pendingJobIncome();
		title(g, "Finanzbuch", pending > 0 ? "+" + Fmt.money(pending) + " Jobeinnahmen werden gesammelt" : null);
		if (today == null) return;
		int top = y + 16;
		int cw = col(w, 4, 6);
		card(g, x, top, cw, "Heute", today);
		card(g, x + cw + 6, top, cw, "7 Tage", week);
		card(g, x + (cw + 6) * 2, top, cw, "30 Tage", month);
		card(g, x + (cw + 6) * 3, top, cw, "Gesamt", all);
	}

	private static void card(GuiGraphics g, int x, int y, int w, String label, FinanceBook.Summary s) {
		stat(g, x, y, w, 40, label, Fmt.signedMoney(s.net()), s.net() >= 0 ? Theme.POSITIVE : Theme.NEGATIVE,
				"↑" + Fmt.cash(s.income()) + "  ↓" + Fmt.cash(s.expense()));
	}
}
