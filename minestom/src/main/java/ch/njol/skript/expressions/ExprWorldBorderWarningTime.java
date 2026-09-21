package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.expressions.ExprWorldBorder.borderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.canChangeBorderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.changeBorders;

@Name("World Border Warning Time")
@Description("""
	How long the warning indicator is shown for when a world border is closing in. Minecraft stores this \
	as whole seconds, so anything smaller is rounded down. Resetting it restores the value the default \
	world border uses.""")
@Examples("""
	set warning time of world border of {_instance} to 10 seconds
	add 5 seconds to warning time of {_instance}'s world border""")
public class ExprWorldBorderWarningTime extends SimplePropertyExpression<Object, Timespan> {

	static {
		register(ExprWorldBorderWarningTime.class, Timespan.class, "[world border] warning time", "worldborders/instances");
	}

	@Override
	public @Nullable Timespan convert(Object from) {
		WorldBorder border = borderOf(from);
		if (border == null) return null;
		return new Timespan(Timespan.TimePeriod.SECOND, Math.max(0, border.warningTime()));
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (!canChangeBorderOf(getExpr(), getPropertyName())) return null;
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(Timespan.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		long seconds = 0;
		if (mode != Changer.ChangeMode.DELETE && mode != Changer.ChangeMode.RESET) {
			if (delta == null || delta.length == 0 || !(delta[0] instanceof Timespan timespan)) return;
			seconds = timespan.getAs(Timespan.TimePeriod.SECOND);
		}
		long delta0 = seconds;
		changeBorders(getExpr(), event, border -> {
			long warningTime = switch (mode) {
				case ADD -> border.warningTime() + delta0;
				case REMOVE -> border.warningTime() - delta0;
				case SET -> delta0;
				default -> WorldBorder.DEFAULT_BORDER.warningTime();
			};
			return border.withWarningTime((int) Math.max(0, Math.min(Integer.MAX_VALUE, warningTime)));
		});
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

	@Override
	protected String getPropertyName() {
		return "warning time";
	}

}
