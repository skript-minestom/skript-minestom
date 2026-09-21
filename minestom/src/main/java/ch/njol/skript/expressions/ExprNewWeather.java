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
import ch.njol.util.Kleenean;
import net.minestom.server.instance.Weather;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Weather")
@Description("""
	Creates a weather with the given rain and thunder levels. Both are numbers between 0 and 1 \
	and are clamped to that range. A level that isn't given defaults to 0.""")
@Examples("""
	set weather of {_instance} to new weather with rain level 0.4 and thunder level 0.2
	set {_drizzle} to new weather with rain level 0.15""")
public class ExprNewWeather extends SimpleExpression<Weather> {

	static {
		Skript.registerExpression(ExprNewWeather.class, Weather.class, ExpressionType.COMBINED,
			"[a] [new] weather with rain level %number% [[(,|and)] [with] thunder level %number%]",
			"[a] [new] weather with thunder level %number%");
	}

	private @Nullable Expression<Number> rainLevel;
	private @Nullable Expression<Number> thunderLevel;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) {
			rainLevel = (Expression<Number>) expressions[0];
			thunderLevel = (Expression<Number>) expressions[1];
		} else {
			thunderLevel = (Expression<Number>) expressions[0];
		}
		return true;
	}

	@Override
	protected Weather @Nullable [] get(Event event) {
		Float rain = levelOf(rainLevel, event);
		if (rain == null) return new Weather[0];
		Float thunder = levelOf(thunderLevel, event);
		if (thunder == null) return new Weather[0];
		return new Weather[]{new Weather(rain, thunder)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Weather> getReturnType() {
		return Weather.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("new weather");
		if (rainLevel != null) sb.append("with rain level", rainLevel);
		if (thunderLevel != null) sb.append((rainLevel == null ? "with" : "and with") + " thunder level", thunderLevel);
		return sb.toString();
	}

	private static @Nullable Float levelOf(@Nullable Expression<Number> expression, Event event) {
		if (expression == null) return 0f;
		Number number = expression.getSingle(event);
		if (number == null) return null;
		double level = number.doubleValue();
		if (Double.isNaN(level)) return null;
		return (float) Math.max(0, Math.min(1, level));
	}

}
