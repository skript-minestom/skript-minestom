package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New World Border")
@Description("""
	Creates a world border with the given diameter, and optionally a center, a warning distance and a \
	warning time. Anything left out is taken from the default world border, which is centered at 0, 0 \
	with a warning distance of 5 and a warning time of 15 seconds.""")
@Examples("""
	set world border of {_instance} to new world border with diameter 500
	set {_border} to new world border with diameter 500 centered at {_spawn} with warning distance 10 and warning time 5 seconds""")
public class ExprNewWorldBorder extends SimpleExpression<WorldBorder> {

	static {
		Skript.registerExpression(ExprNewWorldBorder.class, WorldBorder.class, ExpressionType.COMBINED,
			"[a] [new] world border with [a] diameter [of] %number% [[and] [with] center[ed] [at] %-point%] "
				+ "[[and] [with] [a] warning distance [of] %-number%] [[and] [with] [a] warning time [of] %-timespan%]");
	}

	private Expression<Number> diameter;
	private @Nullable Expression<Point> center;
	private @Nullable Expression<Number> warningDistance;
	private @Nullable Expression<Timespan> warningTime;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		diameter = (Expression<Number>) expressions[0];
		center = (Expression<Point>) expressions[1];
		warningDistance = (Expression<Number>) expressions[2];
		warningTime = (Expression<Timespan>) expressions[3];
		return true;
	}

	@Override
	protected WorldBorder @Nullable [] get(Event event) {
		Number diameter = this.diameter.getSingle(event);
		if (diameter == null || Double.isNaN(diameter.doubleValue())) return new WorldBorder[0];

		double centerX = WorldBorder.DEFAULT_BORDER.centerX();
		double centerZ = WorldBorder.DEFAULT_BORDER.centerZ();
		if (center != null) {
			Point center = this.center.getSingle(event);
			if (center == null || Double.isNaN(center.x()) || Double.isNaN(center.z())) return new WorldBorder[0];
			centerX = center.x();
			centerZ = center.z();
		}

		int warningDistance = WorldBorder.DEFAULT_BORDER.warningDistance();
		if (this.warningDistance != null) {
			Number distance = this.warningDistance.getSingle(event);
			if (distance == null || Double.isNaN(distance.doubleValue())) return new WorldBorder[0];
			warningDistance = clampToInt(distance.doubleValue());
		}

		int warningTime = WorldBorder.DEFAULT_BORDER.warningTime();
		if (this.warningTime != null) {
			Timespan time = this.warningTime.getSingle(event);
			if (time == null) return new WorldBorder[0];
			warningTime = clampToInt(time.getAs(Timespan.TimePeriod.SECOND));
		}

		return new WorldBorder[]{
			new WorldBorder(Math.max(0, diameter.doubleValue()), centerX, centerZ, warningDistance, warningTime)
		};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends WorldBorder> getReturnType() {
		return WorldBorder.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("new world border with diameter", diameter);
		if (center != null) sb.append("centered at", center);
		if (warningDistance != null) sb.append("with warning distance", warningDistance);
		if (warningTime != null) sb.append("with warning time", warningTime);
		return sb.toString();
	}

	private static int clampToInt(double value) {
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
	}

}
