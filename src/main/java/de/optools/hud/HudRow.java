package de.optools.hud;

/**
 * One rendered line of a HUD widget. A row is either a label/value pair or a progress bar
 * ({@code progress >= 0}).
 */
public record HudRow(String label, String value, int valueColor, double progress) {
	public static HudRow text(String label, String value, int color) {
		return new HudRow(label, value, color, -1);
	}

	public static HudRow bar(String label, double fraction) {
		return new HudRow(label, null, 0, Math.max(0, Math.min(1, fraction)));
	}

	public boolean isBar() {
		return progress >= 0;
	}
}
