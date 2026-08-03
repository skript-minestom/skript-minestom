package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Action Bar")
@Description("Sends an action bar to the given players.")
@Examples("send action bar \"Hello World!\" to all players")
public class EffActionBar extends Effect {

	static {
		Skript.registerEffect(EffActionBar.class, "send action[ ]bar %component% [to %players%]");
	}

	private Expression<ComponentWrapper> component;
	private Expression<Player> players;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		component = (Expression<ComponentWrapper>) expressions[0];
		players = (Expression<Player>) expressions[1];
		return true;
	}

	@Override
	protected void execute(Event event) {
		ComponentWrapper wrapper = this.component.getSingle(event);
		if (wrapper == null) return;
		Component component = wrapper.getComponent();
		for (Player player : players.getArray(event)) {
			player.sendActionBar(component);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "send action bar " + component.toString(event, debug) + " to " + players.toString(event, debug);
	}

}
