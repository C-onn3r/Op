package de.optools.gui.screen;

import de.optools.OpTools;
import de.optools.config.OpToolsConfig;
import de.optools.gui.Theme;
import de.optools.gui.UiDraw;
import de.optools.gui.widget.UiButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * First-start setup. "Lokal verwenden" is the working default; the self-hosted server option is visible but
 * clearly marked as Coming Soon and cannot be activated in 0.1 Alpha (the address is only remembered).
 */
public final class FirstStartScreen extends OpScreen {
	private EditBox addressBox;
	private int cardX, cardW, localY, remoteY, cardH;

	public FirstStartScreen(Screen parent) {
		super(Component.literal("OP Tools – Einrichtung"), parent);
	}

	@Override
	protected void init() {
		super.init();
		cardW = Math.min(340, width - 40);
		cardX = (width - cardW) / 2;
		cardH = 52;
		int top = Math.max(20, height / 2 - 95);
		localY = top + 42;
		remoteY = localY + cardH + 8;

		addressBox = new EditBox(font, cardX + 10, remoteY + 30, cardW - 20 - 90, 14, Component.literal("Server-Adresse"));
		addressBox.setHint(Component.literal("z. B. optools.example.de:8080"));
		addressBox.setMaxLength(253);
		addressBox.setValue(OpTools.get().config().storage.remoteAddress);
		addRenderableWidget(addressBox);

		UiButton connect = add(new UiButton("Verbinden", UiButton.Style.SECONDARY, () -> {
		}));
		connect.enabled = false;
		connect.tooltip("Coming Soon – in Version 0.1 Alpha noch nicht verfügbar");
		connect.bounds(cardX + cardW - 96, remoteY + 30, 86, 14);

		int bw = 160;
		add(new UiButton("Lokal verwenden  →", UiButton.Style.PRIMARY, this::finish))
				.bounds((width - bw) / 2, remoteY + cardH + 14, bw, 20);
	}

	private void finish() {
		OpTools mod = OpTools.get();
		OpToolsConfig cfg = mod.config();
		cfg.storage.mode = OpToolsConfig.DataMode.LOCAL;
		cfg.storage.remoteAddress = addressBox.getValue().strip();
		cfg.setupDone = true;
		mod.saveConfig();
		minecraft.setScreen(parent);
	}

	@Override
	protected void renderContent(GuiGraphics g, int mx, int my, float delta) {
		int accent = Theme.accent();
		int top = localY - 42;
		UiDraw.panel(g, width / 2 - 9, top, 18, 18, accent);
		UiDraw.textCentered(g, "OP", width / 2, top + 5, 0xFFFFFFFF);
		UiDraw.textCentered(g, "Willkommen bei OP Tools " + OpTools.VERSION, width / 2, top + 24, Theme.TEXT);
		UiDraw.textCentered(g, "Wie sollen deine Daten gespeichert werden?", width / 2, top + 34, Theme.MUTED);

		// local card (selected)
		UiDraw.panel(g, cardX, localY, cardW, cardH, Theme.withAlpha(accent, 40));
		UiDraw.outline(g, cardX, localY, cardW, cardH, accent);
		UiDraw.panel(g, cardX + 10, localY + 10, 9, 9, accent);
		UiDraw.panel(g, cardX + 12, localY + 12, 5, 5, 0xFFFFFFFF);
		UiDraw.text(g, "Lokal verwenden", cardX + 26, localY + 10, Theme.TEXT);
		UiDraw.badge(g, "EMPFOHLEN", cardX + cardW - UiDraw.badgeWidth("EMPFOHLEN") - 8, localY + 8, Theme.POSITIVE);
		UiDraw.text(g, "Job-Historie, Finanzbuch und Einstellungen bleiben auf", cardX + 26, localY + 24, Theme.MUTED);
		UiDraw.text(g, "diesem PC (config/optools). Keine Anmeldung nötig.", cardX + 26, localY + 35, Theme.MUTED);

		// remote card (coming soon)
		UiDraw.panel(g, cardX, remoteY, cardW, cardH, Theme.PANEL_RAISED);
		UiDraw.outline(g, cardX, remoteY, cardW, cardH, Theme.BORDER);
		UiDraw.outline(g, cardX + 10, remoteY + 10, 9, 9, Theme.FAINT);
		UiDraw.text(g, "Eigenen OP Tools Server verbinden", cardX + 26, remoteY + 10, Theme.MUTED);
		UiDraw.badge(g, "COMING SOON", cardX + cardW - UiDraw.badgeWidth("COMING SOON") - 8, remoteY + 8, Theme.WARNING);
		UiDraw.text(g, "IP / Domain deines selbst gehosteten Servers:", cardX + 26, remoteY + 20, Theme.FAINT);

		UiDraw.textCentered(g, "Kann später jederzeit unter Einstellungen geändert werden.", width / 2, remoteY + cardH + 40, Theme.FAINT);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
