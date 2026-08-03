package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.entity.Entity;
import org.jspecify.annotations.Nullable;


@Name("Entity ID")
@Description("The network entity ID of an entity.")
@Examples("broadcast \"ID: %entity id of player%\"")
public class ExprEntityId extends SimplePropertyExpression<Entity, Integer> {

	static {
		register(ExprEntityId.class, Integer.class, "entity id", "entities");
	}

	@Override
	public @Nullable Integer convert(Entity from) {
		return from.getEntityId();
	}

	@Override
	protected String getPropertyName() {
		return "entity id";
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

}
