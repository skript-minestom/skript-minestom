package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import org.jspecify.annotations.Nullable;

@Name("UUID")
@Description("The UUID of an entity.")
@Examples("""
	broadcast "UUID: %uuid of player%\"""")
public class ExprUUID extends SimplePropertyExpression<Object, String> {

	static {
		register(ExprUUID.class, String.class, "uuid", "entities/instances");
	}

	@Override
	public @Nullable String convert(Object from) {
		return from instanceof Entity entity ? entity.getUuid().toString() : ((Instance) from).getUuid().toString();
	}

	@Override
	protected String getPropertyName() {
		return "uuid";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
