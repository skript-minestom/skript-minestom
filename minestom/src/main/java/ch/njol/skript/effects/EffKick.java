package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.minestom.CustomConnectEvent;
import ch.njol.skript.events.wrapper.CustomConnectWrapper;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Kick")
@Description("Kicks the given players from the server with an optional message. During a connect event, kicks the connecting player through that event instead.")
@Examples("""
	kick player due to "You have been banned\"""")
public class EffKick extends Effect {

	static {
		Skript.registerEffect(EffKick.class, "kick %players% [due to %-component%]");
	}

	private Expression<Player> players;
	@Nullable
	private Expression<ComponentWrapper> message;
	private boolean connectEvent;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		players = (Expression<Player>) expressions[0];
		message = (Expression<ComponentWrapper>) expressions[1];
		connectEvent = getParser().isCurrentEvent(CustomConnectWrapper.class);
		return true;
	}

	@Override
	protected void execute(Event event) {
		Component message = ComponentWrapper.getOrElse(this.message, event, Component.empty());
		if (message == null) return;
		for (Player player : players.getArray(event)) {
			if (connectEvent) {
				CustomConnectEvent e = ((CustomConnectWrapper) event).getEvent();
				if (e.getPlayer().equals(player)) {
					e.kick(message);
					continue;
				}
			}
			player.kick(message);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "kick " + players.toString(event, debug) + (message == null ? "" : message.toString(event, debug));
	}

}
