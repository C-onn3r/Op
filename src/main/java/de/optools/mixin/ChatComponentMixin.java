package de.optools.mixin;

import de.optools.OpTools;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lets chat actions decorate every line before it is shown (system and player chat). */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component optools$decorate(Component message) {
		OpTools mod = OpTools.get();
		return mod == null ? message : mod.decorateChat(message);
	}
}
