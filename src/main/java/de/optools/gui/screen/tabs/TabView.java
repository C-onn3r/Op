package de.optools.gui.screen.tabs;

import de.optools.OpTools;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.MainScreen;
import de.optools.gui.widget.UiElement;
import net.minecraft.client.gui.GuiGraphics;

/** Content of one tab of the {@link MainScreen}. */
public abstract class TabView {
	protected final MainScreen screen;
	protected final OpTools mod = OpTools.get();
	protected int x, y, w, h;

	protected TabView(MainScreen screen) {
		this.screen = screen;
	}

	public final void init(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		build();
	}

	protected abstract void build();

	/** Custom drawing below the elements. */
	public abstract void render(GuiGraphics g, int mx, int my, float delta);

	public void tick() {
	}

	protected <T extends UiElement> T add(T element) {
		return screen.addElement(element);
	}

	protected void title(GuiGraphics g, String title, String subtitle) {
		UiDraw.text(g, title, x, y + 2, Theme.TEXT);
		if (subtitle != null) UiDraw.text(g, subtitle, x + UiDraw.font().width(title) + 8, y + 2, Theme.FAINT);
	}

	/** Stat card: label on top, big value below, optional sub line. */
	protected static void stat(GuiGraphics g, int x, int y, int w, int h, String label, String value, int color, String sub) {
		UiDraw.card(g, x, y, w, h);
		UiDraw.text(g, UiDraw.ellipsize(label, w - 10), x + 6, y + 5, Theme.MUTED);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(x + 6, y + 16);
		pose.scale(1.35f, 1.35f);
		g.drawString(UiDraw.font(), UiDraw.ellipsize(value, (int) ((w - 12) / 1.35f)), 0, 0, color, false);
		pose.popMatrix();
		if (sub != null) UiDraw.text(g, UiDraw.ellipsize(sub, w - 10), x + 6, y + h - 12, Theme.FAINT);
	}

	/** Splits the width into {@code n} columns with {@code gap}. */
	protected static int col(int total, int n, int gap) {
		return (total - gap * (n - 1)) / n;
	}
}
