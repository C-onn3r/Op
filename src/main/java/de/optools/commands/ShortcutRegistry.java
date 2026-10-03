package de.optools.commands;

import com.google.gson.reflect.TypeToken;
import de.optools.OpTools;
import de.optools.util.Json;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Loads command shortcuts from {@code config/optools/shortcuts.json}. Adding a shortcut is a one-line JSON
 * change (or a new entry in {@link #defaults()}).
 */
public final class ShortcutRegistry {
	private static final Pattern VALID_ALIAS = Pattern.compile("[a-z0-9_\\-]{1,32}");

	private final Path file;
	private List<CommandShortcut> shortcuts = new ArrayList<>();

	public ShortcutRegistry(Path dir) {
		this.file = dir.resolve("shortcuts.json");
	}

	/**
	 * Defaults. CityBuild targets use the navigator ({@code /nav <ziel>}, documented in the OPSUCHT wiki for e.g.
	 * {@code /nav luxury-island}); the exact CityBuild target names can be adjusted in shortcuts.json.
	 */
	public static List<CommandShortcut> defaults() {
		List<CommandShortcut> list = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			list.add(new CommandShortcut("cb" + i, "nav citybuild-" + i, "Wechselt auf CityBuild " + i));
		}
		list.add(new CommandShortcut("fw", "farm", "Öffnet den Farmwelt-Navigator"));
		list.add(new CommandShortcut("rw", "redstone", "Verbindet mit der Redstone-Welt"));
		list.add(new CommandShortcut("lux", "nav luxury-island", "Teleport zur Luxury Island"));
		return list;
	}

	public void load() {
		List<CommandShortcut> loaded = Json.read(file, new TypeToken<ArrayList<CommandShortcut>>() {
		}.getType(), null);
		if (loaded == null) {
			loaded = defaults();
			shortcuts = loaded;
			save();
		}
		List<CommandShortcut> valid = new ArrayList<>();
		for (CommandShortcut s : loaded) {
			if (s == null || s.alias == null || s.command == null) continue;
			s.alias = s.alias.toLowerCase(Locale.ROOT).replace("/", "").strip();
			if (!VALID_ALIAS.matcher(s.alias).matches()) {
				OpTools.LOG.warn("Ungültiger Kurzbefehl ignoriert: {}", s.alias);
				continue;
			}
			valid.add(s);
		}
		shortcuts = valid;
	}

	public void save() {
		try {
			Json.write(file, shortcuts);
		} catch (IOException e) {
			OpTools.LOG.error("shortcuts.json konnte nicht gespeichert werden", e);
		}
	}

	public List<CommandShortcut> all() {
		return shortcuts;
	}

	public void replaceAll(List<CommandShortcut> list) {
		shortcuts = new ArrayList<>(list);
		save();
	}

	public Path file() {
		return file;
	}
}
