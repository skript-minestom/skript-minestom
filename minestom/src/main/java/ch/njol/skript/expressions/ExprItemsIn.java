package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.Slot;
import ch.njol.util.Kleenean;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Name("Items In")
@Description("""
	All items or specific type(s) of items in an inventory. Useful for looping or storing in a list variable.
	Please note that the positions of the items in the inventory are not saved, only their order is preserved.""")
@Example("""
    loop all items in the player's inventory:
    	loop-item's name = "Bob"
    	remove loop-item from the player""")
@Example("set {inventory::%uuid of player%::*} to items in the player's inventory")
@Since("2.0, 2.8.0 (specific types of items)")
public class ExprItemsIn extends SimpleExpression<Slot> {

	static {
		Skript.registerExpression(ExprItemsIn.class, Slot.class, ExpressionType.PROPERTY,
			"[all [[of] the]] items ([with]in|of|contained in|out of) [1:inventor(y|ies)] %inventories%",
			"all [[of] the] %items% ([with]in|of|contained in|out of) [1:inventor(y|ies)] %inventories%"
		);
	}

	private Expression<AbstractInventory> inventories;

	@Nullable
	private Expression<Item> items;

	@Override
	@SuppressWarnings("unchecked")
	/*
	 * the parse result will be null if it is used via the ExprInventory expression, however the expression will never
	 * be a variable when used with that expression (it is always an anonymous SimpleExpression)
	 */
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		if (matchedPattern == 0) {
			inventories = (Expression<AbstractInventory>) exprs[0];
		} else {
			items = (Expression<Item>) exprs[0];
			inventories = (Expression<AbstractInventory>) exprs[1];
		}
		if (inventories instanceof Variable && !inventories.isSingle() && parseResult.mark != 1)
			Skript.warning("'items in {variable::*}' does not actually represent the items stored in the variable. Use either '{variable::*}' (e.g. 'loop {variable::*}') if the variable contains items, or 'items in inventories {variable::*}' if the variable contains inventories.");
		return true;
	}

	@Override
	protected Slot[] get(Event event) {
		List<Slot> itemSlots = new ArrayList<>();
		Item[] types = this.items == null ? null : this.items.getArray(event);
		for (AbstractInventory inventory : inventories.getArray(event)) {
			for (int i = 0; i < inventory.getSize(); i++) {
				ItemStack itemStack = inventory.getItemStack(i);
				if (isAllowedItem(types, itemStack))
					itemSlots.add(new Slot(itemStack, inventory, i));
			}
		}
		return itemSlots.toArray(new Slot[0]);
	}

	@Override
	@Nullable
	public Iterator<Slot> iterator(Event event) {
		Iterator<? extends AbstractInventory> inventoryIterator = inventories.iterator(event);
		Item[] items = this.items == null ? null : this.items.getArray(event);
		if (inventoryIterator == null || !inventoryIterator.hasNext())
			return null;
		return new Iterator<>() {
			AbstractInventory currentInventory = inventoryIterator.next();

			int next = 0;

			@Override
			public boolean hasNext() {
				while (next < currentInventory.getSize() && !isAllowedItem(items, currentInventory.getItemStack(next)))
					next++;
				while (next >= currentInventory.getSize() && inventoryIterator.hasNext()) {
					currentInventory = inventoryIterator.next();
					next = 0;
					while (next < currentInventory.getSize() && !isAllowedItem(items, currentInventory.getItemStack(next)))
						next++;
				}
				return next < currentInventory.getSize();
			}

			@Override
			public Slot next() {
				if (!hasNext())
					throw new NoSuchElementException();
				int slot = next++;
				return new Slot(currentInventory.getItemStack(slot), currentInventory, slot);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean isLoopOf(String s) {
		return s.equalsIgnoreCase("item");
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (items == null)
			return "items in " + inventories.toString(event, debug);
		return "all " + items.toString(event, debug) + " in " + inventories.toString(event, debug);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<Slot> getReturnType() {
		return Slot.class;
	}

	private boolean isAllowedItem(@Nullable Item[] items, @Nullable ItemStack item) {
		if (items == null)
			return item != null;
		if (item == null)
			return false;

		for (Item i : items) {
			if (i == null) continue;
			if (item.isSimilar(i.getItem()))
				return true;
		}

		return false;
	}

}
