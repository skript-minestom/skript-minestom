package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprVelocity;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@Name("Push")
@Description("""
	Push entities in a given direction or towards a specific point.
	Velocity values on Minestom are 1/20th (20 ticks per second is why) the value of Paper's.""")
@Example("push the player upwards")
@Example("push the victim downwards at speed 0.5")
@Example("push player towards player's target at speed 2")
@Example("pull player along vector(1,1,1) at speed 1.5")
@Since("1.4.6 2.12 (push towards)")
public class EffPush extends Effect {

	static {
		Skript.registerEffect(EffPush.class,
			"(push|thrust|pull) %entities% [along] %direction% [(at|with) [a] (speed|velocity|force) [of] %-number%]",
			"(push|thrust|pull) %entities% (towards|away:away from) %point% [(at|with) [a] (speed|velocity|force) [of] %-number%]");
	}

	private Expression<Entity> entities;
	private @Nullable Expression<Direction> direction;
	private @Nullable Expression<Point> target;
	private boolean awayFrom = false;
	private @Nullable Expression<Number> speed = null;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = (Expression<Entity>) exprs[0];
		if (matchedPattern == 0) {
			direction = (Expression<Direction>) exprs[1];
		} else {
			target = (Expression<Point>) exprs[1];
			awayFrom = parseResult.hasTag("away");
		}
		speed = (Expression<Number>) exprs[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Number speed = this.speed != null ? this.speed.getSingle(event) : null;
		if (this.speed != null && speed == null)
			return;

		Function<Entity, Vec> getDirection;
		if (this.direction != null) {
			// push along
			Direction direction = this.direction.getSingle(event);
			if (direction == null)
				return;
			getDirection = direction::getDirection;
		} else {
			// push towards
			assert this.target != null;
			Point target = this.target.getSingle(event);
			if (target == null)
				return;
			Vec targetVector = target.asVec();
			getDirection = entity -> {
				Vec direction = targetVector.sub(entity.getPosition().asVec());
				if (awayFrom)
					direction = direction.mul(-1);
				return direction;
			};
		}

		Entity[] entities = this.entities.getArray(event);
		for (Entity entity : entities) {
			Vec pushDirection = getDirection.apply(entity);
			if (speed != null)
				pushDirection = pushDirection.normalize().mul(speed.doubleValue());
			if (!(Double.isFinite(pushDirection.x()) && Double.isFinite(pushDirection.y()) && Double.isFinite(pushDirection.z()))) {
				// Some component of the mod vector is not finite, so just stop
				return;
			}
			entity.setVelocity(ExprVelocity.getTrueVelocity(entity).add(pushDirection));
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		var ssb = new SyntaxStringBuilder(event, debug).append("push", entities);
		if (direction != null) {
			ssb.append(direction);
		} else {
			assert target != null;
			if (awayFrom) {
				ssb.append("away from", target);
			} else {
				ssb.append("towards", target);
			}
		}
		if (speed != null)
			ssb.append("at a speed of", speed);
		return ssb.toString();
	}

}