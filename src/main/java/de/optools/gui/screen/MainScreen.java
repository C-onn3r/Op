package de.optools.gui.screen;

import de.optools.OpTools;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.tabs.FinanceTab;
import de.optools.gui.screen.tabs.JobsTab;
import de.optools.gui.screen.tabs.MarketTab;
import de.optools.gui.screen.tabs.OverviewTab;
import de.optools.gui.screen.tabs.SettingsTab;
import de.optools.gui.screen.tabs.ShardsTab;
import de.optools.gui.screen.tabs.TabView;
import de.optools.gui.widget.UiButton;
import de.optools.gui.widget.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/** The OP Tools menu: sidebar navigation + tab content. */
public final class MainScreen extends OpScreen {
	public enum Tab {
		OVERVIEW("Übersicht", "◆", OverviewTab::new),
		JOBS("Job-Tracker", "⛏", JobsTab::new),
		FINANCE("Finanzbuch", "€", FinanceTab::new),
		MARKET("Marktpreise", "⚖", MarketTab::new),
		SHARDS("Shard-Kurse", "✦", ShardsTab::new),
		SETTINGS("Einstellungen", "⚙", SettingsTab::new);

		public final String title;
		public final String icon;
		final Function<MainScreen, TabView> factory;

		Tab(String title, String icon, Function<MainScreen, TabView> factory) {
			this.title = title;
			this.icon = icon;
			this.factory = factory;
		}
	}

	private static final int SIDEBAR = 112;
	private Tab tab;
	private TabView view;

	public MainScreen(Tab tab) {
		super(Component.literal("OP Tools"), null);
		this.tab = tab;
	}

	public Tab tab() {
		return tab;
	}

	public void switchTab(Tab tab) {
		if (this.tab == tab) return;
		this.tab = tab;
		rebuild();
	}

	public <T extends UiElement> T addElement(T element) {
		return add(element);
	}

	/** Rebuilds the current tab (e.g. after a filter change). */
	public void refreshTab() {
		rebuild();
	}

	@Override
	protected void init() {
		super.init();
		int y = 40;
		for (Tab t : Tab.values()) {
			UiButton b = new UiButton(() -> t.icon + "  " + t.title, t == tab ? UiButton.Style.SECONDARY : UiButton.Style.GHOST,
					() -> switchTab(t));
			b.bounds(8, y, SIDEBAR - 16, 18);
			add(b);
			y += 21;
		}
		view = tab.factory.apply(this);
		view.init(SIDEBAR + 10, 10, width - SIDEBAR - 20, height - 20);
	}

	@Override
	public void tick() {
		if (view != null) view.tick();
	}

	@Override
	protected void renderContent(GuiGraphics g, int mx, int my, float delta) {
		// sidebar
		g.fill(0, 0, SIDEBAR, height, Theme.PANEL);
		g.fill(SIDEBAR - 1, 0, SIDEBAR, height, Theme.BORDER);
		int accent = Theme.accent();
		UiDraw.panel(g, 10, 11, 16, 16, accent);
		UiDraw.textCentered(g, "OP", 18, 15, 0xFFFFFFFF);
		UiDraw.text(g, "OP Tools", 31, 11, Theme.TEXT);
		UiDraw.text(g, OpTools.VERSION, 31, 21, Theme.MUTED);

		// active tab marker
		int index = tab.ordinal();
		g.fill(4, 40 + index * 21 + 3, 6, 40 + index * 21 + 15, accent);

		// status
		OpTools mod = OpTools.get();
		boolean connected = mod.isActiveServer() && minecraft.getCurrentServer() != null;
		int sy = height - 34;
		g.fill(8, sy - 6, SIDEBAR - 8, sy - 5, Theme.BORDER);
		UiDraw.panel(g, 10, sy + 2, 5, 5, connected ? Theme.POSITIVE : Theme.FAINT);
		UiDraw.text(g, connected ? "OPSUCHT verbunden" : "Nicht auf OPSUCHT", 19, sy, connected ? Theme.TEXT : Theme.MUTED);
		UiDraw.text(g, "Daten: " + mod.dataStore().provider().displayName(), 10, sy + 12, Theme.FAINT);

		if (view != null) view.render(g, mx, my, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (getFocused() == null && OpTools.get().menuKey().matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		OpTools.get().saveConfig();
		super.onClose();
	}
}
