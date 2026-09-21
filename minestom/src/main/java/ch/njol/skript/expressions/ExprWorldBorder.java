package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@Name("World Border")
@Description("""
	The world border of an instance. While a border is transitioning to a new diameter, this is its state \
	on the current tick rather than the diameter it is heading towards.""")
@Examples("""
	set world border of {_instance} to new world border with diameter 500
	set world border of {_instance} to default world border
	reset world border of {_instance}""")
public class ExprWorldBorder extends SimplePropertyExpression<Instance, WorldBorder> {

	static {
		register(ExprWorldBorder.class, WorldBorder.class, "world border", "instances");
	}

	public static @Nullable WorldBorder borderOf(@Nullable Object object) {
		if (object instanceof WorldBorder border) return border;
		if (object instanceof Instance instance) return instance.getWorldBorder();
		return null;
	}

	public static boolean canChangeBorderOf(Expression<?> expression, String property) {
		if (!WorldBorder.class.isAssignableFrom(expression.getReturnType())) return true;
		if (Changer.ChangerUtils.acceptsChange(expression, Changer.ChangeMode.SET, WorldBorder.class)) return true;
		Skript.error("Can't change the " + property + " of this world border, as there is nowhere to store the " +
			"changed one. Change the " + property + " of an instance, or of a variable holding a world border.");
		return false;
	}

	public static void changeBorders(Expression<?> expression, Event event, UnaryOperator<WorldBorder> operator) {
		List<WorldBorder> delegated = new ArrayList<>();
		for (Object source : expression.getArray(event)) {
			WorldBorder border = borderOf(source);
			if (border == null) continue;
			WorldBorder changed = operator.apply(border);
			if (changed == null) continue;
			if (source instanceof Instance instance) {
				instance.setWorldBorder(changed);
			} else {
				delegated.add(changed);
			}
		}
		if (!delegated.isEmpty())
			expression.change(event, delegated.toArray(new WorldBorder[0]), Changer.ChangeMode.SET);
	}

	@Override
	public WorldBorder convert(Instance from) {
		return from.getWorldBorder();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(WorldBorder.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		WorldBorder border = WorldBorder.DEFAULT_BORDER;
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta.length == 0 || delta[0] == null) return;
			border = (WorldBorder) delta[0];
		}
		for (Instance instance : getExpr().getArray(event))
			instance.setWorldBorder(border);
	}

	@Override
	public Class<? extends WorldBorder> getReturnType() {
		return WorldBorder.class;
	}

	@Override
	protected String getPropertyName() {
		return "world border";
	}

}
