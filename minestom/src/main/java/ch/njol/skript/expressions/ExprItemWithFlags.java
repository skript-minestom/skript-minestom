package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.ItemFlag;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item With Flags")
@Description("An item with the given item flags applied, hiding those parts of its tooltip.")
@Examples("""
	give player diamond sword with the hide attributes item flag
	set {_potion} to potion of healing with all item flags
	set {_i} to stone with the item flags hide dye and hide enchants""")
@Keywords({"item", "flag", "hide", "tooltip"})
public class ExprItemWithFlags extends SimpleExpression<Item> {

	static {
		Skript.registerExpression(ExprItemWithFlags.class, Item.class, ExpressionType.COMBINED,
			"%items% with [the] item flag[s] %itemflags%",
			"%items% with [the] %itemflags% item flag[s]",
			"%items% with all [the] item flags");
	}

	private Expression<Item> items;
	private @Nullable Expression<ItemFlag> flags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		items = (Expression<Item>) expressions[0];
		// the third pattern applies every flag, so it has no item flag expression of its own
		flags = matchedPattern == 2 ? null : (Expression<ItemFlag>) expressions[1];
		return true;
	}

	@Override
	protected Item @Nullable [] get(Event event) {
		ItemFlag[] flags = this.flags == null ? ItemFlag.values() : this.flags.getArray(event);
		if (flags.length == 0) return new Item[0];
		Item[] items = this.items.getArray(event);
		Item[] flagged = new Item[items.length];
		for (int i = 0; i < items.length; i++) {
			Item item = items[i].copy();
			ItemFlag.add(item, false, flags);
			flagged[i] = item;
		}
		return flagged;
	}

	@Override
	public boolean isSingle() {
		return items.isSingle();
	}

	@Override
	public Class<? extends Item> getReturnType() {
		return Item.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (flags == null) return items.toString(event, debug) + " with all item flags";
		return items.toString(event, debug) + " with the item flags " + flags.toString(event, debug);
	}

}
