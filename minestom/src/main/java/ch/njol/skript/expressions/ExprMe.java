package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.events.EffectCommandEvent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Me")
@Description("A 'me' expression that can be used in players' effect commands only.")
@Examples("""
	!heal me
	!kick myself
	!give a diamond axe to me""")
@Since("2.1.1")
public class ExprMe extends SimpleExpression<Player> {

	static {
		Skript.registerExpression(ExprMe.class, Player.class, ExpressionType.SIMPLE, "me", "my[self]");
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		return getParser().isCurrentEvent(EffectCommandEvent.class);
	}

	@Override
	@Nullable
	protected Player[] get(Event e) {
		EffectCommandEvent event = (EffectCommandEvent) e;
		if (event.getExecutor() instanceof Player player) return new Player[]{player};
		return new Player[0];
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Player> getReturnType() {
		return Player.class;
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "me";
	}

}
