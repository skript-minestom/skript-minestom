package ch.njol.skript.expressions;


import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Peter Güttinger
 */
@Name("Position")
@Description("""
	The position of a block or entity. This not only represents the x, y and z coordinates of the position but also includes the world and the direction an entity is looking (e.g. teleporting to a saved position will make the teleported entity face the same saved direction every time).
	Please note that the position of an entity is at it's feet, use <a href='#ExprEyePoint'>head position</a> to get the position of the head.""")
@Examples("""
	set {home::%uuid of player%} to player's position
	message "Your home was set to %player's position% in %instance of player%.\"""")
@Since("")
public class ExprPositionOf extends PropertyExpression<Point, Point> {

	static {
		register(ExprPositionOf.class, Point.class, "[:block] position", "points");
	}

	private boolean block;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setExpr((Expression<? extends Point>) expressions[0]);
		block = parseResult.hasTag("block");
		return true;
	}

	@Override
	protected Point[] get(Event event, Point[] source) {
		Point[] points = new Point[source.length];
		for (int i = 0; i < source.length; i++) {
			Point point = source[i];
			points[i] = block ? point.asBlockVec() : point;
		}
		return points;
	}

	@Override
	public Class<? extends Point> getReturnType() {
		return Point.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (block ? "block " : "") + "position of " + getExpr().toString(event, debug);
	}

}
