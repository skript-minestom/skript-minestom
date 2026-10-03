package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Raycast;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import org.eclipse.jdt.annotation.Nullable;

@Name("Raycast Hit Position")
@Description("""
	Where a raycast hit. The hit position is the exact point on the block or entity the ray reached, \
	while the hit block position is the position of the block itself, which is what you need to change it. \
	A raycast that hit nothing gives nothing, and a raycast that hit an entity has no block position.""")
@Examples("""
	set {_ray} to block raycast from player
	set block at hit block position of {_ray} to air
	set {_exact} to hit position of {_ray}""")
public class ExprRaycastPoint extends SimplePropertyExpression<Raycast, Point> {

	static {
		register(ExprRaycastPoint.class, Point.class, "hit [block:block] position", "raycasts");
	}

	private boolean block;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		block = parseResult.hasTag("block");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable Point convert(Raycast from) {
		return block ? from.hitBlockPosition() : from.hitPosition();
	}

	@Override
	public Class<? extends Point> getReturnType() {
		return Point.class;
	}

	@Override
	protected String getPropertyName() {
		return block ? "hit block position" : "hit position";
	}

}
