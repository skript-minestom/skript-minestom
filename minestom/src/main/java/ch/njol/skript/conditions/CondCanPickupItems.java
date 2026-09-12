package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;

@Name("Can Pick Up Items")
@Description("Whether a living entity is allowed to pick dropped items up. Disabled for every entity by default.")
@Example("if player can pick up items:")
public class CondCanPickupItems extends PropertyCondition<Entity> {

	static {
		register(CondCanPickupItems.class, PropertyType.CAN, "pick[ ]up items", "entities");
	}

	@Override
	public boolean check(Entity entity) {
		return entity instanceof LivingEntity livingEntity && livingEntity.canPickupItem();
	}

	@Override
	protected PropertyType getPropertyType() {
		return PropertyType.CAN;
	}

	@Override
	protected String getPropertyName() {
		return "pick up items";
	}

}
