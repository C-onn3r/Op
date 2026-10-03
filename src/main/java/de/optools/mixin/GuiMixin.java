package de.optools.mixin;

import de.optools.OpTools;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every actionbar text ends up in {@link Gui#setOverlayMessage}, regardless of the packet type the server uses. */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void optools$onOverlayMessage(Component message, boolean animateColor, CallbackInfo ci) {
		OpTools mod = OpTools.get();
		if (mod != null && message != null) {
			try {
				mod.onActionbar(message);
			} catch (RuntimeException e) {
				OpTools.LOG.debug("Actionbar konnte nicht ausgewertet werden", e);
			}
		}
	}
}
