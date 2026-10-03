package de.optools.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Applies style changes (click/hover events) to character ranges of a chat component while keeping all existing
 * styling. Ranges refer to the plain text without legacy § codes (see {@link #plain()}).
 */
public final class ComponentRewriter {
	private record Segment(Style style, String text) {
	}

	public record Edit(int start, int end, UnaryOperator<Style> style) {
	}

	private final List<Segment> segments = new ArrayList<>();
	private final String plain;

	public ComponentRewriter(Component component) {
		component.visit((style, text) -> {
			if (!text.isEmpty()) segments.add(new Segment(style, text));
			return Optional.empty();
		}, Style.EMPTY);
		StringBuilder sb = new StringBuilder();
		for (Segment s : segments) {
			String t = s.text;
			for (int i = 0; i < t.length(); i++) {
				char c = t.charAt(i);
				if (c == '§' && i + 1 < t.length()) {
					i++;
					continue;
				}
				sb.append(c);
			}
		}
		plain = sb.toString();
	}

	public String plain() {
		return plain;
	}

	/** Builds a new component with the edits applied. Overlapping edits are applied in list order. */
	public Component apply(List<Edit> edits) {
		MutableComponent root = Component.empty();
		int plainIndex = 0;
		for (Segment seg : segments) {
			String t = seg.text;
			StringBuilder chunk = new StringBuilder();
			String activeCodes = "";
			long chunkMask = -1;
			for (int i = 0; i < t.length(); i++) {
				char c = t.charAt(i);
				if (c == '§' && i + 1 < t.length()) {
					String code = t.substring(i, i + 2);
					activeCodes = isColorOrReset(t.charAt(i + 1)) ? code : activeCodes + code;
					chunk.append(code);
					i++;
					continue;
				}
				long mask = maskAt(edits, plainIndex);
				if (chunkMask != -1 && mask != chunkMask) {
					root.append(Component.literal(chunk.toString()).setStyle(styled(seg.style, edits, chunkMask)));
					// Carry legacy formatting into the next chunk, it does not cross component boundaries.
					chunk.setLength(0);
					chunk.append(activeCodes);
				}
				chunkMask = mask;
				chunk.append(c);
				plainIndex++;
			}
			if (!chunk.isEmpty()) {
				root.append(Component.literal(chunk.toString()).setStyle(styled(seg.style, edits, Math.max(chunkMask, 0))));
			}
		}
		return root;
	}

	private static boolean isColorOrReset(char code) {
		char c = Character.toLowerCase(code);
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r';
	}

	private static long maskAt(List<Edit> edits, int index) {
		long mask = 0;
		for (int i = 0; i < edits.size() && i < 63; i++) {
			Edit e = edits.get(i);
			if (index >= e.start && index < e.end) mask |= 1L << i;
		}
		return mask;
	}

	private static Style styled(Style base, List<Edit> edits, long mask) {
		Style s = base;
		for (int i = 0; i < edits.size() && i < 63; i++) {
			if ((mask & (1L << i)) != 0) s = edits.get(i).style.apply(s);
		}
		return s;
	}
}
