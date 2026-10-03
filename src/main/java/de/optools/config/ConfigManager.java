package de.optools.config;

import de.optools.OpTools;
import de.optools.util.Json;

import java.io.IOException;
import java.nio.file.Path;

/** Loads and saves {@code config/optools/config.json}. */
public final class ConfigManager {
	private final Path file;
	private OpToolsConfig config;

	public ConfigManager(Path dir) {
		this.file = dir.resolve("config.json");
	}

	public OpToolsConfig load() {
		config = Json.read(file, OpToolsConfig.class, new OpToolsConfig());
		// Null-safety for partially written / older files.
		if (config.storage == null) config.storage = new OpToolsConfig.Storage();
		if (config.modules == null) config.modules = new OpToolsConfig.Modules();
		if (config.general == null) config.general = new OpToolsConfig.General();
		if (config.jobs == null) config.jobs = new OpToolsConfig.Jobs();
		if (config.finance == null) config.finance = new OpToolsConfig.Finance();
		if (config.market == null) config.market = new OpToolsConfig.Market();
		if (config.chat == null) config.chat = new OpToolsConfig.Chat();
		if (config.hud == null) config.hud = new java.util.LinkedHashMap<>();
		// 0.1 Alpha: the remote provider is not available yet.
		if (config.storage.mode == null || config.storage.mode == OpToolsConfig.DataMode.REMOTE) {
			config.storage.mode = OpToolsConfig.DataMode.LOCAL;
		}
		config.configVersion = OpToolsConfig.CURRENT_VERSION;
		return config;
	}

	public OpToolsConfig get() {
		return config;
	}

	public void save() {
		try {
			Json.write(file, config);
		} catch (IOException e) {
			OpTools.LOG.error("Konfiguration konnte nicht gespeichert werden", e);
		}
	}
}
