package ch.njol.skript.expressions;


import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;

@Name("XYZ Component")
@Description("Gets or changes the x, y or z component of a point.")
@Examples("""
	set {_v} to vector(1, 2, 3)
	send "%x of {_v}%, %y of {_v}%, %z of {_v}%\"""")
@Since("2.2-dev28")
public class ExprPointXYZ extends SimplePropertyExpression<Point, Number> {

	static {
		register(ExprPointXYZ.class, Number.class, "(0:x|1:y|2:z) [component[s]]", "points");
	}

	private final static Character[] axes = new Character[] {'x', 'y', 'z'};

	private int axis;

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		super.init(exprs, matchedPattern, isDelayed, parseResult);
		axis = parseResult.mark;
		return true;
	}

	@Override
	public Number convert(Point point) {
		return axis == 0 ? point.x() : (axis == 1 ? point.y() : point.z());
	}

	@Override
	public Class<Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return axes[axis] + " component";
	}
}
