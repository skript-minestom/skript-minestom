package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.statistic.StatisticWindow.MillisPerTick;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.api.statistic.types.GenericStatistic;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToDoubleFunction;

@Name("MSPT")
@Description("""
	The duration of the server's ticks in milliseconds, as measured by spark.
	Defaults to the mean tick duration of the window, but the minimum, maximum, median and 95th percentile can be asked for instead.
	Without a window, this returns all three of spark's averages, in order: the last 10 seconds, minute and 5 minutes.""")
@Examples("""
	broadcast "%mspt from the last minute%"

	every 30 seconds:
		if 95th percentile mspt from the last 10 seconds > 45:
			broadcast mm("<red>The server is lagging!")""")
@Keywords({"mspt", "tick", "lag", "spark", "performance"})
public class ExprMSPT extends SimpleExpression<Number> {

	private static final String SUFFIX = " mspt [(from|over|in) [the] last (1:10 seconds|2:minute|3:5 minutes)]";

	static {
		Skript.registerExpression(ExprMSPT.class, Number.class, ExpressionType.SIMPLE,
			"[(mean|average)]" + SUFFIX,
			"min[imum]" + SUFFIX,
			"max[imum]" + SUFFIX,
			"median" + SUFFIX,
			"95th percentile" + SUFFIX);
	}

	private Statistic statistic;
	@Nullable
	private Window window;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		statistic = Statistic.values()[matchedPattern];
		if (parseResult.mark != 0) window = Window.values()[parseResult.mark-1];
		return true;
	}

	@Override
	protected Number[] get(Event event) {
		GenericStatistic<DoubleAverageInfo, MillisPerTick> mspt = mspt();
		if (mspt == null) return new Number[0];
		if (window != null) return new Number[]{statistic.of(mspt.poll(window.window))};
		DoubleAverageInfo[] averages = mspt.poll();
		Number[] values = new Number[averages.length];
		for (int i = 0; i < averages.length; i++) values[i] = statistic.of(averages[i]);
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
		return statistic.name + " mspt" + (window == null ? "" : " from the last " + window.name);
	}

	@Nullable
	private static GenericStatistic<DoubleAverageInfo, MillisPerTick> mspt() {
		try {
			return SparkProvider.get().mspt();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	private enum Statistic {
		MEAN("mean", DoubleAverageInfo::mean),
		MIN("minimum", DoubleAverageInfo::min),
		MAX("maximum", DoubleAverageInfo::max),
		MEDIAN("median", DoubleAverageInfo::median),
		PERCENTILE95("95th percentile", DoubleAverageInfo::percentile95th);

		private final String name;
		private final ToDoubleFunction<DoubleAverageInfo> getter;

		Statistic(String name, ToDoubleFunction<DoubleAverageInfo> getter) {
			this.name = name;
			this.getter = getter;
		}

		private double of(DoubleAverageInfo average) {
			return getter.applyAsDouble(average);
		}

	}

	private enum Window {
		SEC10(MillisPerTick.SECONDS_10, "10 seconds"),
		MIN1(MillisPerTick.MINUTES_1, "minute"),
		MIN5(MillisPerTick.MINUTES_5, "5 minutes");

		private final MillisPerTick window;
		private final String name;

		Window(MillisPerTick window, String name) {
			this.window = window;
			this.name = name;
		}

	}

}
