package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.entity.Entity;
import org.jspecify.annotations.Nullable;


@Name("Alive Ticks")
@Description("The number of ticks an entity has been alive.")
@Examples("broadcast \"%alive ticks of player%\"")
public class ExprAliveTicks extends SimplePropertyExpression<Entity, Long> {

	static {
		register(ExprAliveTicks.class, Long.class, "(alive ticks|ticks lived)", "entities");
	}

	@Override
	public @Nullable Long convert(Entity from) {
		return from.getAliveTicks();
	}

	@Override
	protected String getPropertyName() {
		return "alive ticks";
	}

	@Override
	public Class<? extends Long> getReturnType() {
		return Long.class;
	}

}
