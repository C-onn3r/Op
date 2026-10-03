package de.optools;

import de.optools.opsucht.OpsuchtPatterns;
import de.optools.opsucht.parse.RtpEvent;
import de.optools.opsucht.parse.RtpParser;
import de.optools.rtp.RtpTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RtpParserTest {
	private final OpsuchtPatterns patterns = OpsuchtPatterns.defaults();
	private final RtpParser rtp = new RtpParser(() -> patterns);

	private RtpEvent p(String s) {
		return rtp.parse(s).orElseThrow(() -> new AssertionError("nicht erkannt: " + s));
	}

	@Test
	void queued() {
		RtpEvent e = p("RTP » Du wurdest für das Biom Dschungel angemeldet.");
		assertEquals(RtpEvent.Type.QUEUED, e.type());
		assertEquals("Dschungel", e.biome());
		e = p("Du wurdest zur Warteschlange für Biom: Pilzinsel hinzugefügt (Position #3)");
		assertEquals(RtpEvent.Type.QUEUED, e.type());
		assertEquals("Pilzinsel", e.biome());
		assertEquals(3, e.position());
		assertEquals(RtpEvent.Type.QUEUED, p("Du hast dich für den Biom-Teleport angemeldet").type());
		assertEquals(RtpEvent.Type.QUEUED, p("Suche nach einem passenden Ort im Biom Wüste...").type());
	}

	@Test
	void otherStates() {
		RtpEvent t = p("Du wurdest in das Biom Savanne teleportiert!");
		assertEquals(RtpEvent.Type.TELEPORTED, t.type());
		assertEquals("Savanne", t.biome());
		assertEquals(RtpEvent.Type.CANCELLED, p("Du hast dich vom Biom-Teleport abgemeldet.").type());
		RtpEvent c = p("Du wirst in 5 Sekunden teleportiert");
		assertEquals(RtpEvent.Type.COUNTDOWN, c.type());
		assertEquals(5, c.seconds());
		RtpEvent cd = p("Du kannst /rtp erst wieder in 2 Minuten nutzen.");
		assertEquals(RtpEvent.Type.COOLDOWN, cd.type());
		assertEquals(120, cd.seconds());
		RtpEvent pos = p("Warteschlange: Position 2");
		assertEquals(2, pos.position());
	}

	@Test
	void unrelatedLinesIgnored() {
		assertTrue(rtp.parse("Du hast Steve 1.500$ überwiesen.").isEmpty());
		assertTrue(rtp.parse("+2.5 XP · +12.73$ · Holzfäller · Level 58 · 11.91%").isEmpty());
	}

	@Test
	void trackerFlow() {
		long[] now = {1000};
		RtpTracker t = new RtpTracker(() -> now[0]);
		t.accept(p("Du wurdest für das Biom Dschungel angemeldet."));
		assertEquals(RtpTracker.Status.QUEUED, t.snapshot().status());
		assertEquals("Dschungel", t.snapshot().biome());
		now[0] += 2000;
		t.accept(p("Du wurdest in das Biom Dschungel teleportiert!"));
		assertEquals(RtpTracker.Status.TELEPORTED, t.snapshot().status());
		now[0] += 31_000;
		t.tick();
		assertEquals(RtpTracker.Status.IDLE, t.snapshot().status());
	}
}
