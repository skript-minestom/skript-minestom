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
import net.minestom.server.instance.WorldBorder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Transition World Border")
@Description("""
	Moves the world border of an instance to a new state over the given duration. Only the diameter \
	transitions, so a new center or warning setting applies right away. Setting a world border without \
	a duration changes it instantly.""")
@Examples("""
	transition world border of {_instance} to {_border} over 30 seconds
	shrink world border of {_instance} to 50 over 30 seconds
	grow world border of {_instance} to 500 over 1 minute""")
public class EffTransitionWorldBorder extends Effect {

	static {
		Skript.registerEffect(EffTransitionWorldBorder.class,
			"transition [the] world border (of|in) %instances% to %worldborder% over %timespan%",
			"(shrink|grow|resize) [the] world border (of|in) %instances% to [a] [diameter] [of] %number% over %timespan%");
	}

	private Expression<Instance> instances;
	private @Nullable Expression<WorldBorder> border;
	private @Nullable Expression<Number> diameter;
	private Expression<Timespan> duration;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		instances = (Expression<Instance>) expressions[0];
		if (matchedPattern == 0) {
			border = (Expression<WorldBorder>) expressions[1];
		} else {
			diameter = (Expression<Number>) expressions[1];
		}
		duration = (Expression<Timespan>) expressions[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Timespan duration = this.duration.getSingle(event);
		if (duration == null) return;
		double seconds = duration.getDuration().toMillis() / 1000d;

		WorldBorder border = null;
		if (this.border != null) {
			border = this.border.getSingle(event);
			if (border == null) return;
		}

		Double diameter = null;
		if (this.diameter != null) {
			Number number = this.diameter.getSingle(event);
			if (number == null || Double.isNaN(number.doubleValue())) return;
			diameter = Math.max(0, number.doubleValue());
		}

		for (Instance instance : instances.getArray(event)) {
			WorldBorder target = border != null ? border : instance.getWorldBorder().withDiameter(diameter);
			instance.setWorldBorder(target, seconds);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		if (border != null) {
			sb.append("transition world border of", instances, "to", border);
		} else {
			sb.append("resize world border of", instances, "to", diameter);
		}
		sb.append("over", duration);
		return sb.toString();
	}

}
