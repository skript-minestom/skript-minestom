package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.ItemTooltip;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item Tooltip Hidden")
@Description("""
	Whether the tooltip of an item is hidden.
	The additional tooltip covers the same parts as the hide additional tooltip item flag, and an \
	item only counts as having it hidden if every one of those parts is hidden, so an item \
	that merely has its entire tooltip hidden doesn't match.""")
@Examples("""
	if player's tool has its entire tooltip hidden:
		send "you can't see what that is!"

	if {_item} doesn't have its additional tooltip hidden:
		set {_item} to {_item} without additional tooltip""")
@Keywords({"item", "tooltip", "hide"})
public class CondItemTooltipHidden extends Condition {

	static {
		Skript.registerCondition(CondItemTooltipHidden.class,
			"%items% (has|have) [its|their] (entire|:additional) tool[ ]tip[s] hidden",
			"%items% (doesn't|does not|don't|do not) have [its|their] (entire|:additional) tool[ ]tip[s] hidden");
	}

	private Expression<Item> items;

	private boolean additional;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		items = (Expression<Item>) expressions[0];
		additional = parseResult.hasTag("additional");
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		return items.check(event,
			item -> additional ? ItemTooltip.isAdditionalHidden(item) : ItemTooltip.isEntireHidden(item),
			isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return items.toString(event, debug) + (isNegated() ? " doesn't have " : " has ") + (additional ? "additional" : "entire") + " tooltip hidden";
	}

}
