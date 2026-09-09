package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

import java.util.Arrays;
import java.util.Objects;

// known bug: if you do `send true if me contains slot 0 of me`, it doesn't work. if you replace the first me with `me's inventory`, it works

@Name("Contains")
@Description("Checks whether an inventory contains an item, a text contains another piece of text, " +
	"or a list (e.g. {list variable::*} or 'drops') contains another object.")
@Examples("whether player has 4 flint and 2 iron ingot")
@Since("1.0")
public class CondContains extends Condition {

	static {
		Skript.registerCondition(CondContains.class,
			"%inventories% (has|have) %items% [in [(the[ir]|his|her|its)] inventory]",
			"%inventories% (doesn't|does not|do not|don't) have %items% [in [(the[ir]|his|her|its)] inventory]",
			"%inventories/strings/objects% contain[(1¦s)] %items/strings/objects%",
			"%inventories/strings/objects% (doesn't|does not|do not|don't) contain %item/strings/objects%"
		);
	}

	/**
	 * The type of check to perform
	 */
	private enum CheckType {
		STRING, INVENTORY, OBJECTS, UNKNOWN
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?> containers;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<?> items;

	private boolean explicitSingle;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private CheckType checkType;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		containers = LiteralUtils.defendExpression(exprs[0]);
		items = LiteralUtils.defendExpression(exprs[1]);

		explicitSingle = matchedPattern == 2 && parseResult.mark != 1 || containers.isSingle();

		if (matchedPattern <= 1) {
			checkType = CheckType.INVENTORY;
		} else {
			checkType = CheckType.UNKNOWN;
		}

		setNegated(matchedPattern % 2 == 1);
		return LiteralUtils.canInitSafely(containers, items);
	}

	@Override
	public boolean check(Event e) {
		CheckType checkType = this.checkType;

		Object[] containerValues = containers.getAll(e);

		if (containerValues.length == 0)
			return isNegated();

		// Change checkType according to values
		if (checkType == CheckType.UNKNOWN) {
			if (Arrays.stream(containerValues)
					  .allMatch(AbstractInventory.class::isInstance)) {
				checkType = CheckType.INVENTORY;
			} else if (explicitSingle
				&& Arrays.stream(containerValues)
						 .allMatch(String.class::isInstance)) {
				checkType = CheckType.STRING;
			} else {
				checkType = CheckType.OBJECTS;
			}
		}

		if (checkType == CheckType.INVENTORY) {
			return SimpleExpression.check(containerValues, o -> {
				AbstractInventory inventory = (AbstractInventory) o;

				return items.check(e, o1 -> {
					if (o1 instanceof Item item) {
						ItemStack i = item.getItem();
						return containsAtLeast(inventory, i, i.amount());
					} else if (o1 instanceof AbstractInventory) // might need to change this to directly compare itemstack arrays
						return Objects.equals(inventory, o1);
					else
						return false;
				});
			}, isNegated(), containers.getAnd());
		} else if (checkType == CheckType.STRING) {
			boolean caseSensitive = SkriptConfig.caseSensitive.value();

			return SimpleExpression.check(containerValues, o -> {
				String string = (String) o;

				return items.check(e, o1 -> {
					if (o1 instanceof String) {
						return StringUtils.contains(string, (String) o1, caseSensitive);
					} else {
						return false;
					}
				});
			}, isNegated(), containers.getAnd());
		} else {
			assert checkType == CheckType.OBJECTS;

			return items.check(e, o1 -> {
				for (Object o2 : containerValues) {
					if (Comparators.compare(o1, o2) == Relation.EQUAL)
						return true;
				}
				return false;
			}, isNegated());
		}
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return containers.toString(e, debug) + (isNegated() ? " doesn't contain " : " contains ") + items.toString(e, debug);
	}

	private boolean containsAtLeast(AbstractInventory inventory, ItemStack i, int amount) {
		i = i.withAmount(1);
		int tally = 0;
		for (ItemStack item : inventory.getItemStacks()) {
			if (!item.withAmount(1).equals(i)) continue;
			tally += item.amount();
			if (tally >= amount) return true;
		}
		return tally >= amount;
	}

}
