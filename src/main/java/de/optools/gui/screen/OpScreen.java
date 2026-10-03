package de.optools.gui.screen;

import de.optools.gui.Theme;
import de.optools.gui.widget.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Base screen: routes input to {@link UiElement}s, draws the OP Tools backdrop instead of the vanilla blur and
 * still supports vanilla widgets (used for text input).
 */
public abstract class OpScreen extends Screen {
	protected final Screen parent;
	protected final List<UiElement> elements = new ArrayList<>();

	protected OpScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	protected <T extends UiElement> T add(T element) {
		elements.add(element);
		return element;
	}

	public EditBox addEditBox(EditBox box) {
		return addRenderableWidget(box);
	}

	/** Clears our elements and vanilla widgets, then calls {@link #init()} again. */
	protected void rebuild() {
		elements.clear();
		clearWidgets();
		init();
	}

	@Override
	protected void init() {
		elements.clear();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// own stratum, so in-world HUD sprites (hotbar, hearts) are covered as well
		g.nextStratum();
		g.fill(0, 0, width, height, Theme.BACKDROP);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		renderContent(g, mouseX, mouseY, delta);
		for (UiElement e : elements) if (e.visible) e.render(g, mouseX, mouseY, delta);
		super.render(g, mouseX, mouseY, delta);
	}

	protected abstract void renderContent(GuiGraphics g, int mouseX, int mouseY, float delta);

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			UiElement e = elements.get(i);
			if (e.visible && e.mouseClicked(event.x(), event.y(), event.button())) {
				setFocused(null);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		for (UiElement e : elements) if (e.visible && e.mouseDragged(event.x(), event.y(), event.button())) return true;
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = false;
		for (UiElement e : elements) handled |= e.mouseReleased(event.x(), event.y(), event.button());
		return handled || super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			if (elements.get(i).visible && elements.get(i).mouseScrolled(mx, my, v)) return true;
		}
		return super.mouseScrolled(mx, my, h, v);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
