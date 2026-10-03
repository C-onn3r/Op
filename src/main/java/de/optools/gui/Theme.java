package de.optools.gui;

import de.optools.OpTools;

/** Colour palette of the OP Tools UI (ARGB). */
public final class Theme {
	public static final int BACKDROP = 0xD80B0D12;
	public static final int PANEL = 0xF2161922;
	public static final int PANEL_RAISED = 0xFF1E2230;
	public static final int PANEL_HOVER = 0xFF262B3C;
	public static final int BORDER = 0xFF2C3246;
	public static final int TEXT = 0xFFE9ECF4;
	public static final int MUTED = 0xFF8C93A8;
	public static final int FAINT = 0xFF5A6178;
	public static final int POSITIVE = 0xFF3DDC97;
	public static final int NEGATIVE = 0xFFFF5C7A;
	public static final int WARNING = 0xFFFFB547;
	public static final int INFO = 0xFF5CC8FF;

	private Theme() {
	}

	public static int accent() {
		OpTools mod = OpTools.get();
		int rgb = mod == null ? 0x7C5CFF : mod.config().general.accentColor;
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
	}

	public static int withAlpha(int argb, float alpha) {
		return withAlpha(argb, Math.round(alpha * 255));
	}

	/** Linear blend of two colours. */
	public static int mix(int a, int b, float t) {
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
				| ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	/** Selectable accent colours (RGB). */
	public static final int[] ACCENTS = {0x7C5CFF, 0x3D8BFF, 0x00C2A8, 0x3DDC97, 0xFFB547, 0xFF6B5C, 0xFF5CB8};
	public static final String[] ACCENT_NAMES = {"Violett", "Blau", "Türkis", "Grün", "Gold", "Koralle", "Pink"};
}
