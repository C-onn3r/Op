package de.optools;

import de.optools.commands.CommandShortcut;
import de.optools.market.api.MerchantRate;
import de.optools.opsucht.OpsuchtPatterns;
import de.optools.opsucht.parse.ChatLine;
import de.optools.opsucht.parse.ChatLineParser;
import de.optools.opsucht.parse.JobActionbarParser;
import de.optools.opsucht.parse.JobGain;
import de.optools.opsucht.parse.PaymentEvent;
import de.optools.opsucht.parse.PaymentParser;
import de.optools.util.NumberParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
	private final OpsuchtPatterns patterns = OpsuchtPatterns.defaults();
	private final JobActionbarParser jobs = new JobActionbarParser(() -> patterns);
	private final PaymentParser payments = new PaymentParser(() -> patterns);
	private final ChatLineParser chat = new ChatLineParser(() -> patterns);

	@Test
	void numbers() {
		assertEquals(1234.56, NumberParser.parse("1.234,56"), 1e-9);
		assertEquals(1234.56, NumberParser.parse("1,234.56"), 1e-9);
		assertEquals(1000, NumberParser.parse("1.000"), 1e-9);
		assertEquals(12.5, NumberParser.parse("12,5"), 1e-9);
		assertEquals(3.2, NumberParser.parse("3.2"), 1e-9);
		assertEquals(2500, NumberParser.parse("2,5k"), 1e-9);
		assertEquals(3_000_000, NumberParser.parse("3 Mio"), 1e-9);
		assertEquals(1_250_000, NumberParser.parse("1.250.000"), 1e-9);
		assertEquals(42, NumberParser.parse("$42"), 1e-9);
		assertTrue(Double.isNaN(NumberParser.parse("abc")));
	}

	@Test
	void jobActionbarOpmodFormat() {
		JobGain g = jobs.parse("§6Holzfäller §8• §7Level 12 • XP: 4,5 • $1,20 • 37,5%").orElseThrow();
		assertEquals("Holzfäller", g.job());
		assertEquals(12, g.level());
		assertEquals(4.5, g.xp(), 1e-9);
		assertEquals(1.2, g.money(), 1e-9);
		assertEquals(37.5, g.progress(), 1e-9);
	}

	@Test
	void jobActionbarTolerantFormat() {
		JobGain g = jobs.parse("Minenarbeiter Lvl. 7 | +12 XP | +3,50 $ | 81%").orElseThrow();
		assertEquals("Minenarbeiter", g.job());
		assertEquals(7, g.level());
		assertEquals(12, g.xp(), 1e-9);
		assertEquals(3.5, g.money(), 1e-9);
		assertEquals(81, g.progress(), 1e-9);
	}

	@Test
	void jobActionbarCurrentOpsuchtFormat() {
		JobGain g = jobs.parse("+2.5 XP · +12.73$ · Holzfäller · Level 58 · [||||||||||          ] · 11.91%").orElseThrow();
		assertEquals("Holzfäller", g.job());
		assertEquals(58, g.level());
		assertEquals(2.5, g.xp(), 1e-9);
		assertEquals(12.73, g.money(), 1e-9);
		assertEquals(11.91, g.progress(), 1e-9);
	}

	@Test
	void jobFieldsAreOrderIndependent() {
		JobGain g = jobs.parse("Minenarbeiter · Level 3 · 45,5% · +1.250,50$ · +10 XP").orElseThrow();
		assertEquals("Minenarbeiter", g.job());
		assertEquals(3, g.level());
		assertEquals(10, g.xp(), 1e-9);
		assertEquals(1250.5, g.money(), 1e-9);
		assertEquals(45.5, g.progress(), 1e-9);
		// unknown job name right before "Level"
		JobGain u = jobs.parse("+1 XP · +0.50$ · Schmied · Level 2 · 3%").orElseThrow();
		assertEquals("Schmied", u.job());
	}

	@Test
	void strictModeRejectsPaymentsInChat() {
		assertTrue(jobs.parse("Du hast Steve 1.500$ überwiesen.", true).isEmpty());
		assertTrue(jobs.parse("Steve hat dir +50$ gegeben, Level 3", true).isEmpty());
		assertTrue(jobs.parse("+2.5 XP · +12.73$ · Holzfäller · Level 58 · 11.91%", true).isPresent());
	}

	@Test
	void jobActionbarIgnoresOtherText() {
		assertTrue(jobs.parse("Du hast keinen Platz im Inventar").isEmpty());
	}

	@Test
	void payments() {
		PaymentEvent out = payments.parse("Du hast Steve 1.500$ überwiesen.").orElseThrow();
		assertFalse(out.incoming());
		assertEquals("Steve", out.player());
		assertEquals(1500, out.amount(), 1e-9);

		PaymentEvent in = payments.parse("OPSUCHT » Alex hat dir $250,50 überwiesen").orElseThrow();
		assertTrue(in.incoming());
		assertEquals("Alex", in.player());
		assertEquals(250.5, in.amount(), 1e-9);

		PaymentEvent in2 = payments.parse("Du hast 2k $ von Notch erhalten").orElseThrow();
		assertTrue(in2.incoming());
		assertEquals(2000, in2.amount(), 1e-9);
	}

	@Test
	void fakePaymentFromPlayerIsRejected() {
		assertTrue(payments.parse("Griefer123 » Du hast Steve 1.500$ überwiesen").isEmpty());
		assertTrue(payments.parse("Spieler | Griefer123 » Alex hat dir 99999$ überwiesen").isEmpty());
	}

	@Test
	void chatLines() {
		ChatLine line = chat.parse("Spieler | Steve » verkaufe Elytra, schaut in mein /ah").orElseThrow();
		assertEquals("Steve", line.name());
		assertEquals("Steve", line.commandName());
		assertTrue(line.message().startsWith("verkaufe"));

		ChatLine nick = chat.parse("[VIP] ~Nicky » hi").orElseThrow();
		assertEquals("Nicky", nick.commandName());
		assertTrue(nick.isNick());

		assertTrue(chat.parse("OPSUCHT » Der Server startet neu").isEmpty());
		assertNotNull(patterns.auctionHint());
		assertTrue(patterns.auctionHint().matcher("schaut in mein /ah").find());
		assertFalse(patterns.auctionHint().matcher("/ahh").find());
	}

	@Test
	void merchantRate() {
		MerchantRate r = MerchantRate.of("minecraft:paper[custom_name={extra: [{bold: 1b, color: \"gray\", text: \"Gräbergemisch\"}], text: \"\"}]",
				"opshards", 21.0, 22.83);
		assertEquals("Gräbergemisch", r.displayName());
		assertEquals("minecraft:paper", r.itemId());
		assertEquals("OPShards", r.targetLabel());
		assertEquals(8.714, r.changePercent(), 0.01);
		assertEquals("Diamond Block", MerchantRate.of("diamond_block", "opshards", 7, 7.9).displayName());
	}

	@Test
	void shortcuts() {
		CommandShortcut s = new CommandShortcut("cb1", "/nav citybuild-1", "");
		assertEquals("nav citybuild-1", s.resolve(""));
		CommandShortcut p = new CommandShortcut("p", "pay {args}", "");
		assertEquals("pay Steve 10", p.resolve(" Steve 10"));
	}
}
