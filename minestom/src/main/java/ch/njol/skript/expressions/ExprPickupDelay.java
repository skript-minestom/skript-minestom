package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Name("Pickup Delay")
@Description("""
	How long a dropped item has to lie on the ground before it can be picked up, counted from the
	moment it was dropped.
	A non ticking dropped item never counts its delay down.""")
@Examples("""
	drop diamond at player's position:
		before spawn:
			set pickup delay of entity to 3 seconds""")
@Keywords({"drop", "item", "pickup", "pick up"})
public class ExprPickupDelay extends SimplePropertyExpression<Entity, Timespan> {

	static {
		register(ExprPickupDelay.class, Timespan.class, "[item] pick[ ]up delay", "entities");
	}

	@Override
	public @Nullable Timespan convert(Entity from) {
		if (!(from instanceof ItemEntity itemEntity)) return null;
		return new Timespan(itemEntity.getPickupDelay());
	}

	@Override
	public Class<?> @org.eclipse.jdt.annotation.Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, RESET, DELETE -> CollectionUtils.array(Timespan.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Timespan timespan = delta == null ? null : (Timespan) delta[0];
		long millis = timespan == null ? 0 : timespan.get(ChronoUnit.MILLIS);
		for (Entity entity : getExpr().getArray(event)) {
			if (!(entity instanceof ItemEntity itemEntity)) continue;
			long delay = switch (mode) {
				case SET -> millis;
				case ADD -> itemEntity.getPickupDelay() + millis;
				case REMOVE -> Math.max(0, itemEntity.getPickupDelay() - millis);
				case RESET, DELETE -> 0;
				default -> itemEntity.getPickupDelay();
			};
			if (mode != Changer.ChangeMode.RESET && mode != Changer.ChangeMode.DELETE && timespan == null) continue;
			itemEntity.setPickupDelay(Duration.ofMillis(delay));
		}
	}

	@Override
	protected String getPropertyName() {
		return "pickup delay";
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

}
