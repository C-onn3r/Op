package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.IntConsumer;

/** Segmented single-choice selector ("chips"). Wraps into multiple lines if needed. */
public final class UiChips extends UiElement {
	private final List<String> options;
	private final IntConsumer onSelect;
	private int selected;
	private int[][] boxes = new int[0][];

	public UiChips(List<String> options, int selected, IntConsumer onSelect) {
		this.options = options;
		this.selected = selected;
		this.onSelect = onSelect;
	}

	public int selected() {
		return selected;
	}

	/** Computes chip positions; returns the total height used. */
	public int layout() {
		boxes = new int[options.size()][];
		int cx = x, cy = y;
		for (int i = 0; i < options.size(); i++) {
			int w = UiDraw.font().width(options.get(i)) + 12;
			if (cx + w > x + width && cx > x) {
				cx = x;
				cy += 15;
			}
			boxes[i] = new int[]{cx, cy, w, 13};
			cx += w + 4;
		}
		height = cy - y + 13;
		return height;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		if (boxes.length != options.size()) layout();
		int accent = Theme.accent();
		for (int i = 0; i < options.size(); i++) {
			int[] b = boxes[i];
			boolean sel = i == selected;
			boolean hover = UiDraw.inside(mx, my, b[0], b[1], b[2], b[3]);
			int bg = sel ? Theme.withAlpha(accent, 90) : hover ? Theme.PANEL_HOVER : Theme.PANEL_RAISED;
			UiDraw.panel(g, b[0], b[1], b[2], b[3], bg);
			UiDraw.outline(g, b[0], b[1], b[2], b[3], sel ? accent : Theme.BORDER);
			UiDraw.text(g, options.get(i), b[0] + 6, b[1] + 3, sel ? Theme.TEXT : Theme.MUTED);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0) return false;
		for (int i = 0; i < boxes.length; i++) {
			int[] b = boxes[i];
			if (UiDraw.inside(mx, my, b[0], b[1], b[2], b[3])) {
				if (i != selected) {
					selected = i;
					UiButton.playClick();
					onSelect.accept(i);
				}
				return true;
			}
		}
		return false;
	}
}
