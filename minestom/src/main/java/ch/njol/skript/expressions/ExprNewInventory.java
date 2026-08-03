package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.InventoryType;
import ch.njol.util.Kleenean;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.effects.EffOpenInventory.getDefaultTitle;


@Name("New Inventory")
@Description("An expression to create a new inventory.")
@Examples("""
	set {_inv} to new chest 3 row inventory named mm("<red>bob")""")
public class ExprNewInventory extends SimpleExpression<AbstractInventory>{

	static {
		Skript.registerExpression(ExprNewInventory.class, AbstractInventory.class, ExpressionType.COMBINED,
			"new %inventorytype% [inventory] [(named|with (name|title)) %-component%]"
		);
	}

	private Expression<InventoryType> inventoryType;
	@Nullable
	private Expression<ComponentWrapper> name;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean, ParseResult parseResult) {
		inventoryType = (Expression<InventoryType>) exprs[0];
		name = (Expression<ComponentWrapper>) exprs[1];

		if (inventoryType instanceof Literal<InventoryType> literal && literal.getSingle() == InventoryType.PLAYER) {
			Skript.error("Cannot create virtual inventory of type 'player'.");
			return false;
		}
		return true;
	}

	@Override
	protected Inventory[] get(Event e) {
		InventoryType type = inventoryType.getSingle(e);
		if (type == null || type == InventoryType.PLAYER) {
			return new Inventory[0];
		}

		//noinspection DataFlowIssue
		Inventory inventory = new Inventory(type.getMinestomType(), ComponentWrapper.getOrElse(this.name, e, getDefaultTitle(type)));
		return new Inventory[]{inventory};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends AbstractInventory> getReturnType() {
		return AbstractInventory.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "new " + inventoryType.toString(e, debug) + " inventory" + (name == null ? "" : " named " + name.toString(e, debug));
	}

}

