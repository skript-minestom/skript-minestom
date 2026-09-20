package ch.njol.skript.util;

import net.minestom.server.component.DataComponents;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.registry.RegistryKey;

import java.util.*;

public record Enchantment(RegistryKey<net.minestom.server.item.enchant.Enchantment> enchantment, int level) {

	public Enchantment {
		level = Math.min(level, 255);
	}

	public static void add(Item to, boolean notify, Enchantment... enchants) {
		EnchantmentList enchantmentList = to.getItem().get(DataComponents.ENCHANTMENTS);
		Map<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> map;
		if (enchantmentList == null) map = new HashMap<>();
		else map = new HashMap<>(enchantmentList.enchantments());
		put(map, enchants);
		to.modify(i -> i.with(DataComponents.ENCHANTMENTS, new EnchantmentList(map)), notify);
	}

	public static void set(Item to, Enchantment... enchants) {
		to.modify(i -> i.with(DataComponents.ENCHANTMENTS, new EnchantmentList(toMap(enchants))), true);
	}

	public static void remove(Item from, Enchantment... enchants) {
		Enchantment[] currentEnchants = getEnchants(from);
		List<Enchantment> finalEnchants = new ArrayList<>(List.of(currentEnchants));
		for (Enchantment currentEnchant : currentEnchants) {
			for (Enchantment enchant : enchants) {
				if (!currentEnchant.enchantment.equals(enchant.enchantment)) continue;
				int level = enchant.level;
				if (enchant.level == -1 || level == currentEnchant.level) {
					finalEnchants.remove(currentEnchant);
					break;
				}
			}
		}
		set(from, finalEnchants.toArray(new Enchantment[0]));
	}

	public static Enchantment[] getEnchants(Item item) {
		EnchantmentList enchantmentList = item.getItem().get(DataComponents.ENCHANTMENTS);
		if (enchantmentList == null) return new Enchantment[0];
		Map<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> enchantments = enchantmentList.enchantments();
		Enchantment[] enchants = new Enchantment[enchantments.size()];
		int index = 0;
		for (Map.Entry<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> entry : enchantments.entrySet()) {
			enchants[index] = new Enchantment(entry.getKey(), entry.getValue());
			index++;
		}
		return enchants;
	}

	public static int getLevel(Item item, RegistryKey<net.minestom.server.item.enchant.Enchantment> key) {
		for (Enchantment enchantment : getEnchants(item)) {
			if (!enchantment.enchantment.equals(key)) continue;
			return enchantment.level;
		}
		return 0;
	}

	public static void put(Map<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> map, Enchantment... enchants) {
		for (Enchantment enchant : enchants) {
			int level = enchant.level;
			if (level == -1) level = 1; // -1 is used internally if a level is unspecified for removing an enchantment
			map.put(enchant.enchantment, level);
		}
	}

	public static Map<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> toMap(Enchantment... enchants) {
		Map<RegistryKey<net.minestom.server.item.enchant.Enchantment>, Integer> map = new HashMap<>(enchants.length);
		put(map, enchants);
		return map;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Enchantment that = (Enchantment) o;
		return level() == that.level() && Objects.equals(enchantment(), that.enchantment());
	}

	@Override
	public int hashCode() {
		return Objects.hash(enchantment(), level());
	}

}
