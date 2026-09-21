package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.expressions.ExprWorldBorder.borderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.canChangeBorderOf;
import static ch.njol.skript.expressions.ExprWorldBorder.changeBorders;

@Name("World Border Diameter/Warning Distance/Teleport Boundary")
@Description("""
	A number making up a world border, either of a world border itself or of the one an instance is using. \
	The diameter can't go below 0, and neither can the warning distance or the teleport boundary. \
	Resetting one restores it to the value the default world border uses. \
	Only the numbers of an instance's border can be changed, as a world border itself can't be modified.""")
@Examples("""
	set diameter of world border of {_instance} to 100
	add 50 to size of {_instance}'s world border
	set warning distance of world border of {_instance} to 8""")
public class ExprWorldBorderNumber extends SimplePropertyExpression<Object, Number> {

	static {
		register(ExprWorldBorderNumber.class, Number.class,
			"[world border] (diameter|size|warning:warning distance|boundary:dimension teleport boundary)",
			"worldborders/instances");
	}

	private enum Property {
		DIAMETER, WARNING_DISTANCE, TELEPORT_BOUNDARY
	}

	private Property property;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (parseResult.hasTag("warning")) {
			property = Property.WARNING_DISTANCE;
		} else if (parseResult.hasTag("boundary")) {
			property = Property.TELEPORT_BOUNDARY;
		} else {
			property = Property.DIAMETER;
		}
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable Number convert(Object from) {
		WorldBorder border = borderOf(from);
		if (border == null) return null;
		return switch (property) {
			case DIAMETER -> border.diameter();
			case WARNING_DISTANCE -> border.warningDistance();
			case TELEPORT_BOUNDARY -> border.dimensionTeleportBoundary();
		};
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (!canChangeBorderOf(getExpr(), getPropertyName())) return null;
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(Number.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		double amount = 0;
		if (mode != Changer.ChangeMode.DELETE && mode != Changer.ChangeMode.RESET) {
			if (delta == null || delta.length == 0 || !(delta[0] instanceof Number number)) return;
			amount = number.doubleValue();
		}
		double delta0 = amount;
		changeBorders(getExpr(), event, border -> {
			double value = switch (mode) {
				case ADD -> currentOf(border) + delta0;
				case REMOVE -> currentOf(border) - delta0;
				case SET -> delta0;
				default -> currentOf(WorldBorder.DEFAULT_BORDER);
			};
			if (Double.isNaN(value)) return null;
			return withValue(border, Math.max(0, value));
		});
	}

	private double currentOf(WorldBorder border) {
		return switch (property) {
			case DIAMETER -> border.diameter();
			case WARNING_DISTANCE -> border.warningDistance();
			case TELEPORT_BOUNDARY -> border.dimensionTeleportBoundary();
		};
	}

	private WorldBorder withValue(WorldBorder border, double value) {
		return switch (property) {
			case DIAMETER -> border.withDiameter(value);
			case WARNING_DISTANCE -> border.withWarningDistance(toInt(value));
			case TELEPORT_BOUNDARY -> new WorldBorder(border.diameter(), border.centerX(), border.centerZ(),
				border.warningDistance(), border.warningTime(), toInt(value));
		};
	}

	private static int toInt(double value) {
		return (int) Math.min(Integer.MAX_VALUE, value);
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return switch (property) {
			case DIAMETER -> "diameter";
			case WARNING_DISTANCE -> "warning distance";
			case TELEPORT_BOUNDARY -> "dimension teleport boundary";
		};
	}

}
