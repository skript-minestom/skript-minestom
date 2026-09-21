package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.expressions.ExprWorldBorder.borderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.canChangeBorderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.changeBorders;

@Name("World Border Center")
@Description("""
	The center of a world border. A world border only stores an x and a z, so the y of this point is always 0 \
	and the y of a point it is set to is ignored. Resetting it moves the center back to 0, 0.""")
@Examples("""
	set center of world border of {_instance} to {_spawn}
	set {_center} to center of {_instance}'s world border""")
public class ExprWorldBorderCenter extends SimplePropertyExpression<Object, Point> {

	static {
		register(ExprWorldBorderCenter.class, Point.class, "[world border] center", "worldborders/instances");
	}

	@Override
	public @Nullable Point convert(Object from) {
		WorldBorder border = borderOf(from);
		if (border == null) return null;
		return new Vec(border.centerX(), 0, border.centerZ());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (!canChangeBorderOf(getExpr(), getPropertyName())) return null;
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(Point.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		double centerX = 0;
		double centerZ = 0;
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta.length == 0 || !(delta[0] instanceof Point point)) return;
			centerX = point.x();
			centerZ = point.z();
			if (Double.isNaN(centerX) || Double.isNaN(centerZ)) return;
		}
		double x = centerX;
		double z = centerZ;
		changeBorders(getExpr(), event, border -> border.withCenter(x, z));
	}

	@Override
	public Class<? extends Point> getReturnType() {
		return Point.class;
	}

	@Override
	protected String getPropertyName() {
		return "center";
	}

}
