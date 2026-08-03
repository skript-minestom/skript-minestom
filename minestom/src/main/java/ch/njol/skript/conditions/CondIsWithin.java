package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.AABB;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Is Within")
@Description("""
	Whether a point is within something else. The "something" can be a block, an entity, a chunk, a world, or a cuboid formed by two other points.
	Note that using the <a href='#CondCompare'>is between</a> condition will refer to a straight line between points, while this condition will refer to the cuboid between points.""")
@Examples("""
	if player's position is within {_loc1} and {_loc2}:
		send "You are in a PvP zone!" to player
	""")
@Since("2.7, 2.11 (world borders)")
@RequiredPlugins("MC 1.17+ (within block)")
public class CondIsWithin extends Condition {

	static {
		Skript.registerCondition(CondIsWithin.class,
			"%points% (is|are) within %point% and %point%",
			"%points% (isn't|is not|aren't|are not) within %point% and %point%",
			"%points% (is|are) (within|in[side [of]]) %chunks%",
			"%points% (isn't|is not|aren't|are not) (within|in[side [of]]) %chunks%"
		);
	}

	private Expression<Point> locsToCheck, loc1, loc2;
	private Expression<?> area;
	private boolean withinPoints;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setNegated(matchedPattern % 2 == 1);
		locsToCheck = (Expression<Point>) exprs[0];
		if (matchedPattern <= 1) {
			// within two points
			withinPoints = true;
			loc1 = (Expression<Point>) exprs[1];
			loc2 = (Expression<Point>) exprs[2];
		} else {
			// within an entity/chunk/world
			withinPoints = false;
			area = exprs[1];
		}
		return true;
	}

	@Override
	public boolean check(Event event) {
		// within two points
		if (withinPoints) {
			Point one = loc1.getSingle(event);
			Point two = loc2.getSingle(event);
			if (one == null || two == null)
				return isNegated();
			AABB box = new AABB(one, two, null);
			return locsToCheck.check(event, box::contains, isNegated());
		}

		Object[] areas = area.getAll(event);
		return locsToCheck.check(event, point ->
				SimpleExpression.check(areas, object -> {
					/*if (object instanceof Entity entity) {
						BoundingBox entityBox = entity.getBoundingBox();
						return entityBox.(point.toVector());
					} else */if (object instanceof Chunk chunk) {
						return point.sameChunk(chunk.toPosition());
					}
					return false;
				}, false, area.getAnd()),
			isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append(locsToCheck, "is within");
		if (withinPoints) {
			builder.append(loc1, "and", loc2);
		} else {
			builder.append(area);
		}
		return builder.toString();
	}

}
