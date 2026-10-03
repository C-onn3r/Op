package de.optools.config;

import java.util.LinkedHashSet;
import java.util.Set;

/** Position/appearance of a single HUD widget. Position is stored relative to the screen (0..1) to survive resizes. */
public final class HudWidgetConfig {
	public boolean visible = true;
	public double x = 0.01;
	public double y = 0.02;
	public float scale = 1.0f;
	/** Background opacity 0..1. */
	public float opacity = 0.65f;
	/** Ids of the lines/elements shown by this widget. */
	public Set<String> lines = new LinkedHashSet<>();
	/** Hide the widget if there is nothing to show (e.g. no running session). */
	public boolean hideWhenIdle = false;
}
