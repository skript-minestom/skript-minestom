package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.Item;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Drop")
@Description("""
	Drops items on the ground as pickable item entities.
	Dropped items can be picked up right away by any entity whose item pickup state is enabled.""")
@Examples("""
	drop diamond at position(0, 41, 0) in instance {_instance}
	drop player's tool at player's position
	drop 5 of stone above player's position""")
@Keywords({"drop", "item", "spawn"})
public class EffDrop extends Effect {

	static {
		Skript.registerEffect(EffDrop.class,
			"drop %items% [%directions% %points%] [in [(world|instance)[s]] %instances%]");
	}

	private Expression<Item> items;
	private Expression<Point> points;
	private Expression<Instance> instances;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		items = (Expression<Item>) expressions[0];
		points = Direction.combine((Expression<? extends Direction>) expressions[1], (Expression<? extends Point>) expressions[2]);
		instances = (Expression<Instance>) expressions[3];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Item[] items = this.items.getArray(event);
		if (items.length == 0) return;
		Point[] points = this.points.getArray(event);
		Instance[] instances = this.instances.getArray(event);
		for (Instance instance : instances) {
			for (Point point : points) {
				for (Item item : items) {
					ItemStack stack = item.getItem();
					if (stack.isAir()) continue;
					new ItemEntity(stack).setInstance(instance, point);
				}
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "drop " + items.toString(event, debug) + " " + points.toString(event, debug)
			+ " in " + instances.toString(event, debug);
	}

}
