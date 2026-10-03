package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/** Grouped bar chart (supports negative values). */
public final class BarChart extends UiElement {
	public record Bar(String label, double[] values) {
	}

	private final String title;
	private List<Bar> bars = List.of();
	private int[] colors = {0xFF7C5CFF};
	private String[] seriesNames = {""};
	private DoubleFunction<String> format = v -> String.valueOf((long) v);

	public BarChart(String title) {
		this.title = title;
	}

	public BarChart data(List<Bar> bars) {
		this.bars = bars == null ? List.of() : bars;
		return this;
	}

	public BarChart series(String[] names, int[] colors) {
		this.seriesNames = names;
		this.colors = colors;
		return this;
	}

	public BarChart format(DoubleFunction<String> format) {
		this.format = format;
		return this;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		UiDraw.card(g, x, y, width, height);
		// legend
		int lx = x + width - 6;
		for (int s = seriesNames.length - 1; s >= 0; s--) {
			if (seriesNames[s].isEmpty()) continue;
			int w = UiDraw.font().width(seriesNames[s]);
			lx -= w;
			UiDraw.text(g, seriesNames[s], lx, y + 5, Theme.MUTED);
			lx -= 9;
			g.fill(lx, y + 6, lx + 6, y + 12, colors[s % colors.length]);
			lx -= 6;
		}
		UiDraw.text(g, UiDraw.ellipsize(title, lx - x - 10), x + 6, y + 5, Theme.TEXT);
		int left = x + 40, right = x + width - 8, top = y + 18, bottom = y + height - 14;
		boolean any = bars.stream().anyMatch(b -> {
			for (double v : b.values) if (v != 0) return true;
			return false;
		});
		if (bars.isEmpty() || !any || right - left < 10) {
			UiDraw.textCentered(g, UiDraw.ellipsize("Noch keine Daten", width - 10), x + width / 2, y + height / 2 - 4, Theme.FAINT);
			return;
		}
		double min = 0, max = 0;
		for (Bar b : bars) for (double v : b.values) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		if (max - min < 1e-9) max = 1;
		for (int i = 0; i <= 2; i++) {
			int gy = bottom - (bottom - top) * i / 2;
			g.fill(left, gy, right, gy + 1, Theme.withAlpha(0xFFFFFF, 18));
			UiDraw.textRight(g, UiDraw.ellipsize(format.apply(min + (max - min) * i / 2.0), 36), left - 3, gy - 4, Theme.FAINT);
		}
		int zero = bottom - (int) Math.round((0 - min) / (max - min) * (bottom - top));
		g.fill(left, zero, right, zero + 1, Theme.withAlpha(0xFFFFFF, 60));

		int n = bars.size();
		double slot = Math.min(28, (right - left) / (double) n);
		int series = bars.get(0).values.length;
		int barW = Math.max(1, (int) ((slot - 2) / series));
		List<String> tip = null;
		for (int i = 0; i < n; i++) {
			Bar b = bars.get(i);
			int sx = left + (int) Math.round(i * slot) + 1;
			for (int s = 0; s < b.values.length; s++) {
				double v = b.values[s];
				int vy = bottom - (int) Math.round((v - min) / (max - min) * (bottom - top));
				int bx = sx + s * barW;
				g.fill(bx, Math.min(vy, zero), bx + Math.max(1, barW - 1), Math.max(vy, zero) + (v == 0 ? 0 : 1), colors[s % colors.length]);
			}
			if (mx >= sx && mx < sx + slot - 1 && my >= top && my <= bottom) {
				g.fill(sx - 1, top, (int) (sx + slot - 1), bottom, Theme.withAlpha(0xFFFFFF, 14));
				tip = new ArrayList<>();
				tip.add(b.label);
				for (int s = 0; s < b.values.length; s++) {
					String name = s < seriesNames.length && !seriesNames[s].isEmpty() ? seriesNames[s] + ": " : "";
					tip.add(name + format.apply(b.values[s]));
				}
			}
		}
		if (n > 0) {
			UiDraw.text(g, bars.get(0).label, left, bottom + 3, Theme.FAINT);
			String first = bars.get(0).label, last = bars.get(n - 1).label;
			int lastX = left + (int) Math.round(n * slot);
			if (n > 1 && UiDraw.font().width(first) + UiDraw.font().width(last) + 6 < lastX - left) {
				UiDraw.textRight(g, last, lastX, bottom + 3, Theme.FAINT);
			}
		}
		if (tip != null) {
			UiDraw.tooltip(g, tip, mx, my);
		}
	}
}
