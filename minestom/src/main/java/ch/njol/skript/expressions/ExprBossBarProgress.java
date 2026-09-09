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
	How full a boss bar is, as number between 0 and 1.
	Changing it updates the bar for everybody already seeing it.""")
@Examples("""
	set progress of {_bar} to 0.5

	every second:
		subtract 0.05 from progress of {_bar}""")
@Keywords({"boss bar", "bossbar"})
public class ExprBossBarProgress extends SimplePropertyExpression<BossBar, Number> {

	static {
		register(ExprBossBarProgress.class, Number.class, "[bar] progress", "bossbars");
	}

	@Override
	public Number convert(BossBar from) {
		return from.progress();
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
					yield ExprBossBar.toProgress(bossBar.progress() + change, bossBar.progress());
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
