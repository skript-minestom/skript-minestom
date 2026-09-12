package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Item Pickup State")
@Description("""
	Whether a living entity is allowed to pick dropped items up, which is disabled for every entity by default.
	The item pickup event only runs for entities that have this enabled.
	Entities that are not living entities are ignored.""")
@Examples("""
	set item pickup state of player to true

	on item pickup:
		if item pickup state of event-entity is true:
			give event-item to event-entity""")
@Keywords({"pickup", "pick up", "item"})
public class ExprCanPickupItems extends SimplePropertyExpression<Entity, Boolean> {

	static {
		register(ExprCanPickupItems.class, Boolean.class, "item pick[ ]up state", "entities");
	}

	@Override
	public @Nullable Boolean convert(Entity from) {
		if (!(from instanceof LivingEntity livingEntity)) return null;
		return livingEntity.canPickupItem();
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(Boolean.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Boolean state = delta == null ? null : (Boolean) delta[0];
		for (Entity entity : getExpr().getArray(event)) {
			if (!(entity instanceof LivingEntity livingEntity)) continue;
			switch (mode) {
				case SET -> {
					if (state == null) return;
					livingEntity.setCanPickupItem(state);
				}
				case RESET -> livingEntity.setCanPickupItem(false);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "item pickup state";
	}

	@Override
	public Class<? extends Boolean> getReturnType() {
		return Boolean.class;
	}

}
