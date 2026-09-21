package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Raycast;
import net.minestom.server.coordinate.Vec;
import org.eclipse.jdt.annotation.Nullable;

@Name("Raycast Hit Normal")
@Description("""
	A unit vector pointing out of the face a raycast hit, which is what you add to the hit block position \
	to find where a block would be placed against it. A raycast that started inside what it hit has no normal.""")
@Examples("""
	set {_ray} to block raycast from player
	set block at (hit block position of {_ray} offset by hit normal of {_ray}) to torch""")
public class ExprRaycastNormal extends SimplePropertyExpression<Raycast, Vec> {

	static {
		register(ExprRaycastNormal.class, Vec.class, "hit normal", "raycasts");
	}

	@Override
	public @Nullable Vec convert(Raycast from) {
		return from.hitNormal();
	}

	@Override
	public Class<? extends Vec> getReturnType() {
		return Vec.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit normal";
	}

}
