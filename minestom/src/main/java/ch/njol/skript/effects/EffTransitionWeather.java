package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Weather;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Transition Weather")
@Description("""
	Changes the weather of an instance over the given duration. Setting the weather without a duration \
	lets minestom pick one based on how far the levels have to move. The duration is at least one tick.""")
@Examples("""
	transition weather of {_instance} to thundering weather over 10 seconds
	transition weather of {_instance} to clear weather over 1 minute""")
public class EffTransitionWeather extends Effect {

	static {
		Skript.registerEffect(EffTransitionWeather.class,
			"transition [the] weather (of|in) %instances% to %weather% over %timespan%");
	}

	private Expression<Instance> instances;
	private Expression<Weather> weather;
	private Expression<Timespan> duration;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		instances = (Expression<Instance>) expressions[0];
		weather = (Expression<Weather>) expressions[1];
		duration = (Expression<Timespan>) expressions[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Weather weather = this.weather.getSingle(event);
		if (weather == null) return;
		Timespan duration = this.duration.getSingle(event);
		if (duration == null) return;
		long ticks = duration.getAs(Timespan.TimePeriod.TICK);
		int transitionTicks = (int) Math.max(1, Math.min(Integer.MAX_VALUE, ticks));
		for (Instance instance : instances.getArray(event))
			instance.setWeather(weather, transitionTicks);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("transition weather of", instances, "to", weather, "over", duration);
		return sb.toString();
	}

}
