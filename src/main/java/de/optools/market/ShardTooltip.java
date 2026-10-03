package de.optools.market;

import de.optools.OpTools;
import de.optools.market.api.MerchantRate;
import de.optools.util.Fmt;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds the Rohstoffhändler rate ({@code /merchant/rates}) below the normal item tooltip. Plain items match by item
 * id; custom items (e.g. "Gräbergemisch" = paper with custom name/model data) match by name or custom model data.
 */
public final class ShardTooltip {
	private static final Pattern CMD_FLOATS = Pattern.compile("custom_model_data=\\{floats:\\s*\\[\\s*([0-9.]+)");

	private record Rule(String itemId, String name, Float modelData, MerchantRate rate) {
	}

	private static List<MerchantRate> indexedFrom;
	private static List<Rule> rules = List.of();

	private ShardTooltip() {
	}

	public static void register(OpTools mod) {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			try {
				append(mod, stack, lines);
			} catch (RuntimeException e) {
				OpTools.LOG.debug("Shard-Tooltip fehlgeschlagen", e);
			}
		});
	}

	private static void append(OpTools mod, ItemStack stack, List<Component> lines) {
		if (stack.isEmpty() || !mod.config().modules.market || !mod.config().market.tooltipRates) return;
		mod.market().refreshRates();
		List<MerchantRate> rates = mod.market().rates();
		if (rates.isEmpty()) return;
		if (rates != indexedFrom) rebuild(rates);

		MerchantRate rate = find(stack);
		if (rate == null) return;
		ChatFormatting color = rate.target().toLowerCase(Locale.ROOT).contains("shard") ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED;
		double change = rate.changePercent();
		lines.add(Component.empty());
		lines.add(Component.literal("Rohstoffhändler: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(Fmt.decimal2(rate.exchangeRate()) + " " + rate.targetLabel()).withStyle(color))
				.append(Component.literal(" /Stück").withStyle(ChatFormatting.DARK_GRAY)));
		if (stack.getCount() > 1) {
			lines.add(Component.literal("  ×" + stack.getCount() + " = ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(Fmt.decimal2(rate.exchangeRate() * stack.getCount()) + " " + rate.targetLabel()).withStyle(color)));
		}
		lines.add(Component.literal("  Basis " + Fmt.decimal2(rate.base()) + " (" + (change >= 0 ? "+" : "") + Fmt.decimal(change) + " %)")
				.withStyle(change >= 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED));
	}

	private static synchronized void rebuild(List<MerchantRate> rates) {
		List<Rule> list = new ArrayList<>();
		for (MerchantRate r : rates) {
			boolean custom = r.source().contains("[");
			Float cmd = null;
			Matcher m = CMD_FLOATS.matcher(r.source());
			if (m.find()) {
				try {
					cmd = Float.parseFloat(m.group(1));
				} catch (NumberFormatException ignored) {
				}
			}
			list.add(new Rule(r.itemId(), custom ? r.displayName().toLowerCase(Locale.ROOT) : null, cmd, r));
		}
		rules = list;
		indexedFrom = rates;
	}

	private static MerchantRate find(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		Float stackCmd = cmd != null && !cmd.floats().isEmpty() ? cmd.floats().get(0) : null;
		String name = null;
		Component custom = stack.get(DataComponents.CUSTOM_NAME);
		if (custom == null) custom = stack.get(DataComponents.ITEM_NAME);
		if (custom != null) name = custom.getString().strip().toLowerCase(Locale.ROOT);

		for (Rule rule : rules) {
			if (!rule.itemId.equals(id)) continue;
			if (rule.name == null) {
				// plain resource: custom items with the same base item are not traded
				if (stackCmd == null) return rule.rate;
			} else if ((name != null && name.equals(rule.name)) || (stackCmd != null && stackCmd.equals(rule.modelData))) {
				return rule.rate;
			}
		}
		return null;
	}
}
