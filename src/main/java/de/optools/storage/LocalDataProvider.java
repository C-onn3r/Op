package de.optools.storage;

import com.google.gson.reflect.TypeToken;
import de.optools.storage.model.FinanceEntry;
import de.optools.storage.model.JobSessionRecord;
import de.optools.util.Json;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stores everything as JSON files in {@code config/optools/data/}. */
public final class LocalDataProvider implements DataProvider {
	public static final int SCHEMA_VERSION = 1;

	private final Path dir;
	private final Meta meta;

	public LocalDataProvider(Path dir) {
		this.dir = dir;
		Path metaFile = dir.resolve("meta.json");
		Meta loaded = Json.read(metaFile, Meta.class, null);
		if (loaded == null || loaded.deviceId == null || loaded.deviceId.isBlank()) {
			loaded = new Meta();
			loaded.deviceId = UUID.randomUUID().toString();
			loaded.createdAt = System.currentTimeMillis();
		}
		loaded.schemaVersion = SCHEMA_VERSION;
		this.meta = loaded;
		try {
			Json.write(metaFile, meta);
		} catch (IOException ignored) {
		}
	}

	@Override
	public String id() {
		return "local";
	}

	@Override
	public String displayName() {
		return "Lokal";
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public List<JobSessionRecord> loadJobSessions() {
		return Json.read(dir.resolve("job-sessions.json"), new TypeToken<ArrayList<JobSessionRecord>>() {
		}.getType(), new ArrayList<>());
	}

	@Override
	public List<FinanceEntry> loadFinanceEntries() {
		return Json.read(dir.resolve("finance.json"), new TypeToken<ArrayList<FinanceEntry>>() {
		}.getType(), new ArrayList<>());
	}

	@Override
	public void saveJobSessions(List<JobSessionRecord> sessions) throws IOException {
		Json.write(dir.resolve("job-sessions.json"), sessions);
	}

	@Override
	public void saveFinanceEntries(List<FinanceEntry> entries) throws IOException {
		Json.write(dir.resolve("finance.json"), entries);
	}

	@Override
	public String deviceId() {
		return meta.deviceId;
	}

	static final class Meta {
		int schemaVersion;
		String deviceId;
		long createdAt;
	}
}
