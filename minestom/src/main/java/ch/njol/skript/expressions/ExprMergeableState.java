package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.ItemEntity;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Mergeable State")
@Description("""
	Whether a dropped item merges with other dropped items of the same kind near it, which is
	enabled by default.
	A non ticking dropped item never merges either way.""")
@Examples("""
	drop 32 of stone at player's position:
		before spawn:
			set mergeable state of entity to false""")
@Keywords({"drop", "item", "merge"})
public class ExprMergeableState extends SimplePropertyExpression<Entity, Boolean> {

	static {
		register(ExprMergeableState.class, Boolean.class, "mergeab(le|ility) [state]", "entities");
	}

	@Override
	public @Nullable Boolean convert(Entity from) {
		if (!(from instanceof ItemEntity itemEntity)) return null;
		return itemEntity.isMergeable();
	}

	@Override
	public Class<?> @org.eclipse.jdt.annotation.Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(Boolean.class);
		return null;
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Boolean state = delta == null ? null : (Boolean) delta[0];
		if (state == null && mode != Changer.ChangeMode.RESET) return;
		boolean mergeable = mode == Changer.ChangeMode.RESET || state;
		for (Entity entity : getExpr().getArray(event)) {
			if (entity instanceof ItemEntity itemEntity) itemEntity.setMergeable(mergeable);
		}
	}

	@Override
	protected String getPropertyName() {
		return "mergeable state";
	}

	@Override
	public Class<? extends Boolean> getReturnType() {
		return Boolean.class;
	}

}
