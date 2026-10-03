package de.optools.gui.screen.tabs;

import de.optools.gui.ItemIcons;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.LineChart;
import de.optools.gui.widget.ScrollList;
import de.optools.gui.widget.UiButton;
import de.optools.gui.widget.UiChips;
import de.optools.market.api.HistoryPoint;
import de.optools.market.api.MarketItem;
import de.optools.market.api.OrderInfo;
import de.optools.util.Fmt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Marketplace prices from {@code /market/prices} with price history from {@code /market/history/{material}}. */
public final class MarketTab extends TabView {
	private static final String[] INTERVALS = {"HOURLY", "DAILY", "WEEKLY", "MONTHLY"};
	private static final List<String> INTERVAL_LABELS = List.of("Std.", "Tag", "Woche", "Monat");
	private static final int PRICE_COL = 50;
	private static String category = null;
	private static String search = "";
	private static String selectedMaterial;
	private static int interval;

	private ScrollList<MarketItem> list;
	private LineChart chart;
	private UiChips categoryChips;
	private List<String> categoryNames = List.of();
	private int lastItemCount = -1;
	private int detailX, detailW;

	public MarketTab(MainScreen screen) {
		super(screen);
	}

	@Override
	protected void build() {
		mod.market().refresh(false);
		int refreshW = UiButton.widthFor("Aktualisieren");
		add(new UiButton("Aktualisieren", UiButton.Style.SECONDARY, () -> mod.market().refresh(true)))
				.bounds(x + w - refreshW, y - 2, refreshW, 14);

		Set<String> cats = new LinkedHashSet<>();
		mod.market().categories().forEach(c -> cats.add(c.name()));
		mod.market().prices().forEach(i -> cats.add(i.category()));
		List<String> options = new ArrayList<>();
		options.add("Alle");
		options.addAll(cats);
		categoryNames = options;
		int sel = category == null ? 0 : Math.max(0, options.indexOf(category));
		categoryChips = add(new UiChips(options, sel, i -> {
			category = i == 0 ? null : categoryNames.get(i);
			updateList();
		}));
		int listW = (int) (w * (w < 400 ? 0.62 : 0.56));
		categoryChips.bounds(x, y + 16, w, 13);
		int chipsH = categoryChips.layout();

		int searchY = y + 16 + chipsH + 5;
		EditBox box = new EditBox(UiDraw.font(), x + 1, searchY, listW - 2, 14, Component.literal("Suche"));
		box.setHint(Component.literal("Item suchen…"));
		box.setValue(search);
		box.setResponder(s -> {
			search = s;
			updateList();
		});
		screen.addEditBox(box);

		int listTop = searchY + 19;
		list = add(new ScrollList<MarketItem>(18, (g, item, rx, ry, rw, rh, hover, mx, my) -> {
			g.renderItem(ItemIcons.stack(item.material()), rx + 3, ry + 1);
			UiDraw.text(g, UiDraw.ellipsize(ItemIcons.name(item.material()), rw - 24 - 2 * PRICE_COL), rx + 23, ry + 5, Theme.TEXT);
			UiDraw.textRight(g, price(item.buy()), rx + rw - 4 - PRICE_COL, ry + 5, Theme.POSITIVE);
			UiDraw.textRight(g, price(item.sell()), rx + rw - 4, ry + 5, Theme.WARNING);
		})).header(13, g -> {
			int hy = listTop() + 3;
			int scroll = list.items().size() * 18 > list.height - 13 ? 5 : 0;
			UiDraw.text(g, "Item", x + 24, hy, Theme.FAINT);
			UiDraw.textRight(g, "Kauf", x + listW - 5 - scroll - PRICE_COL, hy, Theme.FAINT);
			UiDraw.textRight(g, "Verkauf", x + listW - 5 - scroll, hy, Theme.FAINT);
		}).onClick(item -> {
			selectedMaterial = item.material();
			updateChart();
		}).emptyText(mod.market().loading() ? "Lade Marktdaten…" : "Keine Items gefunden");
		list.bounds(x, listTop, listW, y + h - listTop);

		detailX = x + listW + 6;
		detailW = w - listW - 6;
		UiChips intervalChips = add(new UiChips(INTERVAL_LABELS, interval, i -> {
			interval = i;
			updateChart();
		}));
		intervalChips.bounds(detailX, listTop + 58, detailW, 13);
		int ich = intervalChips.layout();
		chart = add(new LineChart("Preisverlauf Marktplatz"));
		chart.bounds(detailX, listTop + 58 + ich + 4, detailW, Math.max(60, y + h - (listTop + 58 + ich + 4)));
		updateList();
		updateChart();
	}

	private int listTop() {
		return list == null ? 0 : list.y;
	}

	private static String price(OrderInfo info) {
		return info == null || Double.isNaN(info.price()) ? "–" : Fmt.cash(info.price());
	}

	private void updateList() {
		if (list == null) return;
		String q = search.toLowerCase(Locale.ROOT).strip();
		List<MarketItem> items = new ArrayList<>();
		for (MarketItem item : mod.market().prices()) {
			if (category != null && !category.equals(item.category())) continue;
			if (!q.isEmpty() && !item.material().toLowerCase(Locale.ROOT).contains(q.replace(' ', '_'))
					&& !ItemIcons.name(item.material()).toLowerCase(Locale.ROOT).contains(q)) continue;
			items.add(item);
		}
		items.sort(Comparator.comparing(i -> ItemIcons.name(i.material())));
		list.items(items);
		lastItemCount = mod.market().prices().size();
		if (selectedMaterial != null) {
			items.stream().filter(i -> i.material().equals(selectedMaterial)).findFirst().ifPresent(list::select);
		}
	}

	private void updateChart() {
		if (chart == null) return;
		if (selectedMaterial == null) {
			chart.data(List.of()).band(List.of()).emptyText("Item links auswählen");
			return;
		}
		Map<String, List<HistoryPoint>> history = mod.market().history(selectedMaterial);
		if (history == null) {
			String err = mod.market().historyError(selectedMaterial);
			chart.data(List.of()).band(List.of()).emptyText(err != null ? "Fehler: " + err : "Lade Verlauf…");
			return;
		}
		List<HistoryPoint> points = history.getOrDefault(INTERVALS[interval], List.of());
		int max = interval == 0 ? 72 : 60;
		List<HistoryPoint> recent = points.subList(Math.max(0, points.size() - max), points.size());
		List<double[]> line = new ArrayList<>();
		List<double[]> band = new ArrayList<>();
		for (HistoryPoint p : recent) {
			long t = parseTime(p.timestamp());
			if (t < 0 || Double.isNaN(p.avgPrice())) continue;
			line.add(new double[]{t, p.avgPrice()});
			if (!Double.isNaN(p.minPrice()) && !Double.isNaN(p.maxPrice())) band.add(new double[]{t, p.minPrice(), p.maxPrice()});
		}
		boolean daily = interval > 0;
		chart.data(line).band(band).format(v -> daily ? Fmt.date((long) v) : Fmt.dateTime((long) v), v -> Fmt.compact(v) + " $")
				.emptyText("Kein Verlauf verfügbar");
	}

	private static long parseTime(String ts) {
		try {
			return LocalDateTime.parse(ts).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (Exception e) {
			return -1;
		}
	}

	private int historyTicks;

	@Override
	public void tick() {
		mod.market().refresh(false);
		if (mod.market().prices().size() != lastItemCount) {
			// first data arrived: rebuild to get categories
			if (lastItemCount <= 0 && categoryNames.size() <= 1) screen.refreshTab();
			else updateList();
		}
		if (++historyTicks % 10 == 0) updateChart();
	}

	@Override
	protected int titleSpace() {
		return w - UiButton.widthFor("Aktualisieren") - 6;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		var market = mod.market();
		String sub = market.loading() ? "lädt…" : market.pricesUpdated() > 0 ? "Stand " + Fmt.time(market.pricesUpdated()) + " · api.opsucht.net" : null;
		title(g, "Marktpreise", sub);
		if (market.error() != null) UiDraw.text(g, UiDraw.ellipsize(market.error(), w), x, y + h - 9, Theme.NEGATIVE);

		// detail card
		int top = list.y;
		UiDraw.card(g, detailX, top, detailW, 54);
		MarketItem item = list.selected();
		if (item == null) {
			UiDraw.textCentered(g, "Item auswählen für Details", detailX + detailW / 2, top + 23, Theme.FAINT);
			return;
		}
		g.renderItem(ItemIcons.stack(item.material()), detailX + 5, top + 5);
		UiDraw.text(g, UiDraw.ellipsize(ItemIcons.name(item.material()), detailW - 30), detailX + 25, top + 6, Theme.TEXT);
		UiDraw.text(g, UiDraw.ellipsize(item.category() + " · " + item.material(), detailW - 30), detailX + 25, top + 16, Theme.FAINT);
		int half = (detailW - 15) / 2;
		side(g, detailX + 5, top + 29, half, "BUY", item.buy(), Theme.POSITIVE);
		side(g, detailX + 10 + half, top + 29, half, "SELL", item.sell(), Theme.WARNING);
	}

	private static void side(GuiGraphics g, int x, int y, int w, String label, OrderInfo info, int color) {
		String head = info == null ? label : label + " · " + info.activeOrders() + " Orders";
		UiDraw.text(g, UiDraw.ellipsize(head, w), x, y, Theme.FAINT);
		UiDraw.text(g, UiDraw.ellipsize(info == null ? "–" : Fmt.money(info.price()), w), x, y + 10, color);
	}
}
