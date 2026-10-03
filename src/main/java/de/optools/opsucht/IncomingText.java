package de.optools.opsucht;

import com.mojang.serialization.JsonOps;
import de.optools.OpTools;
import de.optools.opsucht.parse.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Records server texts per client path (actionbar, titles, boss bars, system chat). Used to find out on which path
 * OPSUCHT sends a piece of information ({@code /optools quellen}) and, optionally, to log everything into
 * {@code config/optools/debug/} ({@code /optools debuglog}).
 */
public final class IncomingText {
	public enum Source {
		ACTIONBAR("Actionbar"),
		TITLE("Titel"),
		SUBTITLE("Untertitel"),
		BOSSBAR("Bossbar"),
		SYSTEM_CHAT("System-Chat");

		public final String label;

		Source(String label) {
			this.label = label;
		}
	}

	/** @param count how often this exact text arrived in a row (e.g. 17 for a Timber-axe burst). */
	public record Entry(long time, Source source, String plain, int count) {
	}

	private static final int KEEP_PER_SOURCE = 8;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	private final Map<Source, Deque<Entry>> recent = new EnumMap<>(Source.class);
	private final Path dir;
	private ExecutorService writer;

	public IncomingText(Path dir) {
		this.dir = dir;
		for (Source s : Source.values()) recent.put(s, new ArrayDeque<>());
	}

	/**
	 * Stores the text; consecutive identical texts of one source are collapsed in the in-memory list (with a repeat
	 * counter). The file log contains every single packet.
	 */
	public void record(Source source, Component component, boolean logToFile) {
		String plain = TextUtil.stripFormatting(component.getString());
		if (plain.isBlank()) return;
		Deque<Entry> q = recent.get(source);
		synchronized (q) {
			Entry last = q.peekLast();
			if (last != null && last.plain.equals(plain)) {
				q.removeLast();
				q.addLast(new Entry(System.currentTimeMillis(), source, plain, last.count + 1));
			} else {
				q.addLast(new Entry(System.currentTimeMillis(), source, plain, 1));
			}
			while (q.size() > KEEP_PER_SOURCE) q.removeFirst();
		}
		if (logToFile) writeLine(source, plain, json(component));
	}

	public List<Entry> recent(Source source) {
		Deque<Entry> q = recent.get(source);
		synchronized (q) {
			return new ArrayList<>(q);
		}
	}

	public Path logFile() {
		return dir.resolve("incoming-" + LocalDate.now() + ".log");
	}

	private static String json(Component component) {
		try {
			Minecraft mc = Minecraft.getInstance();
			var ops = mc.level != null ? mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
			return ComponentSerialization.CODEC.encodeStart(ops, component).result().map(Object::toString).orElse("");
		} catch (RuntimeException e) {
			return "";
		}
	}

	private synchronized void writeLine(Source source, String plain, String json) {
		if (writer == null) {
			writer = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "OP-Tools-DebugLog");
				t.setDaemon(true);
				return t;
			});
		}
		String line = LocalTime.now().format(TIME) + " [" + source.name() + "] " + plain
				+ (json.isEmpty() ? "" : "\n    json: " + json) + "\n";
		Path file = logFile();
		writer.execute(() -> {
			try {
				Files.createDirectories(dir);
				Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				OpTools.LOG.warn("Debug-Log konnte nicht geschrieben werden: {}", e.getMessage());
			}
		});
	}
}
