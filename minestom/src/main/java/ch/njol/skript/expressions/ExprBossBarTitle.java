package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;

@Name("Boss Bar Title")
@Description("The title of a boss bar. Changing it updates the bar for everybody already seeing it.")
@Examples("set title of {_bar} to \"<red>Dragon: <white>%health of {_dragon}%\"")
@Keywords({"boss bar", "bossbar"})
public class ExprBossBarTitle extends SimplePropertyExpression<BossBar, ComponentWrapper> {

	static {
		register(ExprBossBarTitle.class, ComponentWrapper.class, "title", "bossbars");
	}

	@Override
	public ComponentWrapper convert(BossBar from) {
		return toWrapper(from.name());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(ComponentWrapper.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Component title = Component.empty();
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta[0] == null) return;
			title = ((ComponentWrapper) delta[0]).getComponent();
		}
		for (BossBar bossBar : getExpr().getArray(event)) {
			bossBar.name(title);
		}
	}

	@Override
	protected String getPropertyName() {
		return "title";
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

}
