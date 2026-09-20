package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;

@Name("Is Mergeable")
@Description("Whether a dropped item merges with other dropped items of the same kind near it. Enabled by default.")
@Example("if event-entity is mergeable:")
public class CondIsMergeable extends PropertyCondition<Entity> {

	static {
		register(CondIsMergeable.class, "mergeable", "entities");
	}

	@Override
	public boolean check(Entity entity) {
		return entity instanceof ItemEntity itemEntity && itemEntity.isMergeable();
	}

	@Override
	protected String getPropertyName() {
		return "mergeable";
	}

}
