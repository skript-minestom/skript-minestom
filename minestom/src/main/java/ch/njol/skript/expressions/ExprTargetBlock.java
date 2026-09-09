package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Target Block")
@Description("""
	The target block position of what an entity is looking at.
	All blocks are treated as 1x1, collision shapes are not checked yet.
	Default range is 50.""")
@Examples("send player's target block")
public class ExprTargetBlock extends PropertyExpression<Entity, Point> {

	static {
		Skript.registerExpression(ExprTargetBlock.class, Point.class, ExpressionType.PROPERTY,
			"[the] target[ed] block [of %entities%] [with range %-number%]",
			"%entities%'[s] target[ed] block [with range %-number%]");
	}

	@Nullable
	private Expression<Number> range;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Entity>) expressions[0]);
		range = (Expression<Number>) expressions[1 - matchedPattern];
		return true;
	}

	@Override
	protected Point[] get(Event event, Entity[] source) {
		double range = this.range == null ? 50 : this.range.getOptionalSingle(event).map(Number::doubleValue).orElse(50d);
		List<Point> points = new ArrayList<>();
		for (Entity entity : source) {
			Point target = entity.getTargetBlockPosition(range);
			if (target != null) points.add(target);
		}
		return points.toArray(new Point[0]);
	}

	@Override
	public boolean isSingle() {
		return getExpr().isSingle();
	}

	@Override
	public Class<? extends Point> getReturnType() {
		return Point.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder b = new SyntaxStringBuilder(event, debug);
		b.append("target block of", getExpr());
		b.append("with range", range == null ? 50 : range);
		return b.toString();
	}

}
