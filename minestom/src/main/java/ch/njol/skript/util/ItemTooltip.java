package ch.njol.skript.util;

import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.component.TooltipDisplay;

import java.util.function.UnaryOperator;

public final class ItemTooltip {

	private ItemTooltip() { }

	public static void setEntireHidden(Item item, boolean hidden) {
		modify(item, display -> display.withHideTooltip(hidden), true);
	}

	public static void setAdditionalHidden(Item item, boolean hidden) {
		if (hidden) ItemFlag.add(item, true, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
		else ItemFlag.remove(item, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

	}

	public static boolean isEntireHidden(Item item) {
		return getDisplay(item.getItem()).hideTooltip();
	}

	/**
	 * @return whether every component of the additional tooltip is hidden. An item that only has its
	 * entire tooltip hidden doesn't count, as showing the tooltip again would reveal them.
	 */
	public static boolean isAdditionalHidden(Item item) {
		return ItemFlag.getHiddenComponents(item.getItem()).containsAll(ItemFlag.HIDE_ADDITIONAL_TOOLTIP.getDataComponents());
	}

	public static TooltipDisplay getDisplay(DataComponent.Holder holder) {
		return holder.get(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.EMPTY);
	}

	public static void modify(Item item, UnaryOperator<TooltipDisplay> modifyFunction, boolean notify) {
		item.modify(stack -> {
			TooltipDisplay display = modifyFunction.apply(getDisplay(stack));
			if (display.equals(getDisplay(stack.material().prototype()))) return stack.reset(DataComponents.TOOLTIP_DISPLAY);
			return stack.with(DataComponents.TOOLTIP_DISPLAY, display);
		}, notify);
	}

}
