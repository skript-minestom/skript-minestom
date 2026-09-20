package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.NonTickingItemEntity;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

@Name("Drop")
@Description("""
	Drops items on the ground as dropped items, which any entity whose item pickup state is
	enabled can pick up right away.
	A non ticking dropped item never merges with other dropped items and never counts down its
	pickup delay.
	To spawn a dropped item that is only there to be looked at, spawn an item entity instead and
	give it an item through the dropped item expression.""")
@Examples("""
	drop diamond at position(0, 41, 0) in instance {_instance}
	drop player's tool at player's position
	drop 5 of stone above player's position

	drop diamond at player's position:
		before spawn:
			set pickup delay of entity to 3 seconds
		after spawn:
			broadcast "dropped a diamond for %player%\"""")
@Keywords({"drop", "item", "spawn"})
public class EffSecDrop extends EffSecEntitySpawn {

	static {
		Skript.registerSection(EffSecDrop.class,
			"drop [:non ticking] %items% [%directions% %points%] [in [(world|instance)[s]] %instances%] [:sync]");
	}

	private Expression<Item> items;
	private Expression<Point> points;
	private Expression<Instance> instances;

	private boolean nonTicking;
	private boolean sync;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		items = (Expression<Item>) expressions[0];
		points = Direction.combine((Expression<? extends Direction>) expressions[1], (Expression<? extends Point>) expressions[2]);
		instances = (Expression<Instance>) expressions[3];
		nonTicking = parseResult.hasTag("non ticking");
		sync = parseResult.hasTag("sync");
		return loadSpawnSections(sectionNode);
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		Item[] items = this.items.getArray(event);
		Point[] points = this.points.getArray(event);
		Instance[] instances = this.instances.getArray(event);
		Object variables = Variables.copyLocalVariables(event);
		Object mostRecentLocals = variables;
		for (Instance instance : instances) {
			for (Point point : points) {
				for (Item item : items) {
					ItemStack stack = item.getItem();
					if (stack.isAir()) continue;
					ItemEntity droppedItem = nonTicking ? new NonTickingItemEntity(stack) : new ItemEntity(stack);
					mostRecentLocals = spawn(droppedItem, instance, point, variables, sync);
				}
			}
		}
		Variables.setLocalVariables(event, mostRecentLocals);
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "drop " + (nonTicking ? "non ticking " : "") + items.toString(event, debug) + " "
			+ points.toString(event, debug) + " in " + instances.toString(event, debug);
	}

}
