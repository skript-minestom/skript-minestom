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

@Name("Boss Bar Overlay")
@Description("""
	The notching of a boss bar, which is either progress for a solid bar or one of the notched styles.
	Changing it updates the bar for everybody already seeing it.""")
@Examples("set overlay of {_bar} to notched 12")
@Keywords({"boss bar", "bossbar", "notched"})
public class ExprBossBarOverlay extends SimplePropertyExpression<BossBar, BossBar.Overlay> {

	static {
		register(ExprBossBarOverlay.class, BossBar.Overlay.class, "[bar] overlay", "bossbars");
	}

	@Override
	public BossBar.Overlay convert(BossBar from) {
		return from.overlay();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(BossBar.Overlay.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta[0] == null) return;
			overlay = (BossBar.Overlay) delta[0];
		}
		for (BossBar bossBar : getExpr().getArray(event)) {
			bossBar.overlay(overlay);
		}
	}

	@Override
	protected String getPropertyName() {
		return "overlay";
	}

	@Override
	public Class<? extends BossBar.Overlay> getReturnType() {
		return BossBar.Overlay.class;
	}

}
