package de.optools.hud;

import de.optools.OpTools;
import de.optools.config.ModuleId;
import de.optools.finance.FinanceBook;
import de.optools.gui.Theme;
import de.optools.util.Fmt;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Small finance widget: today's income, expenses and net. Hidden by default. */
public final class FinanceHudWidget extends HudWidget {
	private static final List<LineOption> OPTIONS = List.of(
			new LineOption("net", "Netto heute", true),
			new LineOption("income", "Einnahmen heute", true),
			new LineOption("expense", "Ausgaben heute", true));

	private long cacheTime;
	private FinanceBook.Summary cached;

	@Override
	public String id() {
		return "finance";
	}

	@Override
	public String title() {
		return "Finanzen heute";
	}

	@Override
	public ModuleId module() {
		return ModuleId.FINANCE;
	}

	@Override
	public List<LineOption> lineOptions() {
		return OPTIONS;
	}

	@Override
	protected double defaultY() {
		return 0.5;
	}

	@Override
	protected boolean defaultVisible() {
		return false;
	}

	@Override
	public List<HudRow> rows(boolean preview) {
		long now = System.currentTimeMillis();
		if (cached == null || now - cacheTime > 2000) {
			long startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			cached = OpTools.get().financeBook().summary(startOfDay);
			cacheTime = now;
		}
		FinanceBook.Summary s = cached;
		double pending = OpTools.get().financeBook().pendingJobIncome();
		double income = s.income() + pending;
		double net = s.net() + pending;
		List<HudRow> rows = new ArrayList<>();
		if (shows("net")) rows.add(HudRow.text("Netto", Fmt.signedMoney(net), net >= 0 ? Theme.POSITIVE : Theme.NEGATIVE));
		if (shows("income")) rows.add(HudRow.text("Ein", Fmt.money(income), Theme.POSITIVE));
		if (shows("expense")) rows.add(HudRow.text("Aus", Fmt.money(s.expense()), Theme.NEGATIVE));
		return rows;
	}
}
