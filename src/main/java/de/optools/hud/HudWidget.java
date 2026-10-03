package de.optools.hud;

import de.optools.OpTools;
import de.optools.config.HudWidgetConfig;
import de.optools.config.ModuleId;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Base class for HUD widgets. A widget only provides data rows; layout and rendering (and dragging in the editor)
 * are handled centrally by {@link HudManager}, so all widgets share one look.
 */
public abstract class HudWidget {
	/** A selectable line of a widget. */
	public record LineOption(String id, String label, boolean defaultOn) {
	}

	public abstract String id();

	public abstract String title();

	/** Module that must be enabled for this widget to be shown. */
	public abstract ModuleId module();

	public abstract List<LineOption> lineOptions();

	/** @param preview true in the HUD editor: return example data if nothing real is available */
	public abstract List<HudRow> rows(boolean preview);

	/** Short status shown in the widget header (e.g. "pausiert"), or null. */
	public String headerStatus() {
		return null;
	}

	protected double defaultX() {
		return 0.005;
	}

	/** Top left by default: free in vanilla and away from the chat. */
	protected double defaultY() {
		return 0.02;
	}

	protected boolean defaultVisible() {
		return true;
	}

	public HudWidgetConfig config() {
		return OpTools.get().config().hud.computeIfAbsent(id(), k -> {
			HudWidgetConfig c = new HudWidgetConfig();
			c.x = defaultX();
			c.y = defaultY();
			c.visible = defaultVisible();
			c.lines = new LinkedHashSet<>();
			for (LineOption o : lineOptions()) if (o.defaultOn()) c.lines.add(o.id());
			return c;
		});
	}

	public boolean shows(String lineId) {
		return config().lines.contains(lineId);
	}
}
