package ch.njol.skript.util;

import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A group of {@link DataComponent}s that can be hidden from an item's tooltip at once.
 * <p>
 * Minestom has no item flags of its own; hiding is done through
 * {@link DataComponents#TOOLTIP_DISPLAY}, which holds the set of components that should not be
 * rendered. These constants mirror the item flags Skript users are already familiar with and map
 * each of them onto the components it hides.
 */
public enum ItemFlag {

	HIDE_ENCHANTS(DataComponents.ENCHANTMENTS),
	HIDE_ATTRIBUTES(DataComponents.ATTRIBUTE_MODIFIERS),
	HIDE_UNBREAKABLE(DataComponents.UNBREAKABLE),
	HIDE_DESTROYS(DataComponents.CAN_BREAK),
	HIDE_PLACED_ON(DataComponents.CAN_PLACE_ON),
	HIDE_DYE(DataComponents.DYED_COLOR),
	HIDE_ARMOR_TRIM(DataComponents.TRIM),
	HIDE_STORED_ENCHANTS(DataComponents.STORED_ENCHANTMENTS),
	HIDE_ADDITIONAL_TOOLTIP(DataComponents.BANNER_PATTERNS, DataComponents.BEES,
		DataComponents.BLOCK_ENTITY_DATA, DataComponents.BLOCK_STATE, DataComponents.BUNDLE_CONTENTS,
		DataComponents.CHARGED_PROJECTILES, DataComponents.CONTAINER, DataComponents.CONTAINER_LOOT,
		DataComponents.FIREWORK_EXPLOSION, DataComponents.FIREWORKS, DataComponents.INSTRUMENT,
		DataComponents.MAP_ID, DataComponents.PAINTING_VARIANT, DataComponents.POT_DECORATIONS,
		DataComponents.POTION_CONTENTS, DataComponents.TROPICAL_FISH_PATTERN,
		DataComponents.WRITTEN_BOOK_CONTENT);

	private final Set<DataComponent<?>> dataComponents;

	ItemFlag(DataComponent<?>... dataComponents) {
		this.dataComponents = Set.of(dataComponents);
	}

	public Set<DataComponent<?>> getDataComponents() {
		return dataComponents;
	}

	public static void add(Item to, boolean notify, ItemFlag... flags) {
		Set<DataComponent<?>> hidden = new HashSet<>(getHiddenComponents(to.getItem()));
		for (ItemFlag flag : flags)
			hidden.addAll(flag.dataComponents);
		apply(to, hidden, notify);
	}

	public static void set(Item to, ItemFlag... flags) {
		Set<DataComponent<?>> hidden = new HashSet<>();
		for (ItemFlag flag : flags)
			hidden.addAll(flag.dataComponents);
		apply(to, hidden, true);
	}

	public static void remove(Item from, ItemFlag... flags) {
		Set<DataComponent<?>> hidden = new HashSet<>(getHiddenComponents(from.getItem()));
		for (ItemFlag flag : flags)
			hidden.removeAll(flag.dataComponents);
		apply(from, hidden, true);
	}

	/**
	 * @return every flag whose components are all hidden on the given item.
	 */
	public static ItemFlag[] getFlags(Item item) {
		Set<DataComponent<?>> hidden = getHiddenComponents(item.getItem());
		if (hidden.isEmpty()) return new ItemFlag[0];
		List<ItemFlag> flags = new ArrayList<>();
		for (ItemFlag flag : values()) {
			if (hidden.containsAll(flag.dataComponents)) flags.add(flag);
		}
		return flags.toArray(new ItemFlag[0]);
	}

	public static Set<DataComponent<?>> getHiddenComponents(ItemStack item) {
		TooltipDisplay display = item.get(DataComponents.TOOLTIP_DISPLAY);
		return display == null ? Set.of() : display.hiddenComponents();
	}

	/**
	 * Replaces the hidden components of the item, leaving the rest of its tooltip display
	 * (currently only {@link TooltipDisplay#hideTooltip()}) untouched.
	 */
	private static void apply(Item item, Set<DataComponent<?>> hidden, boolean notify) {
		item.modify(stack -> {
			TooltipDisplay display = stack.get(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.EMPTY);
			TooltipDisplay newDisplay = new TooltipDisplay(display.hideTooltip(), hidden);
			TooltipDisplay prototype = stack.material().prototype()
				.get(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.EMPTY);
			// don't leave an explicit override behind if it's what the material does anyway,
			// otherwise an item that had all of its flags removed wouldn't equal a plain one
			if (newDisplay.equals(prototype)) return stack.reset(DataComponents.TOOLTIP_DISPLAY);
			return stack.with(DataComponents.TOOLTIP_DISPLAY, newDisplay);
		}, notify);
	}

}
