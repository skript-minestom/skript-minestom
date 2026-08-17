package ch.njol.skript.util;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;

public class NonTickingLivingEntity extends LivingEntity {

	public NonTickingLivingEntity(EntityType entityType) {
		super(entityType);
	}

	@Override
	public void tick(long time) {}

}
