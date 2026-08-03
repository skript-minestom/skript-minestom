package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import org.jspecify.annotations.Nullable;


@Name("Instance/World")
@Description("The instance an entity is in.")
@Examples("broadcast \"%instance of player%\"")
public class ExprInstance extends SimplePropertyExpression<Entity, Instance> {

	static {
		register(ExprInstance.class, Instance.class, "instance", "entities");
	}

	@Override
	public @Nullable Instance convert(Entity from) {
		return from.getInstance();
	}

	@Override
	public Class<?> @org.jetbrains.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		Skript.error("You cannot change the instance of entity with the instance expression. Use the teleport effect section instead.");
		return null;
	}

	@Override
	protected String getPropertyName() {
		return "instance";
	}

	@Override
	public Class<? extends Instance> getReturnType() {
		return Instance.class;
	}

}
