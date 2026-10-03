package de.optools.gui.screen.tabs;

import de.optools.gui.ItemIcons;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.ScrollList;
import de.optools.gui.widget.UiButton;
import de.optools.market.api.MerchantRate;
import de.optools.util.Fmt;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Exchange rates of the Rohstoffhändler from {@code /merchant/rates}. */
public final class ShardsTab extends TabView {
	private ScrollList<MerchantRate> list;
	private List<MerchantRate> source;

	public ShardsTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		mod.market().refresh(false);
		int refreshW = UiButton.widthFor("Aktualisieren");
		add(new UiButton("Aktualisieren", UiButton.Style.SECONDARY, () -> mod.market().refresh(true)))
				.bounds(x + w - refreshW, y - 2, refreshW, 14);
		int top = y + 74;
		list = add(new ScrollList<MerchantRate>(20, (g, r, rx, ry, rw, rh, hover, mx, my) -> {
			g.renderItem(ItemIcons.stack(r.itemId()), rx + 3, ry + 2);
			UiDraw.text(g, UiDraw.ellipsize(r.displayName(), rw * 45 / 100 - 28), rx + 24, ry + 6, Theme.TEXT);
			UiDraw.text(g, r.targetLabel(), rx + rw * 45 / 100, ry + 6, r.target().toLowerCase(Locale.ROOT).contains("shard") ? Theme.accent() : Theme.NEGATIVE);
			UiDraw.textRight(g, Fmt.decimal2(r.exchangeRate()), rx + rw - rw * 28 / 100, ry + 6, Theme.TEXT);
			UiDraw.textRight(g, Fmt.decimal2(r.base()), rx + rw - rw * 14 / 100, ry + 6, Theme.MUTED);
			double change = r.changePercent();
			UiDraw.textRight(g, (change >= 0 ? "+" : "") + Fmt.decimal(change) + "%", rx + rw - 4, ry + 6,
					change >= 0 ? Theme.POSITIVE : Theme.NEGATIVE);
			if (hover) UiDraw.tooltip(g, List.of(r.displayName(), "Quelle: " + UiDraw.ellipsize(r.source(), 220),
					"Ziel: " + r.target(), "Kurs: " + r.exchangeRate() + " · Basis: " + r.base()), mx, my);
		})).header(13, g -> {
			int hy = top + 3;
			int scroll = list.items().size() * 20 > list.height - 13 ? 5 : 0;
			int rw = w - 2 - scroll;
			UiDraw.text(g, "Rohstoff", x + 25, hy, Theme.FAINT);
			UiDraw.text(g, "Währung", x + 1 + rw * 45 / 100, hy, Theme.FAINT);
			UiDraw.textRight(g, "Kurs", x + 1 + rw - rw * 28 / 100, hy, Theme.FAINT);
			UiDraw.textRight(g, "Basis", x + 1 + rw - rw * 14 / 100, hy, Theme.FAINT);
			UiDraw.textRight(g, "Δ", x + 1 + rw - 4, hy, Theme.FAINT);
		}).emptyText("Lade Kurse…");
		list.bounds(x, top, w, y + h - top);
		update();
	}

	private void update() {
		source = mod.market().rates();
		List<MerchantRate> rates = new ArrayList<>(source);
		rates.sort(Comparator.comparing(MerchantRate::target).thenComparing(MerchantRate::displayName));
		list.items(rates);
	}

	@Override
	public void tick() {
		mod.market().refresh(false);
		if (mod.market().rates() != source) update();
	}

	@Override
	protected int titleSpace() {
		return w - UiButton.widthFor("Aktualisieren") - 6;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		var market = mod.market();
		title(g, "Shard-Kurse", market.loading() ? "lädt…" : "Rohstoffhändler · api.opsucht.net/merchant/rates");
		if (market.error() != null) UiDraw.text(g, market.error(), x, y + 64, Theme.NEGATIVE);

		// highlight cards: OPShards and Redcoins
		List<MerchantRate> rates = market.rates();
		int cw = col(w, 3, 6);
		MerchantRate bestShard = rates.stream().filter(r -> r.target().equalsIgnoreCase("opshards"))
				.max(Comparator.comparingDouble(MerchantRate::changePercent)).orElse(null);
		MerchantRate bestRed = rates.stream().filter(r -> r.target().equalsIgnoreCase("redcoins"))
				.max(Comparator.comparingDouble(MerchantRate::changePercent)).orElse(null);
		long shardCount = rates.stream().filter(r -> r.target().equalsIgnoreCase("opshards")).count();
		stat(g, x, y + 16, cw, 42, "Bester OPShards-Kurs", bestShard == null ? "–" : bestShard.displayName(), Theme.accent(),
				bestShard == null ? null : Fmt.decimal2(bestShard.exchangeRate()) + " (" + signed(bestShard.changePercent()) + " zur Basis)");
		stat(g, x + cw + 6, y + 16, cw, 42, "Bester Redcoins-Kurs", bestRed == null ? "–" : bestRed.displayName(), Theme.NEGATIVE,
				bestRed == null ? null : Fmt.decimal2(bestRed.exchangeRate()) + " (" + signed(bestRed.changePercent()) + " zur Basis)");
		stat(g, x + (cw + 6) * 2, y + 16, cw, 42, "Handelbare Rohstoffe", String.valueOf(rates.size()), Theme.TEXT,
				shardCount + " × OPShards · " + (rates.size() - shardCount) + " × Redcoins");
	}

	private static String signed(double v) {
		return (v >= 0 ? "+" : "") + Fmt.decimal(v) + "%";
	}
}
