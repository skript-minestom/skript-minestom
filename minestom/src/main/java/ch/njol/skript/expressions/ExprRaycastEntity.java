package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Raycast;
import net.minestom.server.entity.Entity;
import org.eclipse.jdt.annotation.Nullable;

@Name("Raycast Hit Entity")
@Description("The entity a raycast hit, or nothing if it hit a block or nothing at all.")
@Examples("""
	set {_ray} to entity raycast from player
	if hit entity of {_ray} is set:
		kill hit entity of {_ray}""")
public class ExprRaycastEntity extends SimplePropertyExpression<Raycast, Entity> {

	static {
		register(ExprRaycastEntity.class, Entity.class, "hit entity", "raycasts");
	}

	@Override
	public @Nullable Entity convert(Raycast from) {
		return from.hitEntity();
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit entity";
	}

}
