package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.Slot;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.metadata.item.ItemEntityMeta;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Dropped Item")
@Description("""
	The item a dropped item entity is showing.
	This also works on an item entity that was spawned through the spawn section rather than
	dropped, which is how such an entity is given an item to show.""")
@Examples("""
	on item pickup:
		give dropped item of event-entity to player

	spawn item at player's position:
		before spawn:
			set dropped item of entity to diamond""")
@Keywords({"drop", "item", "entity"})
public class ExprDroppedItem extends SimplePropertyExpression<Entity, Slot> {

	static {
		register(ExprDroppedItem.class, Slot.class, "drop[ped] item [slot]", "entities");
	}

	@Override
	public @Nullable Slot convert(Entity from) {
		if (from instanceof ItemEntity itemEntity) {
			return new Slot(itemEntity.getItemStack(), new EntityUpdater() {
				@Override
				public void update(ItemStack item) {
					itemEntity.setItemStack(item);
				}

				@Override
				public ItemStack getCurrentItem() {
					return itemEntity.getItemStack();
				}
			});
		}
		if (from.getEntityMeta() instanceof ItemEntityMeta meta) {
			return new Slot(meta.getItem(), new EntityUpdater() {
				@Override
				public void update(ItemStack item) {
					meta.setItem(item);
				}

				@Override
				public ItemStack getCurrentItem() {
					return meta.getItem();
				}
			});
		}
		return null;
	}

	@Override
	public Class<?> @org.eclipse.jdt.annotation.Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET
			|| mode == Changer.ChangeMode.DELETE) return CollectionUtils.array(Item.class);
		return null;
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Item item = delta == null ? null : (Item) delta[0];
		ItemStack stack = mode == Changer.ChangeMode.SET ? (item == null ? null : item.getItem()) : ItemStack.AIR;
		if (stack == null) return;
		for (Entity entity : getExpr().getArray(event)) {
			if (entity instanceof ItemEntity itemEntity) itemEntity.setItemStack(stack);
			else if (entity.getEntityMeta() instanceof ItemEntityMeta meta) meta.setItem(stack);
		}
	}

	@Override
	protected String getPropertyName() {
		return "dropped item";
	}

	@Override
	public Class<? extends Slot> getReturnType() {
		return Slot.class;
	}

	private abstract static class EntityUpdater implements Slot.Updater {

		@Override
		public int getSlot() {
			return 0;
		}

		@Override
		public AbstractInventory getContainer() {
			return null;
		}

	}

}
