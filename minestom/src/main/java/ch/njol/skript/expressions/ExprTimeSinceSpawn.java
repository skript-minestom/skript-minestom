package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import org.jspecify.annotations.Nullable;

@Name("Time Since Spawn")
@Description("""
	How long ago a dropped item entered its instance.
	This is real time rather than ticks, so it keeps counting for a non ticking dropped item.""")
@Examples("""
	on item pickup:
		if time since spawn of event-entity is less than 5 seconds:
			cancel event""")
@Keywords({"drop", "item", "spawn", "age"})
public class ExprTimeSinceSpawn extends SimplePropertyExpression<Entity, Timespan> {

	static {
		register(ExprTimeSinceSpawn.class, Timespan.class, "time since spawn", "entities");
	}

	@Override
	public @Nullable Timespan convert(Entity from) {
		if (!(from instanceof ItemEntity itemEntity)) return null;
		return new Timespan(itemEntity.getTimeSinceSpawn());
	}

	@Override
	protected String getPropertyName() {
		return "time since spawn";
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

}
