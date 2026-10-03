package de.optools.mixin;

import de.optools.OpTools;
import de.optools.opsucht.IncomingText;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every actionbar text ends up in {@link Gui#setOverlayMessage}, regardless of the packet type the server uses
 * (set-action-bar packet or system chat with overlay flag). Titles and subtitles are captured as well, because some
 * servers show HUD information there (moved with custom fonts).
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void optools$onOverlayMessage(Component message, boolean animateColor, CallbackInfo ci) {
		dispatch(IncomingText.Source.ACTIONBAR, message);
	}

	@Inject(method = "setTitle", at = @At("HEAD"))
	private void optools$onTitle(Component title, CallbackInfo ci) {
		dispatch(IncomingText.Source.TITLE, title);
	}

	@Inject(method = "setSubtitle", at = @At("HEAD"))
	private void optools$onSubtitle(Component subtitle, CallbackInfo ci) {
		dispatch(IncomingText.Source.SUBTITLE, subtitle);
	}

	private static void dispatch(IncomingText.Source source, Component message) {
		OpTools mod = OpTools.get();
		if (mod == null || message == null) return;
		try {
			mod.onIncoming(source, message);
		} catch (RuntimeException e) {
			OpTools.LOG.debug("{} konnte nicht ausgewertet werden", source, e);
		}
	}
}
