package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Boss Bar Progress")
@Description("""
	How full a boss bar is, as a percentage between 0 and 100.
	Values outside of that range are clamped rather than rejected, so subtracting 30 from a bar at 10% leaves it empty instead of erroring.
	Changing it updates the bar for everybody already seeing it.""")
@Examples("""
	set progress of {_bar} to 50

	every second:
		remove 5 from progress of {_bar}""")
@Keywords({"boss bar", "bossbar"})
public class ExprBossBarProgress extends SimplePropertyExpression<BossBar, Number> {

	static {
		register(ExprBossBarProgress.class, Number.class, "progress", "bossbars");
	}

	@Override
	public Number convert(BossBar from) {
		return ExprBossBar.toPercent(from.progress());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(Number.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Number value = delta == null ? null : (Number) delta[0];
		for (BossBar bossBar : getExpr().getArray(event)) {
			float progress = switch (mode) {
				case SET -> ExprBossBar.toProgress(value, bossBar.progress());
				case ADD, REMOVE -> {
					if (value == null) yield bossBar.progress();
					double change = mode == Changer.ChangeMode.ADD ? value.doubleValue() : -value.doubleValue();
					yield ExprBossBar.toProgress(ExprBossBar.toPercent(bossBar.progress()) + change, bossBar.progress());
				}
				case DELETE, RESET -> BossBar.MIN_PROGRESS;
				default -> bossBar.progress();
			};
			bossBar.progress(progress);
		}
	}

	@Override
	protected String getPropertyName() {
		return "progress";
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

}
