package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;

@Name("Is Pickable")
@Description("""
	Whether a dropped item can be picked up right now, meaning its own flag is enabled, its
	pickup delay has passed and it has not been removed.""")
@Example("if event-entity is pickable:")
public class CondIsPickable extends PropertyCondition<Entity> {

	static {
		register(CondIsPickable.class, "pickable", "entities");
	}

	@Override
	public boolean check(Entity entity) {
		return entity instanceof ItemEntity itemEntity && itemEntity.isPickable();
	}

	@Override
	protected String getPropertyName() {
		return "pickable";
	}

}
