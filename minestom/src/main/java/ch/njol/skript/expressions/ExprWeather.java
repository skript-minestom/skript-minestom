package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Weather;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Weather")
@Description("""
	The weather of an instance. Setting it lets minestom pick how long the change takes; \
	use the transition weather effect to control the duration yourself.""")
@Examples("""
	set weather of {_instance} to thundering weather
	set weather of {_instance} to new weather with rain level 0.4
	reset weather of {_instance}""")
public class ExprWeather extends SimplePropertyExpression<Instance, Weather> {

	static {
		register(ExprWeather.class, Weather.class, "weather", "instances");
	}

	public static @Nullable Weather weatherOf(@Nullable Object object) {
		if (object instanceof Weather weather) return weather;
		if (object instanceof Instance instance) return instance.getWeather();
		return null;
	}

	@Override
	public Weather convert(Instance from) {
		return from.getWeather();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(Weather.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Weather weather = Weather.CLEAR;
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta.length == 0 || delta[0] == null) return;
			weather = (Weather) delta[0];
		}
		for (Instance instance : getExpr().getArray(event))
			instance.setWeather(weather);
	}

	@Override
	public Class<? extends Weather> getReturnType() {
		return Weather.class;
	}

	@Override
	protected String getPropertyName() {
		return "weather";
	}

}
