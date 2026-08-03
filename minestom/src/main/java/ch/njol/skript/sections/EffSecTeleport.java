package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.EntitySpawnWrapper;
import ch.njol.skript.lang.*;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Name("Teleport")
@Description("""
	Teleports one or more entities to a location.
	Optionally specify an instance to move entities into a different world.
	Use 'sync' to wait for each teleport to finish before continuing.
	An optional subsection runs once the teleport completes.""")
@Examples("""
	teleport player to vector(0, 64, 0):
	    broadcast "Player arrived!"
	teleport {_entity} to {_loc} in instance {_instance} sync:
	    set health of entity to 20""")
public class EffSecTeleport extends EffectSection {

	static {
		Skript.registerSection(EffSecTeleport.class, "teleport %entities% to %point% [instance:in [(world|instance)] %-instance%] [:sync]");
	}

	private Expression<Entity> entities;
	private Expression<Point> point;
	@Nullable
	private Expression<Instance> instance;

	private boolean providedInstance;
	@Nullable
	private Trigger callback;
	private boolean sync;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		entities = (Expression<Entity>) expressions[0];
		point = (Expression<Point>) expressions[1];
		instance = (Expression<Instance>) expressions[2];
		providedInstance = parseResult.hasTag("instance");
		sync = parseResult.hasTag("sync");
		if (sectionNode != null) callback = loadCode(sectionNode, "teleport callback", EntitySpawnWrapper.class);
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		Point point = this.point.getSingle(event);
		if (point == null) return super.walk(event, false);
		Pos pos = point.asPos();
		Instance i = instance != null ? instance.getSingle(event) : null;
		if (providedInstance && i == null) return super.walk(event, false);
		Object variables = Variables.copyLocalVariables(event);
		for (Entity entity : entities.getArray(event)) {
			CompletableFuture<Void> future;
			Instance instance;
			if (i == null || i.equals(entity.getInstance())) {
				future = entity.teleport(pos);
				instance = entity.getInstance();
			}
			else {
				future = entity.setInstance(i, pos);
				instance = i;
			}
			future.whenComplete((_, throwable) -> {
				if (throwable != null || callback == null) return;
				Event e = new EntitySpawnWrapper(new EntitySpawnEvent(entity, instance));
				Variables.setLocalVariables(e, variables);
				TriggerItem.walk(callback, e);
				Variables.removeLocals(e);
			});
			if (sync) future.join();
		}
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "teleport " + entities.toString(event, debug) + " to " + point.toString(event, debug) +
			(instance != null ? " in instance " + instance.toString(event, debug) : "");
	}

}
