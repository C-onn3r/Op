package de.optools.gui.widget;

import de.optools.gui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;

/** Minimal retained-mode UI element used by all OP Tools screens. */
public abstract class UiElement {
	public int x, y, width, height;
	public boolean visible = true;

	public UiElement bounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		return this;
	}

	public abstract void render(GuiGraphics g, int mouseX, int mouseY, float delta);

	public boolean isHovered(double mx, double my) {
		return visible && UiDraw.inside(mx, my, x, y, width, height);
	}

	public boolean mouseClicked(double mx, double my, int button) {
		return false;
	}

	public boolean mouseDragged(double mx, double my, int button) {
		return false;
	}

	public boolean mouseReleased(double mx, double my, int button) {
		return false;
	}

	public boolean mouseScrolled(double mx, double my, double amount) {
		return false;
	}
}
