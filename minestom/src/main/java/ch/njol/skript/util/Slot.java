package ch.njol.skript.util;

import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public class Slot extends Item {

	private final Updater updater;

	public Slot(ItemStack item, Updater updater) {
		super(item);
		this.updater = updater;
	}

	public Slot(ItemStack item, AbstractInventory container, int slot) {
		this(item, new InventoryUpdater(container, slot));
	}

	public Slot(ItemStack item, EquipmentHandler handler, EquipmentSlot slot) {
		this(item, new EquipmentUpdater(handler, slot));
	}

	public Slot(ItemStack item, EquipmentHandler handler, PlayerHand hand) {
		this(item, new EquipmentUpdater(handler, hand));
	}

	@Override
	public void modify(UnaryOperator<ItemStack> modifyFunction, boolean notifyContainer) {
		ItemStack containerSlotItem = updater.getCurrentItem();
		ItemStack preModificationItem = getItem();
		super.modify(modifyFunction, notifyContainer);
		if (notifyContainer && preModificationItem.equals(containerSlotItem)) updater.update(getItem());
	}

	public int getIndex() {
		return updater.getSlot();
	}

	public @Nullable AbstractInventory getContainer() {
		return updater.getContainer();
	}

	public interface Updater {

		void update(ItemStack item);

		ItemStack getCurrentItem();

		int getSlot();

		AbstractInventory getContainer();

	}

	static class InventoryUpdater implements Updater {

		private final AbstractInventory container;
		private final int slot;

		public InventoryUpdater(AbstractInventory container, int slot) {
			this.container = container;
			this.slot = slot;
		}

		@Override
		public void update(ItemStack item) {
			container.setItemStack(slot, item);
		}

		@Override
		public ItemStack getCurrentItem() {
			return container.getItemStack(slot);
		}

		@Override
		public int getSlot() {
			return slot;
		}

		@Override
		public AbstractInventory getContainer() {
			return container;
		}

	}

	static class EquipmentUpdater implements Updater {

		private final EquipmentHandler container;
		private final EquipmentSlot slot;

		public EquipmentUpdater(EquipmentHandler container, EquipmentSlot slot) {
			this.container = container;
			this.slot = slot;
		}

		public EquipmentUpdater(EquipmentHandler container, PlayerHand hand) {
			this(container, EquipmentSlot.valueOf(hand.toString() + "_HAND"));
		}

		@Override
		public void update(ItemStack item) {
			container.setEquipment(slot, item);
		}

		@Override
		public ItemStack getCurrentItem() {
			return container.getEquipment(slot);
		}

		@Override
		public int getSlot() {
			return switch (slot) {
				case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> slot.armorSlot();
				case OFF_HAND, MAIN_HAND -> getPlayerSlot();
				default -> -1;
			};
		}

		@Override
		public AbstractInventory getContainer() {
			return null;
		}

		private int getPlayerSlot() {
			if (container instanceof Player player) {
				if (slot == EquipmentSlot.OFF_HAND) return PlayerInventoryUtils.OFFHAND_SLOT;
				if (slot == EquipmentSlot.MAIN_HAND) return player.getHeldSlot();
			}
			return -1;
		}

	}

}
