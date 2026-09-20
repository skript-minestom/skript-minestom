package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.util.VectorMath.fromSkriptYaw;

@Name("Point with Property")
@Description("A point with a specific property (x, y, z, yaw, pitch) modified.")
@Examples("set {_p} to player's position with yaw 90")
public class ExprPointWithProperty extends SimpleExpression<Point> {

	static {
		Skript.registerExpression(ExprPointWithProperty.class, Point.class, ExpressionType.COMBINED,
			"%point% with (:x|:y|:z|:yaw|:pitch) [value] [of] %number%");
	}

	private Expression<Point> point;
	private Expression<Number> value;

	private String property;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		point = (Expression<Point>) expressions[0];
		value = (Expression<Number>) expressions[1];
		property = parseResult.tags.getFirst();
		return true;
	}

	@Override
	protected @Nullable Point[] get(Event event) {
		Point point = this.point.getSingle(event);
		if (point == null) return new Point[0];
		Number propertyValue = value.getSingle(event);
		if (propertyValue == null) return new Point[0];
		Point newPoint = switch (property) {
			case "x" -> point.withX(propertyValue.doubleValue());
			case "y" -> point.withY(propertyValue.doubleValue());
			case "z" -> point.withZ(propertyValue.doubleValue());
			case "yaw" -> point.asPos().withYaw(fromSkriptYaw(propertyValue.floatValue()));
			case "pitch" -> point.asPos().withPitch(propertyValue.floatValue());
			default -> point; // should never get here anyway
		};
		return new Point[]{newPoint};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Point> getReturnType() {
		return Point.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return point.toString(event, debug) + " with " + property + value.toString(event, debug);
	}

}
