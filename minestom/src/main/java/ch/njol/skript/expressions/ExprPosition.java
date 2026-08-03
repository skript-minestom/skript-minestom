package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.expressions.base.WrapperExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Peter Güttinger
 */
@Name("Point")
@Description("The position where an event happened (e.g. at an entity or block), or a location <a href='#ExprDirection'>relative</a> to another (e.g. 1 meter above another location).")
@Examples("""
	drop 5 apples at the event-position # exactly the same as writing 'drop 5 apples'
	set {_loc} to the position 1 meter above the player""")
@Since("2.0")
public class ExprPosition extends WrapperExpression<Point> {
	static {
		Skript.registerExpression(ExprPosition.class, Point.class, ExpressionType.COMBINED, "[the] position %directions% [%point%]");
	}

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		if (exprs.length > 0) {
			super.setExpr(Direction.combine((Expression<? extends Direction>) exprs[0], (Expression<? extends Point>) exprs[1]));
			return true;
		} else {
			setExpr(new EventValueExpression<>(Point.class));
			return ((EventValueExpression<Point>) getExpr()).init();
		}
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return getExpr() instanceof EventValueExpression ? "the position" : "the position " + getExpr().toString(e, debug);
	}

}
