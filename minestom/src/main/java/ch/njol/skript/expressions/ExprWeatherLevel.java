package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Weather;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.expressions.ExprWeather.weatherOf;

@Name("Rain/Thunder Level")
@Description("""
	The rain or thunder level of an instance or a weather, a number between 0 and 1. \
	Values outside of that range are clamped. Only the level of an instance can be changed, \
	as a weather itself can't be modified.""")
@Examples("""
	set rain level of {_instance} to 0.5
	add 0.2 to thunder level of {_instance}
	set {_level} to rain level of (weather of {_instance})""")
public class ExprWeatherLevel extends SimplePropertyExpression<Object, Number> {

	static {
		register(ExprWeatherLevel.class, Number.class, "(rain|:thunder) level", "instances/weathers");
	}

	private boolean thunder;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		thunder = parseResult.hasTag("thunder");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable Number convert(Object from) {
		Weather weather = weatherOf(from);
		if (weather == null) return null;
		return thunder ? weather.thunderLevel() : weather.rainLevel();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (Weather.class.isAssignableFrom(getExpr().getReturnType())) {
			Skript.error("Can't change the " + getPropertyName() + " of a weather, as a weather can't be modified. " +
				"Change the " + getPropertyName() + " of an instance instead, or build a new weather.");
			return null;
		}
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
		for (Object source : getExpr().getArray(event)) {
			if (!(source instanceof Instance instance)) continue;
			Weather weather = instance.getWeather();
			double current = thunder ? weather.thunderLevel() : weather.rainLevel();
			double level = switch (mode) {
				case ADD -> current + amount;
				case REMOVE -> current - amount;
				case SET -> amount;
				default -> 0;
			};
			if (Double.isNaN(level)) continue;
			float clamped = (float) Math.max(0, Math.min(1, level));
			instance.setWeather(thunder ? weather.withThunderLevel(clamped) : weather.withRainLevel(clamped));
		}
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return thunder ? "thunder level" : "rain level";
	}

}
