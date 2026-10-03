package de.optools.hud;

import de.optools.OpTools;
import de.optools.config.HudWidgetConfig;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.screen.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Registry, layout and renderer for all HUD widgets. */
public final class HudManager {
	private static final int PAD = 5;
	private static final int HEADER = 12;
	private static final int ROW = 10;
	private static final int BAR_ROW = 8;
	private static final int MIN_WIDTH = 96;

	private final OpTools mod;
	private final List<HudWidget> widgets = new ArrayList<>();

	public record Layout(int x, int y, int width, int height, float scale, List<HudRow> rows) {
		public int scaledWidth() {
			return Math.round(width * scale);
		}

		public int scaledHeight() {
			return Math.round(height * scale);
		}
	}

	public HudManager(OpTools mod) {
		this.mod = mod;
		register(new JobHudWidget());
		register(new FinanceHudWidget());
		register(new RtpHudWidget());
	}

	/** New widgets only need to be registered here. */
	public void register(HudWidget widget) {
		widgets.add(widget);
	}

	public List<HudWidget> widgets() {
		return widgets;
	}

	public boolean isWidgetEnabled(HudWidget w) {
		return mod.config().modules.hud && mod.config().modules.isEnabled(w.module()) && w.config().visible;
	}

	public void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
		if (!mod.config().modules.hud || !mod.isActiveServer()) return;
		for (HudWidget w : widgets) {
			if (!isWidgetEnabled(w)) continue;
			Layout layout = layout(w, false, g.guiWidth(), g.guiHeight());
			if (layout == null) continue;
			draw(g, w, layout, false);
		}
	}

	/** @return null if the widget has nothing to show. */
	public Layout layout(HudWidget w, boolean preview, int screenW, int screenH) {
		HudWidgetConfig cfg = w.config();
		List<HudRow> rows = w.rows(preview);
		if (rows.isEmpty() && !preview) {
			if (cfg.hideWhenIdle) return null;
			rows = List.of(HudRow.text("Warte auf Job-Aktivität…", "", Theme.MUTED));
			if (!(w instanceof JobHudWidget)) return null;
		}
		Font font = UiDraw.font();
		String status = w.headerStatus();
		int width = font.width(w.title()) + (status != null ? font.width(status) + 10 : 0) + 12;
		int height = PAD + HEADER;
		for (HudRow row : rows) {
			if (row.isBar()) {
				height += BAR_ROW;
				width = Math.max(width, font.width(row.label()) + 60);
			} else {
				height += ROW;
				width = Math.max(width, font.width(row.label()) + font.width(row.value()) + 14);
			}
		}
		width = Math.max(MIN_WIDTH, width + PAD * 2);
		height += PAD - 1;
		float scale = Math.max(0.5f, Math.min(2.5f, cfg.scale));
		int sw = Math.round(width * scale), sh = Math.round(height * scale);
		int x = (int) Math.round(cfg.x * screenW);
		int y = (int) Math.round(cfg.y * screenH);
		x = Math.max(0, Math.min(screenW - sw, x));
		y = Math.max(0, Math.min(screenH - sh, y));
		return new Layout(x, y, width, height, scale, rows);
	}

	public void draw(GuiGraphics g, HudWidget w, Layout l, boolean highlighted) {
		HudWidgetConfig cfg = w.config();
		int accent = Theme.accent();
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(l.x(), l.y());
		pose.scale(l.scale(), l.scale());

		int bg = Theme.withAlpha(0x10131A, Math.max(0f, Math.min(1f, cfg.opacity)));
		UiDraw.panel(g, 0, 0, l.width(), l.height(), bg);
		// accent strip on the left
		g.fill(0, 2, 2, l.height() - 2, accent);
		if (highlighted) UiDraw.outline(g, 0, 0, l.width(), l.height(), accent);

		UiDraw.textShadow(g, w.title(), PAD + 2, PAD, Theme.TEXT);
		String status = w.headerStatus();
		if (status != null) UiDraw.textRight(g, status, l.width() - PAD, PAD, Theme.WARNING);
		g.fill(PAD + 2, PAD + HEADER - 3, l.width() - PAD, PAD + HEADER - 2, Theme.withAlpha(0xFFFFFF, 28));

		int y = PAD + HEADER;
		for (HudRow row : l.rows()) {
			if (row.isBar()) {
				int labelW = UiDraw.font().width(row.label());
				UiDraw.progressBar(g, PAD + 2, y + 1, l.width() - PAD * 2 - labelW - 6, 4, row.progress(), accent,
						Theme.withAlpha(0xFFFFFF, 30));
				UiDraw.textRight(g, row.label(), l.width() - PAD, y - 1, Theme.MUTED);
				y += BAR_ROW;
			} else {
				UiDraw.textShadow(g, row.label(), PAD + 2, y, Theme.MUTED);
				if (!row.value().isEmpty()) {
					int vw = UiDraw.font().width(row.value());
					UiDraw.textShadow(g, row.value(), l.width() - PAD - vw, y, row.valueColor());
				}
				y += ROW;
			}
		}
		pose.popMatrix();
	}
}
