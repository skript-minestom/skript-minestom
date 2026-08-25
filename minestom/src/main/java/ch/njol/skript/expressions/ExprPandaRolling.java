package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.animal.PandaMeta;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Panda Rolling State")
@Description("""
	The rolling state of a panda.
	Entities that are not pandas are ignored.""")
@Examples("""
	spawn a panda at player's position:
		after spawn:
			set rolling state of entity to true

	if rolling state of {_panda} is true:
		set rolling state of {_panda} to false""")
@Keywords({"panda", "roll", "rolling"})
public class ExprPandaRolling extends SimplePropertyExpression<Entity, Boolean> {

	static {
		register(ExprPandaRolling.class, Boolean.class, "roll(ing|) [state]", "entities");
	}

	@Override
	public @Nullable Boolean convert(Entity from) {
		if (!(from.getEntityMeta() instanceof PandaMeta meta)) return null;
		return meta.isRolling();
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(Boolean.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Boolean state = delta == null ? null : (Boolean) delta[0];
		for (Entity entity : getExpr().getArray(event)) {
			if (!(entity.getEntityMeta() instanceof PandaMeta meta)) continue;
			switch (mode) {
				case SET -> {
					if (state == null) return;
					meta.setRolling(state);
				}
				case RESET -> meta.setRolling(false);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "rolling state";
	}

	@Override
	public Class<? extends Boolean> getReturnType() {
		return Boolean.class;
	}

}
