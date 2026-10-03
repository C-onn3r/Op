package de.optools.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.optools.OpTools;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Small JSON file helper with atomic writes, so a crash never leaves a half-written history file. */
public final class Json {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private Json() {
	}

	public static <T> T read(Path file, Type type, T fallback) {
		if (!Files.isRegularFile(file)) return fallback;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			T value = GSON.fromJson(reader, type);
			return value != null ? value : fallback;
		} catch (Exception e) {
			OpTools.LOG.error("Konnte {} nicht lesen – Sicherung wird angelegt", file, e);
			backupBroken(file);
			return fallback;
		}
	}

	public static void write(Path file, Object value) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			GSON.toJson(value, writer);
		}
		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void backupBroken(Path file) {
		try {
			Files.copy(file, file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ignored) {
		}
	}
}
