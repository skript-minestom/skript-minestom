package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.BossBarColor;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Boss Bar")
@Description("""
	Creates a new boss bar.
	Progress is a percentage between 0 and 100 and is clamped to that range, defaulting to 100.
	The color defaults to white and the overlay to progress.
	A boss bar is not shown to anybody until it is shown to them.""")
@Examples("""
	set {_bar} to new boss bar titled "<red>Dragon"
	add {_bar} to boss bars of all players

	set {_bar} to new boss bar titled "Event starting" with progress 0 and with color green bar""")
@Keywords({"boss bar", "bossbar"})
public class ExprBossBar extends SimpleExpression<BossBar> {

	static {
		Skript.registerExpression(ExprBossBar.class, BossBar.class, ExpressionType.COMBINED,
			"[new] boss[ ]bar [(with [the] title|titled) %-component%] [[and] with progress %-number%] [[and] with colo[u]r %-bossbarcolor%] [[and] with overlay %-bossbaroverlay%]");
	}

	@Nullable
	private Expression<ComponentWrapper> title;
	@Nullable
	private Expression<Number> progress;
	@Nullable
	private Expression<BossBarColor> color;
	@Nullable
	private Expression<BossBar.Overlay> overlay;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		title = (Expression<ComponentWrapper>) expressions[0];
		progress = (Expression<Number>) expressions[1];
		color = (Expression<BossBarColor>) expressions[2];
		overlay = (Expression<BossBar.Overlay>) expressions[3];
		return true;
	}

	@Override
	protected BossBar[] get(Event event) {
		Component title = ComponentWrapper.getOrElse(this.title, event, Component.empty());
		assert title != null;
		float progress = BossBar.MAX_PROGRESS;
		if (this.progress != null) progress = toProgress(this.progress.getSingle(event), BossBar.MAX_PROGRESS);
		BossBar.Color color = BossBar.Color.WHITE;
		if (this.color != null) {
			BossBarColor single = this.color.getSingle(event);
			if (single != null) color = single.getColor();
		}
		BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
		if (this.overlay != null) {
			BossBar.Overlay single = this.overlay.getSingle(event);
			if (single != null) overlay = single;
		}
		return new BossBar[]{BossBar.bossBar(title, progress, color, overlay)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends BossBar> getReturnType() {
		return BossBar.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("new boss bar");
		if (title != null) builder.append("titled", title);
		if (progress != null) builder.append("with progress", progress);
		if (color != null) builder.append("with color", color);
		if (overlay != null) builder.append("with overlay", overlay);
		return builder.toString();
	}

	public static float toProgress(@Nullable Number percent, float defaultProgress) {
		if (percent == null) return defaultProgress;
		double value = percent.doubleValue();
		if (Double.isNaN(value)) return defaultProgress;
		return (float) (Math.clamp(value, 0, 100) / 100);
	}

	public static double toPercent(float progress) {
		return progress * 100D;
	}

}
