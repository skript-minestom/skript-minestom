package ch.njol.skript.util;

import net.minestom.server.entity.ItemEntity;
import net.minestom.server.item.ItemStack;

public class NonTickingItemEntity extends ItemEntity {

	public NonTickingItemEntity(ItemStack itemStack) {
		super(itemStack);
	}

	@Override
	public void tick(long time) {}

}
