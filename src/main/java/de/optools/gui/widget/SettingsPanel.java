package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** Scrollable settings form (headers, toggles, sliders, cycle buttons, actions, info text). */
public final class SettingsPanel extends UiElement {
	private final List<Row> rows = new ArrayList<>();
	private final Runnable onChange;
	private double scroll;
	private Row dragging;

	public SettingsPanel(Runnable onChange) {
		this.onChange = onChange;
	}

	// ---- builder ----

	public SettingsPanel header(String title) {
		rows.add(new HeaderRow(title));
		return this;
	}

	public SettingsPanel toggle(String label, String description, BooleanSupplier get, Consumer<Boolean> set) {
		rows.add(new ToggleRow(label, description, get, set));
		return this;
	}

	public SettingsPanel slider(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set,
								DoubleFunction<String> format) {
		rows.add(new SliderRow(label, min, max, step, get, set, format));
		return this;
	}

	public SettingsPanel cycle(String label, Supplier<String> value, Runnable next) {
		rows.add(new CycleRow(label, value, next));
		return this;
	}

	public SettingsPanel action(String label, String button, UiButton.Style style, Runnable action) {
		rows.add(new ActionRow(label, button, style, action));
		return this;
	}

	public SettingsPanel info(String text, int color) {
		rows.add(new InfoRow(text, color));
		return this;
	}

	public SettingsPanel spacer(int h) {
		rows.add(new SpacerRow(h));
		return this;
	}

	// ---- layout / render ----

	private int contentHeight() {
		int h = 4;
		for (Row r : rows) h += r.height(width - 16);
		return h;
	}

	private void clamp() {
		scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight() - height)));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		clamp();
		g.enableScissor(x, y, x + width, y + height);
		int ry = y + 4 - (int) scroll;
		int rw = width - 16;
		for (Row r : rows) {
			int h = r.height(rw);
			r.top = ry;
			r.left = x + 6;
			r.width = rw;
			if (ry + h > y && ry < y + height) r.render(g, x + 6, ry, rw, h, mx, my, UiDraw.inside(mx, my, x, y, width, height));
			ry += h;
		}
		g.disableScissor();
		int ch = contentHeight();
		if (ch > height) {
			int thumbH = Math.max(14, height * height / ch);
			int thumbY = y + (int) ((height - thumbH) * (scroll / (ch - height)));
			g.fill(x + width - 4, y, x + width - 1, y + height, Theme.withAlpha(0xFFFFFF, 12));
			g.fill(x + width - 4, thumbY, x + width - 1, thumbY + thumbH, Theme.withAlpha(Theme.accent(), 200));
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!isHovered(mx, my) || button != 0) return false;
		for (Row r : rows) {
			if (my >= r.top && my < r.top + r.height(r.width) && r.click(mx, my)) {
				if (r instanceof SliderRow) dragging = r;
				onChange.run();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button) {
		if (dragging instanceof SliderRow s) {
			s.setFromMouse(mx);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (dragging != null) {
			dragging = null;
			onChange.run();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double amount) {
		if (!isHovered(mx, my)) return false;
		scroll -= amount * 20;
		clamp();
		return true;
	}

	// ---- rows ----

	private abstract static class Row {
		int top, left, width;

		abstract int height(int width);

		abstract void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean panelHovered);

		boolean click(double mx, double my) {
			return false;
		}
	}

	private static final class SpacerRow extends Row {
		final int h;

		SpacerRow(int h) {
			this.h = h;
		}

		int height(int w) {
			return h;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
		}
	}

	private static final class HeaderRow extends Row {
		final String title;

		HeaderRow(String title) {
			this.title = title;
		}

		int height(int w) {
			return 22;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			g.fill(x, y + 8, x + 2, y + 18, Theme.accent());
			UiDraw.text(g, title.toUpperCase(java.util.Locale.ROOT), x + 7, y + 9, Theme.TEXT);
			g.fill(x + 12 + UiDraw.font().width(title.toUpperCase(java.util.Locale.ROOT)), y + 13, x + w, y + 14, Theme.BORDER);
		}
	}

	private static final class InfoRow extends Row {
		final String text;
		final int color;

		InfoRow(String text, int color) {
			this.text = text;
			this.color = color;
		}

		List<FormattedCharSequence> lines(int w) {
			return UiDraw.font().split(Component.literal(text), Math.max(20, w - 4));
		}

		int height(int w) {
			return lines(w).size() * 10 + 4;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			int ly = y + 2;
			for (FormattedCharSequence line : lines(w)) {
				g.drawString(UiDraw.font(), line, x + 2, ly, color, false);
				ly += 10;
			}
		}
	}

	private static final class ToggleRow extends Row {
		final String label, description;
		final BooleanSupplier get;
		final Consumer<Boolean> set;

		ToggleRow(String label, String description, BooleanSupplier get, Consumer<Boolean> set) {
			this.label = label;
			this.description = description;
			this.get = get;
			this.set = set;
		}

		int height(int w) {
			return description == null || description.isEmpty() ? 20 : 28;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			boolean hover = ph && UiDraw.inside(mx, my, x, y, w, h);
			if (hover) UiDraw.panel(g, x - 2, y, w + 4, h - 2, Theme.withAlpha(0xFFFFFF, 10));
			UiDraw.text(g, label, x + 2, y + 5, Theme.TEXT);
			if (description != null && !description.isEmpty()) {
				UiDraw.text(g, UiDraw.ellipsize(description, w - 40), x + 2, y + 16, Theme.FAINT);
			}
			boolean on = get.getAsBoolean();
			int sw = 22, sh = 11, sx = x + w - sw - 2, sy = y + (h - 2 - sh) / 2;
			UiDraw.panel(g, sx, sy, sw, sh, on ? Theme.accent() : Theme.withAlpha(0xFFFFFF, 40));
			int knob = on ? sx + sw - 10 : sx + 1;
			UiDraw.panel(g, knob, sy + 1, 9, sh - 2, Theme.TEXT);
		}

		boolean click(double mx, double my) {
			set.accept(!get.getAsBoolean());
			UiButton.playClick();
			return true;
		}
	}

	private static final class SliderRow extends Row {
		final String label;
		final double min, max, step;
		final DoubleSupplier get;
		final DoubleConsumer set;
		final DoubleFunction<String> format;

		SliderRow(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> format) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
			this.format = format;
		}

		int height(int w) {
			return 26;
		}

		int trackX() {
			return left + 2;
		}

		int trackW() {
			return width - 4;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			double v = get.getAsDouble();
			UiDraw.text(g, label, x + 2, y + 3, Theme.TEXT);
			UiDraw.textRight(g, format.apply(v), x + w - 2, y + 3, Theme.accent());
			double t = (v - min) / (max - min);
			UiDraw.progressBar(g, x + 2, y + 15, w - 4, 4, t, Theme.accent(), Theme.withAlpha(0xFFFFFF, 30));
			int kx = x + 2 + (int) Math.round(t * (w - 4));
			UiDraw.panel(g, kx - 3, y + 13, 7, 8, Theme.TEXT);
		}

		boolean click(double mx, double my) {
			setFromMouse(mx);
			return true;
		}

		void setFromMouse(double mx) {
			double t = Math.max(0, Math.min(1, (mx - trackX()) / Math.max(1, trackW())));
			double v = min + t * (max - min);
			v = Math.round(v / step) * step;
			set.accept(Math.max(min, Math.min(max, v)));
		}
	}

	private static final class CycleRow extends Row {
		final String label;
		final Supplier<String> value;
		final Runnable next;

		CycleRow(String label, Supplier<String> value, Runnable next) {
			this.label = label;
			this.value = value;
			this.next = next;
		}

		int height(int w) {
			return 20;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			UiDraw.text(g, label, x + 2, y + 5, Theme.TEXT);
			String v = value.get() + "  ›";
			int bw = UiDraw.font().width(v) + 12;
			boolean hover = ph && UiDraw.inside(mx, my, x + w - bw, y + 1, bw, 15);
			UiDraw.panel(g, x + w - bw, y + 1, bw, 15, hover ? Theme.PANEL_HOVER : Theme.PANEL_RAISED);
			UiDraw.outline(g, x + w - bw, y + 1, bw, 15, Theme.BORDER);
			UiDraw.text(g, v, x + w - bw + 6, y + 5, Theme.accent());
		}

		boolean click(double mx, double my) {
			next.run();
			UiButton.playClick();
			return true;
		}
	}

	private static final class ActionRow extends Row {
		final String label, button;
		final UiButton.Style style;
		final Runnable action;

		ActionRow(String label, String button, UiButton.Style style, Runnable action) {
			this.label = label;
			this.button = button;
			this.style = style;
			this.action = action;
		}

		int height(int w) {
			return 20;
		}

		int buttonWidth() {
			return UiDraw.font().width(button) + 14;
		}

		void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean ph) {
			UiDraw.text(g, UiDraw.ellipsize(label, w - buttonWidth() - 8), x + 2, y + 5, Theme.TEXT);
			int bw = buttonWidth();
			UiButton b = new UiButton(button, style, () -> {
			});
			b.bounds(x + w - bw, y + 1, bw, 15);
			b.render(g, ph ? mx : -1, ph ? my : -1, 0);
		}

		boolean click(double mx, double my) {
			int bw = buttonWidth();
			if (mx >= left + width - bw) {
				UiButton.playClick();
				action.run();
				return true;
			}
			return false;
		}
	}
}
