package de.optools.gui.screen;

import de.optools.OpTools;
import de.optools.config.HudWidgetConfig;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.widget.UiButton;
import de.optools.hud.HudManager;
import de.optools.hud.HudWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag widgets to move them, scroll to resize, right-click to show/hide. */
public final class HudEditorScreen extends OpScreen {
	private HudWidget dragging;
	private double grabX, grabY;
	private HudWidget hovered;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD anpassen"), parent);
	}

	@Override
	protected void init() {
		super.init();
		int bw = 70;
		add(new UiButton("Fertig", UiButton.Style.PRIMARY, this::onClose)).bounds(width / 2 - bw - 3, height - 26, bw, 18);
		add(new UiButton("Zurücksetzen", UiButton.Style.SECONDARY, this::resetAll)).bounds(width / 2 + 3, height - 26, bw + 10, 18);
	}

	private void resetAll() {
		OpTools.get().config().hud.clear();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.nextStratum();
		g.fill(0, 0, width, height, 0x66000000);
		// grid
		for (int gx = 0; gx < width; gx += 20) g.fill(gx, 0, gx + 1, height, 0x10FFFFFF);
		for (int gy = 0; gy < height; gy += 20) g.fill(0, gy, width, gy + 1, 0x10FFFFFF);
	}

	@Override
	protected void renderContent(GuiGraphics g, int mx, int my, float delta) {
		HudManager hud = OpTools.get().hud();
		hovered = null;
		for (HudWidget w : hud.widgets()) {
			HudManager.Layout l = hud.layout(w, true, width, height);
			if (l == null) continue;
			boolean enabled = hud.isWidgetEnabled(w);
			boolean hover = UiDraw.inside(mx, my, l.x(), l.y(), l.scaledWidth(), l.scaledHeight());
			if (hover) hovered = w;
			hud.draw(g, w, l, hover || w == dragging);
			if (!enabled) {
				g.fill(l.x(), l.y(), l.x() + l.scaledWidth(), l.y() + l.scaledHeight(), 0x99000000);
				UiDraw.textCentered(g, "ausgeblendet", l.x() + l.scaledWidth() / 2, l.y() + l.scaledHeight() / 2 - 4, Theme.MUTED);
			}
		}
		String line1 = "Ziehen = verschieben · Mausrad = Größe · Shift+Mausrad = Deckkraft · Rechtsklick = ein/aus";
		String line2;
		if (hovered != null) {
			HudWidgetConfig c = hovered.config();
			line2 = hovered.title() + " · Größe " + Math.round(c.scale * 100) + " % · Deckkraft " + Math.round(c.opacity * 100) + " %";
		} else {
			line2 = "Weitere Optionen unter /optools → Einstellungen";
		}
		int maxW = width - 20;
		int pw = Math.min(maxW, Math.max(font.width(line1), font.width(line2)) + 16);
		UiDraw.panel(g, width / 2 - pw / 2, 8, pw, 24, Theme.PANEL);
		UiDraw.textCentered(g, UiDraw.ellipsize(line1, pw - 12), width / 2, 12, Theme.TEXT);
		UiDraw.textCentered(g, UiDraw.ellipsize(line2, pw - 12), width / 2, 22, hovered != null ? Theme.MUTED : Theme.FAINT);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		HudManager hud = OpTools.get().hud();
		for (HudWidget w : hud.widgets()) {
			HudManager.Layout l = hud.layout(w, true, width, height);
			if (l == null || !UiDraw.inside(event.x(), event.y(), l.x(), l.y(), l.scaledWidth(), l.scaledHeight())) continue;
			if (event.button() == 1) {
				w.config().visible = !w.config().visible;
				UiButton.playClick();
				return true;
			}
			if (event.button() == 0) {
				dragging = w;
				grabX = event.x() - l.x();
				grabY = event.y() - l.y();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		HudManager.Layout l = OpTools.get().hud().layout(dragging, true, width, height);
		if (l == null) return true;
		double nx = Math.max(0, Math.min(width - l.scaledWidth(), event.x() - grabX));
		double ny = Math.max(0, Math.min(height - l.scaledHeight(), event.y() - grabY));
		// snap to 4px grid for tidy alignment
		nx = Math.round(nx / 4) * 4;
		ny = Math.round(ny / 4) * 4;
		dragging.config().x = nx / width;
		dragging.config().y = ny / height;
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (hovered == null) return super.mouseScrolled(mx, my, h, v);
		HudWidgetConfig c = hovered.config();
		if (minecraft.hasShiftDown()) {
			c.opacity = (float) Math.max(0, Math.min(1, c.opacity + v * 0.05));
		} else {
			c.scale = (float) Math.max(0.5, Math.min(2.5, c.scale + v * 0.05));
		}
		return true;
	}

	@Override
	public void onClose() {
		OpTools.get().saveConfig();
		super.onClose();
	}
}
