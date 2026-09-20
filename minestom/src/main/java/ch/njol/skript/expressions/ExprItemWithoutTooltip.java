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
import ch.njol.skript.util.ItemTooltip;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item Without Tooltip")
@Description("""
	An item with its tooltip hidden, or shown again.
	The entire tooltip hides everything, including the name and lore of the item.
	The additional tooltip hides the same parts as the hide additional tooltip item flag, \
	such as potion effects, banner patterns and the contents of a container.""")
@Examples("""
	give player stone without entire tooltip
	give player potion of healing without its additional tooltip
	set {_item} to {_hidden item} with entire tooltip""")
@Keywords({"item", "tooltip", "hide"})
public class ExprItemWithoutTooltip extends SimpleExpression<Item> {

	static {
		Skript.registerExpression(ExprItemWithoutTooltip.class, Item.class, ExpressionType.COMBINED,
			"%items% with[:out] [the|its|their] (entire|:additional) tool[ ]tip[s]");
	}

	private Expression<Item> items;

	private boolean hidden;
	private boolean additional;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		items = (Expression<Item>) expressions[0];
		hidden = parseResult.hasTag("out");
		additional = parseResult.hasTag("additional");
		return true;
	}

	@Override
	protected Item @Nullable [] get(Event event) {
		Item[] items = this.items.getArray(event);
		Item[] modified = new Item[items.length];
		for (int i = 0; i < items.length; i++) {
			Item item = items[i].copy();
			if (additional) ItemTooltip.setAdditionalHidden(item, hidden);
			else ItemTooltip.setEntireHidden(item, hidden);
			modified[i] = item;
		}
		return modified;
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
		return items.toString(event, debug) + (hidden ? " without " : " with ") + (additional ? "additional" : "entire") + " tooltip";
	}

}
