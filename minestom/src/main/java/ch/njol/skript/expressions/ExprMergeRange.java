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

@Name("Merge Range")
@Description("""
	How far away a dropped item looks for other dropped items to merge with, in blocks.
	Only a dropped item that is mergeable uses this.""")
@Examples("""
	drop 32 of stone at player's position:
		before spawn:
			set merge range of entity to 4""")
@Keywords({"drop", "item", "merge"})
public class ExprMergeRange extends SimplePropertyExpression<Entity, Number> {

	static {
		register(ExprMergeRange.class, Number.class, "merge range", "entities");
	}

	@Override
	public @Nullable Number convert(Entity from) {
		if (!(from instanceof ItemEntity itemEntity)) return null;
		return itemEntity.getMergeRange();
	}

	@Override
	public Class<?> @org.eclipse.jdt.annotation.Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, RESET -> CollectionUtils.array(Number.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Number number = delta == null ? null : (Number) delta[0];
		if (number == null && mode != Changer.ChangeMode.RESET) return;
		float range = number == null ? 0 : number.floatValue();
		for (Entity entity : getExpr().getArray(event)) {
			if (!(entity instanceof ItemEntity itemEntity)) continue;
			switch (mode) {
				case SET -> itemEntity.setMergeRange(range);
				case ADD -> itemEntity.setMergeRange(itemEntity.getMergeRange() + range);
				case REMOVE -> itemEntity.setMergeRange(Math.max(0, itemEntity.getMergeRange() - range));
				case RESET -> itemEntity.setMergeRange(1);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "merge range";
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

}
