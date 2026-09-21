package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.instance.Weather;

import static ch.njol.skript.expressions.ExprWeather.weatherOf;

@Name("Is Raining")
@Description("Checks whether an instance or a weather is raining or thundering, meaning its level is above 0.")
@Examples("""
	if {_instance} is raining:
	if weather of {_instance} is thundering:""")
public class CondIsRaining extends PropertyCondition<Object> {

	static {
		register(CondIsRaining.class, "(raining|:thundering)", "instances/weathers");
	}

	private boolean thunder;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		thunder = parseResult.hasTag("thundering");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public boolean check(Object value) {
		Weather weather = weatherOf(value);
		if (weather == null) return false;
		return thunder ? weather.thunderLevel() > 0 : weather.isRaining();
	}

	@Override
	protected String getPropertyName() {
		return thunder ? "thundering" : "raining";
	}

}
