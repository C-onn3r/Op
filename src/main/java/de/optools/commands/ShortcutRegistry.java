package de.optools.commands;

import com.google.gson.reflect.TypeToken;
import de.optools.OpTools;
import de.optools.util.Json;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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

	/** Defaults. CityBuilds are reached via the navigator: {@code /cb1} sends {@code /nav cb1}. */
	public static List<CommandShortcut> defaults() {
		List<CommandShortcut> list = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			list.add(new CommandShortcut("cb" + i, "nav cb" + i, "Wechselt auf CityBuild " + i));
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
		if (migrate(loaded)) {
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

	/** 0.1 Alpha shipped wrong CityBuild targets ("nav citybuild-1"); fix them in existing files. */
	private static boolean migrate(List<CommandShortcut> list) {
		boolean changed = false;
		for (CommandShortcut s : list) {
			if (s == null || s.command == null) continue;
			Matcher m = OLD_CB_TARGET.matcher(s.command.strip());
			if (m.matches()) {
				s.command = "nav cb" + m.group(1);
				changed = true;
			}
		}
		return changed;
	}

	private static final Pattern OLD_CB_TARGET = Pattern.compile("/?nav citybuild-([0-9]+)");

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
