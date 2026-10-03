package de.optools.mixin;

import de.optools.OpTools;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/** Boss bar texts (servers often use them, with custom fonts, for HUD-like displays). */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
	@Shadow
	@Final
	Map<UUID, LerpingBossEvent> events;

	@Inject(method = "update", at = @At("TAIL"))
	private void optools$onBossUpdate(ClientboundBossEventPacket packet, CallbackInfo ci) {
		OpTools mod = OpTools.get();
		if (mod == null) return;
		try {
			for (Map.Entry<UUID, LerpingBossEvent> e : events.entrySet()) {
				mod.onBossbar(e.getKey(), e.getValue().getName());
			}
		} catch (RuntimeException ex) {
			OpTools.LOG.debug("Bossbar konnte nicht ausgewertet werden", ex);
		}
	}
}
