package ch.njol.skript.util;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;

public class NonTickingEntity extends Entity {

	public NonTickingEntity(EntityType entityType) {
		super(entityType);
	}

	@Override
	public void tick(long time) {}

}
