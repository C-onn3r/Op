package de.optools.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Resolves API material names (e.g. {@code DIAMOND_BLOCK}, {@code minecraft:paper}) to item stacks for icons/names. */
public final class ItemIcons {
	private static final Map<String, ItemStack> CACHE = new HashMap<>();

	private ItemIcons() {
	}

	public static ItemStack stack(String material) {
		return CACHE.computeIfAbsent(material, m -> {
			String id = m.toLowerCase(Locale.ROOT);
			Identifier identifier = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
			if (identifier == null) return new ItemStack(Items.BARRIER);
			Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.BARRIER);
			return new ItemStack(item == Items.AIR ? Items.BARRIER : item);
		});
	}

	/** Localised item name, falling back to a prettified material name. */
	public static String name(String material) {
		ItemStack stack = stack(material);
		if (stack.is(Items.BARRIER)) return de.optools.market.api.MerchantRate.prettify(material);
		return stack.getHoverName().getString();
	}
}
