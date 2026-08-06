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

@Name("Boss Bar Color")
@Description("The color of a boss bar. Changing it updates the bar for everybody already seeing it.")
@Examples("""
	set color of {_bar} to red

	if progress of {_bar} < 25:
		set color of {_bar} to red""")
@Keywords({"boss bar", "bossbar", "color"})
public class ExprBossBarColor extends SimplePropertyExpression<BossBar, BossBar.Color> {

	static {
		register(ExprBossBarColor.class, BossBar.Color.class, "colo[u]r", "bossbars");
	}

	@Override
	public BossBar.Color convert(BossBar from) {
		return from.color();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(BossBar.Color.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		BossBar.Color color = BossBar.Color.WHITE;
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta[0] == null) return;
			color = (BossBar.Color) delta[0];
		}
		for (BossBar bossBar : getExpr().getArray(event)) {
			bossBar.color(color);
		}
	}

	@Override
	protected String getPropertyName() {
		return "color";
	}

	@Override
	public Class<? extends BossBar.Color> getReturnType() {
		return BossBar.Color.class;
	}

}
