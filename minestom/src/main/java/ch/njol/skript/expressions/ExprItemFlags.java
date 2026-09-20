package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.ItemFlag;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Name("Item Flags")
@Description("""
	The item flags of an item, which are the parts of its tooltip that are hidden.
	A flag is only listed if everything it hides is currently hidden.""")
@Examples("""
	add hide enchants to item flags of player's tool
	remove hide dye from the item flags of {_item}
	set item flags of {_item} to hide attributes and hide unbreakable
	clear item flags of {_item}

	if item flags of player's tool contains hide attributes:
		send "your tool's attributes are hidden!\"""")
@Keywords({"item", "flag", "hide", "tooltip"})
public class ExprItemFlags extends PropertyExpression<Item, ItemFlag> {

	static {
		register(ExprItemFlags.class, ItemFlag.class, "item flag[s]", "items");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Item>) expressions[0]);
		return true;
	}

	@Override
	protected ItemFlag[] get(Event event, Item[] source) {
		List<ItemFlag> flags = new ArrayList<>();
		for (Item item : source) {
			for (ItemFlag flag : ItemFlag.getFlags(item)) {
				if (!flags.contains(flag)) flags.add(flag);
			}
		}
		return flags.toArray(new ItemFlag[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(ItemFlag[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		ItemFlag[] flags = delta == null ? new ItemFlag[0] : Arrays.copyOf(delta, delta.length, ItemFlag[].class);
		for (Item item : getExpr().getArray(event)) {
			switch (mode) {
				case SET -> ItemFlag.set(item, flags);
				case ADD -> ItemFlag.add(item, true, flags);
				case REMOVE -> ItemFlag.remove(item, flags);
				case DELETE, RESET -> ItemFlag.set(item);
			}
		}
	}

	@Override
	public Class<? extends ItemFlag> getReturnType() {
		return ItemFlag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "item flags of " + getExpr().toString(event, debug);
	}

}
