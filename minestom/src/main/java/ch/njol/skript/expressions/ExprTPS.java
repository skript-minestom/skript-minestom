package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond;
import me.lucko.spark.api.statistic.types.DoubleStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Name("TPS")
@Description({
	"The server's ticks per second, as measured by spark.",
	"Without a window, this returns all five of spark's averages, in order: the last 5 seconds, 10 seconds, 1 minute, 5 minutes and 15 minutes."
})
@Example("broadcast \"%tps from the last 1 minute%\"")
@Example("""
	every 30 seconds:
		if tps from the last 5 seconds < 15:
			broadcast "<red>The server is lagging!\"""")
@Keywords({"tps", "lag", "spark", "performance"})
public class ExprTPS extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprTPS.class, Number.class, ExpressionType.SIMPLE,
			"[the] [server] tps [(from|over|in) [the] last (sec5:5 seconds|sec10:10 seconds|min1:1 minute|min5:5 minutes|min15:15 minutes)]");
	}

	@Nullable
	private Window window;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!parseResult.tags.isEmpty())
			window = Window.valueOf(parseResult.tags.getFirst().toUpperCase(Locale.ENGLISH));
		return true;
	}

	@Override
	protected Number[] get(Event event) {
		DoubleStatistic<TicksPerSecond> tps = statistic();
		if (tps == null) return new Number[0];
		if (window != null) return new Number[]{tps.poll(window.window)};
		double[] averages = tps.poll();
		Number[] values = new Number[averages.length];
		for (int i = 0; i < averages.length; i++) values[i] = averages[i];
		return values;
	}

	@Override
	public boolean isSingle() {
		return window != null;
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "tps" + (window == null ? "" : " from the last " + window.name);
	}

	@Nullable
	private static DoubleStatistic<TicksPerSecond> statistic() {
		try {
			return SparkProvider.get().tps();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	private enum Window {
		SEC5(TicksPerSecond.SECONDS_5, "5 seconds"),
		SEC10(TicksPerSecond.SECONDS_10, "10 seconds"),
		MIN1(TicksPerSecond.MINUTES_1, "1 minute"),
		MIN5(TicksPerSecond.MINUTES_5, "5 minutes"),
		MIN15(TicksPerSecond.MINUTES_15, "15 minutes");

		private final TicksPerSecond window;
		private final String name;

		Window(TicksPerSecond window, String name) {
			this.window = window;
			this.name = name;
		}

	}

}
