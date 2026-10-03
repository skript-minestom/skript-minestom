package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Raycast;

@Name("Raycast Distance")
@Description("""
	How far a raycast travelled before it hit something. A raycast that hit nothing gives its full range.""")
@Examples("""
	set {_ray} to raycast from player
	send "that is %distance of {_ray}% blocks away" to player""")
public class ExprRaycastDistance extends SimplePropertyExpression<Raycast, Number> {

	static {
		register(ExprRaycastDistance.class, Number.class, "[hit] distance", "raycasts");
	}

	@Override
	public Number convert(Raycast from) {
		return from.distance();
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit distance";
	}

}
