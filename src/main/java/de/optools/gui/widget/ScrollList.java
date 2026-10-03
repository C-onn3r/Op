package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Consumer;

/** Scrollable list with fixed row height, optional header row and click selection. */
public final class ScrollList<T> extends UiElement {
	@FunctionalInterface
	public interface RowRenderer<T> {
		void render(GuiGraphics g, T item, int x, int y, int width, int height, boolean hovered, int mouseX, int mouseY);
	}

	private final int rowHeight;
	private final RowRenderer<T> renderer;
	private List<T> items = List.of();
	private Consumer<T> onClick = t -> {
	};
	private Consumer<GuiGraphics> header;
	private int headerHeight;
	private double scroll;
	private String emptyText = "Keine Einträge";
	private T selected;
	private boolean draggingBar;

	public ScrollList(int rowHeight, RowRenderer<T> renderer) {
		this.rowHeight = rowHeight;
		this.renderer = renderer;
	}

	public ScrollList<T> items(List<T> items) {
		this.items = items == null ? List.of() : items;
		clampScroll();
		return this;
	}

	public List<T> items() {
		return items;
	}

	public ScrollList<T> onClick(Consumer<T> onClick) {
		this.onClick = onClick;
		return this;
	}

	public ScrollList<T> header(int height, Consumer<GuiGraphics> header) {
		this.headerHeight = height;
		this.header = header;
		return this;
	}

	public ScrollList<T> emptyText(String text) {
		this.emptyText = text;
		return this;
	}

	public void select(T item) {
		this.selected = item;
	}

	public T selected() {
		return selected;
	}

	private int viewTop() {
		return y + headerHeight;
	}

	private int viewHeight() {
		return height - headerHeight;
	}

	private int contentHeight() {
		return items.size() * rowHeight;
	}

	private void clampScroll() {
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight() - viewHeight())));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		UiDraw.card(g, x, y, width, height);
		if (header != null) {
			header.accept(g);
			g.fill(x + 1, y + headerHeight - 1, x + width - 1, y + headerHeight, Theme.BORDER);
		}
		if (items.isEmpty()) {
			UiDraw.textCentered(g, emptyText, x + width / 2, viewTop() + viewHeight() / 2 - 4, Theme.FAINT);
			return;
		}
		clampScroll();
		boolean scrollable = contentHeight() > viewHeight();
		int rowWidth = width - 2 - (scrollable ? 5 : 0);
		g.enableScissor(x + 1, viewTop(), x + width - 1, y + height - 1);
		int first = (int) (scroll / rowHeight);
		for (int i = first; i < items.size(); i++) {
			int ry = viewTop() + i * rowHeight - (int) scroll;
			if (ry > y + height) break;
			T item = items.get(i);
			boolean hover = UiDraw.inside(mx, my, x + 1, ry, rowWidth, rowHeight) && my >= viewTop() && my < y + height;
			if (item == selected) g.fill(x + 1, ry, x + 1 + rowWidth, ry + rowHeight, Theme.withAlpha(Theme.accent(), 50));
			else if (hover) g.fill(x + 1, ry, x + 1 + rowWidth, ry + rowHeight, Theme.PANEL_HOVER);
			else if (i % 2 == 1) g.fill(x + 1, ry, x + 1 + rowWidth, ry + rowHeight, Theme.withAlpha(0xFFFFFF, 6));
			renderer.render(g, item, x + 1, ry, rowWidth, rowHeight, hover, mx, my);
		}
		g.disableScissor();
		if (scrollable) {
			int trackH = viewHeight() - 4;
			int thumbH = Math.max(12, trackH * viewHeight() / contentHeight());
			int thumbY = viewTop() + 2 + (int) ((trackH - thumbH) * (scroll / (contentHeight() - viewHeight())));
			g.fill(x + width - 5, viewTop() + 2, x + width - 2, viewTop() + 2 + trackH, Theme.withAlpha(0xFFFFFF, 12));
			g.fill(x + width - 5, thumbY, x + width - 2, thumbY + thumbH, Theme.withAlpha(Theme.accent(), 200));
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!isHovered(mx, my) || my < viewTop()) return false;
		if (contentHeight() > viewHeight() && mx >= x + width - 6) {
			draggingBar = true;
			scrollToMouse(my);
			return true;
		}
		int index = (int) ((my - viewTop() + scroll) / rowHeight);
		if (index >= 0 && index < items.size()) {
			selected = items.get(index);
			onClick.accept(selected);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button) {
		if (!draggingBar) return false;
		scrollToMouse(my);
		return true;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		boolean was = draggingBar;
		draggingBar = false;
		return was;
	}

	private void scrollToMouse(double my) {
		double t = (my - viewTop()) / Math.max(1, viewHeight());
		scroll = t * (contentHeight() - viewHeight());
		clampScroll();
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double amount) {
		if (!isHovered(mx, my)) return false;
		scroll -= amount * rowHeight * 2;
		clampScroll();
		return true;
	}
}
