package de.optools.opsucht;

import de.optools.OpTools;
import de.optools.util.Json;

import java.io.IOException;
import java.nio.file.Path;

/** Loads {@code opsucht-patterns.json}, writes/upgrades the defaults and keeps the compiled patterns. */
public final class PatternRepository {
	private final Path file;
	private volatile OpsuchtPatterns current = OpsuchtPatterns.defaults();

	public PatternRepository(Path dir) {
		this.file = dir.resolve("opsucht-patterns.json");
	}

	public OpsuchtPatterns get() {
		return current;
	}

	public Path file() {
		return file;
	}

	public void load() {
		OpsuchtPatterns loaded = Json.read(file, OpsuchtPatterns.class, null);
		boolean writeDefaults = loaded == null
				|| (!loaded.customized && loaded.version < OpsuchtPatterns.DEFAULTS_VERSION);
		if (writeDefaults) {
			loaded = new OpsuchtPatterns();
			try {
				Json.write(file, loaded);
			} catch (IOException e) {
				OpTools.LOG.error("opsucht-patterns.json konnte nicht geschrieben werden", e);
			}
		}
		loaded.compile();
		for (String error : loaded.errors()) OpTools.LOG.warn("Ungültiges OPSUCHT-Pattern: {}", error);
		current = loaded;
	}
}
