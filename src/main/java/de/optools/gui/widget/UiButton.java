package de.optools.gui.widget;

import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

public class UiButton extends UiElement {
	public enum Style {PRIMARY, SECONDARY, GHOST, DANGER}

	private final Supplier<String> label;
	private final Runnable action;
	private Style style;
	public boolean enabled = true;
	private String tooltip;

	public UiButton(String label, Style style, Runnable action) {
		this(() -> label, style, action);
	}

	public UiButton(Supplier<String> label, Style style, Runnable action) {
		this.label = label;
		this.style = style;
		this.action = action;
	}

	public UiButton tooltip(String tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	public UiButton style(Style style) {
		this.style = style;
		return this;
	}

	public static int widthFor(String label) {
		return UiDraw.font().width(label) + 16;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		if (!visible) return;
		boolean hover = enabled && isHovered(mx, my);
		int accent = Theme.accent();
		int bg, fg = Theme.TEXT, border = 0;
		switch (style) {
			case PRIMARY -> bg = hover ? Theme.mix(accent, 0xFFFFFFFF, 0.15f) : accent;
			case DANGER -> {
				bg = hover ? Theme.withAlpha(Theme.NEGATIVE, 120) : Theme.withAlpha(Theme.NEGATIVE, 60);
				border = Theme.withAlpha(Theme.NEGATIVE, 180);
			}
			case GHOST -> {
				bg = hover ? Theme.PANEL_HOVER : 0;
				fg = hover ? Theme.TEXT : Theme.MUTED;
			}
			default -> {
				bg = hover ? Theme.PANEL_HOVER : Theme.PANEL_RAISED;
				border = Theme.BORDER;
			}
		}
		if (!enabled) {
			bg = Theme.withAlpha(Theme.PANEL_RAISED, 160);
			fg = Theme.FAINT;
			border = Theme.BORDER;
		}
		if (bg != 0) UiDraw.panel(g, x, y, width, height, bg);
		if (border != 0) UiDraw.outline(g, x, y, width, height, border);
		String text = UiDraw.ellipsize(label.get(), width - 8);
		UiDraw.textCentered(g, text, x + width / 2, y + (height - 8) / 2, fg);
		if (tooltip != null && isHovered(mx, my)) {
			g.setTooltipForNextFrame(UiDraw.font(), net.minecraft.network.chat.Component.literal(tooltip), mx, my);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!visible || !enabled || button != 0 || !isHovered(mx, my)) return false;
		playClick();
		action.run();
		return true;
	}

	public static void playClick() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}
}
