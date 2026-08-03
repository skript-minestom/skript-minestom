package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Item;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item Named")
@Description("An item with a specific custom name.")
@Examples("""
	give player stone named "Lucky Stone\"""")
public class ExprItemNamed extends SimpleExpression<Item> {

	static {
		Skript.registerExpression(ExprItemNamed.class, Item.class, ExpressionType.COMBINED, "%item% (with [the] name|named) %component%");
	}

	private Expression<Item> item;
	private Expression<ComponentWrapper> name;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		item = (Expression<Item>) expressions[0];
		name = (Expression<ComponentWrapper>) expressions[1];
		return true;
	}

	@Override
	protected @Nullable Item[] get(Event event) {
		Item item = this.item.getSingle(event);
		if (item == null) return new Item[0];
		item = item.copy();
		Component name = ComponentWrapper.getOrElse(this.name, event, null);
		if (name != null) item.modify(i -> i.withCustomName(name));
		return new Item[]{item};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Item> getReturnType() {
		return Item.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return item.toString(event, debug) + " named " + name.toString(event, debug);
	}

}
