package de.optools.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Drawing primitives for the custom look (soft-cornered panels, bars, badges). */
public final class UiDraw {
	private UiDraw() {
	}

	public static Font font() {
		return Minecraft.getInstance().font;
	}

	/** Rectangle with 1px cut corners – reads as slightly rounded at GUI scale. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h, int color) {
		if (w <= 2 || h <= 2) {
			g.fill(x, y, x + w, y + h, color);
			return;
		}
		g.fill(x + 1, y, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x + 1, y, x + w - 1, y + 1, color);
		g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void card(GuiGraphics g, int x, int y, int w, int h) {
		panel(g, x, y, w, h, Theme.PANEL_RAISED);
		outline(g, x, y, w, h, Theme.BORDER);
	}

	public static void progressBar(GuiGraphics g, int x, int y, int w, int h, double fraction, int color, int bg) {
		panel(g, x, y, w, h, bg);
		int fw = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
		if (fw > 0) {
			panel(g, x, y, Math.max(fw, 2), h, color);
			// subtle highlight
			g.fill(x + 1, y, x + Math.max(fw, 2) - 1, y + 1, Theme.withAlpha(0xFFFFFF, 60));
		}
	}

	public static void text(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(font(), s, x, y, color, false);
	}

	public static void textShadow(GuiGraphics g, String s, int x, int y, int color) {
		g.drawString(font(), s, x, y, color, true);
	}

	public static void textRight(GuiGraphics g, String s, int right, int y, int color) {
		g.drawString(font(), s, right - font().width(s), y, color, false);
	}

	public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
		g.drawString(font(), s, cx - font().width(s) / 2, y, color, false);
	}

	/** Truncates with "…" to fit {@code maxWidth}. */
	public static String ellipsize(String s, int maxWidth) {
		Font f = font();
		if (f.width(s) <= maxWidth) return s;
		String dots = "…";
		int dw = f.width(dots);
		int end = s.length();
		while (end > 0 && f.width(s.substring(0, end)) + dw > maxWidth) end--;
		return s.substring(0, end) + dots;
	}

	public static void badge(GuiGraphics g, String s, int x, int y, int color) {
		int w = font().width(s) + 8;
		panel(g, x, y, w, 11, Theme.withAlpha(color, 60));
		outline(g, x, y, w, 11, Theme.withAlpha(color, 160));
		text(g, s, x + 4, y + 2, color);
	}

	public static int badgeWidth(String s) {
		return font().width(s) + 8;
	}

	/** Multi-line tooltip. */
	public static void tooltip(GuiGraphics g, java.util.List<String> lines, int mx, int my) {
		java.util.List<net.minecraft.util.FormattedCharSequence> seq = new java.util.ArrayList<>();
		for (String line : lines) seq.add(net.minecraft.network.chat.Component.literal(line).getVisualOrderText());
		g.setTooltipForNextFrame(font(), seq, mx, my);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
