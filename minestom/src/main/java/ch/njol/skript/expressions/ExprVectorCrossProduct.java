package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.coordinate.Vec;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Vectors - Cross Product")
@Description("Gets the cross product between two vectors.")
@Examples("send \"%vector 1, 0, 0 cross vector 0, 1, 0%\"")
@Since("2.2-dev28")
public class ExprVectorCrossProduct extends SimpleExpression<Vec> {

	static {
		Skript.registerExpression(ExprVectorCrossProduct.class, Vec.class, ExpressionType.COMBINED, "%vector% cross %vector%");
	}

	@SuppressWarnings("null")
	private Expression<Vec> first, second;

	@Override
	@SuppressWarnings({"unchecked", "null"})
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		first = (Expression<Vec>) exprs[0];
		second = (Expression<Vec>) exprs[1];
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected Vec[] get(Event event) {
		Vec first = this.first.getSingle(event);
		Vec second = this.second.getSingle(event);
		if (first == null || second == null)
			return null;
		return CollectionUtils.array(first.cross(second));
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Vec> getReturnType() {
		return Vec.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return first.toString(event, debug) + " cross " + second.toString(event, debug);
	}

}
