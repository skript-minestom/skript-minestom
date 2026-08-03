package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;


@Name("Ping")
@Description("A player's network latency in milliseconds. Is able to be spoofed as well, but will be overridden when the server is pinged.")
@Examples("""
	broadcast "Ping: %ping of player%\"""")
public class ExprPing extends SimplePropertyExpression<Player, Integer> {

	static {
		register(ExprPing.class, Integer.class, "(ping|latency)", "players");
	}

	@Override
	public @Nullable Integer convert(Player from) {
		return from.getLatency();
	}

	@Override
	public Class<?> @org.jetbrains.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(Integer.class);
		return null;
	}

	@Override
	public void change(Event event, Object @org.jetbrains.annotations.Nullable [] delta, Changer.ChangeMode mode) {
		Integer latency = delta == null ? null : (Integer) delta[0];
		if (latency == null) return;
		for (Player p : getExpr().getArray(event)) {
			p.refreshLatency(latency);
		}
	}

	@Override
	protected String getPropertyName() {
		return "ping";
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

}
