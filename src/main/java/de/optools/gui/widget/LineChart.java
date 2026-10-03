package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/** Line chart with optional min/max band, area fill, grid and hover read-out. Points are (x, y) pairs. */
public final class LineChart extends UiElement {
	private final String title;
	private List<double[]> points = List.of();
	private List<double[]> band = List.of();
	private DoubleFunction<String> yFormat = v -> String.valueOf((long) v);
	private DoubleFunction<String> xFormat = v -> "";
	private int color;
	private String emptyText = "Noch keine Daten";

	public LineChart(String title) {
		this.title = title;
	}

	public LineChart data(List<double[]> points) {
		this.points = points == null ? List.of() : points;
		return this;
	}

	/** Optional band: entries (x, low, high). */
	public LineChart band(List<double[]> band) {
		this.band = band == null ? List.of() : band;
		return this;
	}

	public LineChart format(DoubleFunction<String> x, DoubleFunction<String> y) {
		this.xFormat = x;
		this.yFormat = y;
		return this;
	}

	public LineChart color(int color) {
		this.color = color;
		return this;
	}

	public LineChart emptyText(String text) {
		this.emptyText = text;
		return this;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		UiDraw.card(g, x, y, width, height);
		UiDraw.text(g, title, x + 6, y + 5, Theme.TEXT);
		int lineColor = color != 0 ? color : Theme.accent();

		int left = x + 40, right = x + width - 8, top = y + 18, bottom = y + height - 14;
		if (points.size() < 2 || right - left < 10 || bottom - top < 10) {
			UiDraw.textCentered(g, emptyText, x + width / 2, y + height / 2 - 4, Theme.FAINT);
			return;
		}
		double minX = points.get(0)[0], maxX = points.get(points.size() - 1)[0];
		double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (double[] p : points) {
			minY = Math.min(minY, p[1]);
			maxY = Math.max(maxY, p[1]);
		}
		for (double[] b : band) {
			minY = Math.min(minY, b[1]);
			maxY = Math.max(maxY, b[2]);
		}
		if (maxY - minY < 1e-9) {
			maxY += 1;
			minY -= 1;
		}
		double pad = (maxY - minY) * 0.08;
		minY -= pad;
		maxY += pad;
		if (maxX - minX < 1e-9) maxX = minX + 1;

		// grid + y labels
		for (int i = 0; i <= 3; i++) {
			int gy = bottom - (bottom - top) * i / 3;
			g.fill(left, gy, right, gy + 1, Theme.withAlpha(0xFFFFFF, 18));
			double v = minY + (maxY - minY) * i / 3.0;
			UiDraw.textRight(g, UiDraw.ellipsize(yFormat.apply(v), 36), left - 3, gy - 4, Theme.FAINT);
		}
		// zero line
		if (minY < 0 && maxY > 0) {
			int zy = toY(0, minY, maxY, top, bottom);
			g.fill(left, zy, right, zy + 1, Theme.withAlpha(0xFFFFFF, 50));
		}
		// x labels (start / end)
		UiDraw.text(g, xFormat.apply(minX), left, bottom + 3, Theme.FAINT);
		UiDraw.textRight(g, xFormat.apply(maxX), right, bottom + 3, Theme.FAINT);

		// band
		if (band.size() >= 2) {
			for (int i = 0; i < band.size() - 1; i++) {
				double[] a = band.get(i), b = band.get(i + 1);
				int x1 = toX(a[0], minX, maxX, left, right), x2 = toX(b[0], minX, maxX, left, right);
				for (int px = x1; px <= x2; px++) {
					double t = x2 == x1 ? 0 : (px - x1) / (double) (x2 - x1);
					int lo = toY(a[1] + (b[1] - a[1]) * t, minY, maxY, top, bottom);
					int hi = toY(a[2] + (b[2] - a[2]) * t, minY, maxY, top, bottom);
					g.fill(px, Math.min(lo, hi), px + 1, Math.max(lo, hi) + 1, Theme.withAlpha(lineColor, 40));
				}
			}
		}

		// line with area fill
		int baseY = toY(Math.max(minY, Math.min(maxY, 0)), minY, maxY, top, bottom);
		int prevY = Integer.MIN_VALUE;
		List<int[]> screenPts = new ArrayList<>(points.size());
		for (double[] p : points) screenPts.add(new int[]{toX(p[0], minX, maxX, left, right), toY(p[1], minY, maxY, top, bottom)});
		for (int i = 0; i < screenPts.size() - 1; i++) {
			int[] a = screenPts.get(i), b = screenPts.get(i + 1);
			for (int px = a[0]; px <= b[0]; px++) {
				double t = b[0] == a[0] ? 1 : (px - a[0]) / (double) (b[0] - a[0]);
				int py = (int) Math.round(a[1] + (b[1] - a[1]) * t);
				if (band.isEmpty()) {
					g.fill(px, Math.min(py, baseY), px + 1, Math.max(py, baseY), Theme.withAlpha(lineColor, 34));
				}
				int from = prevY == Integer.MIN_VALUE ? py : Math.min(prevY, py);
				int to = prevY == Integer.MIN_VALUE ? py : Math.max(prevY, py);
				g.fill(px, from, px + 1, to + 2, lineColor);
				prevY = py;
			}
		}

		// hover
		if (UiDraw.inside(mx, my, left, top, right - left + 1, bottom - top)) {
			int best = 0;
			for (int i = 1; i < screenPts.size(); i++) {
				if (Math.abs(screenPts.get(i)[0] - mx) < Math.abs(screenPts.get(best)[0] - mx)) best = i;
			}
			int[] sp = screenPts.get(best);
			g.fill(sp[0], top, sp[0] + 1, bottom, Theme.withAlpha(0xFFFFFF, 60));
			g.fill(sp[0] - 2, sp[1] - 2, sp[0] + 3, sp[1] + 3, Theme.TEXT);
			double[] p = points.get(best);
			g.setTooltipForNextFrame(UiDraw.font(), Component.literal(xFormat.apply(p[0]) + ": " + yFormat.apply(p[1])), mx, my);
		}
	}

	private static int toX(double v, double min, double max, int left, int right) {
		return left + (int) Math.round((v - min) / (max - min) * (right - left));
	}

	private static int toY(double v, double min, double max, int top, int bottom) {
		return bottom - (int) Math.round((v - min) / (max - min) * (bottom - top));
	}
}
