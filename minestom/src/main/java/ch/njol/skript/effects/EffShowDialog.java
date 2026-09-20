package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.skript.util.dialog.SkriptDialogs;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Show / Close Dialog")
@Description("""
	Shows a dialog to players, or closes whatever dialog they currently have open.
	A dialog can be given as a value or as the namespaced key of a registered dialog,
	including dialogs that came from a datapack.""")
@Examples("""
	show {_d} to player
	show "myserver:shop" to all players
	close dialog of player""")
@Keywords({"dialog"})
public class EffShowDialog extends Effect {

	static {
		Skript.registerEffect(EffShowDialog.class,
			"show [dialog] %dialog/string% to %players%",
			"close [current] dialog [of %players%]");
	}

	private boolean close;
	private @Nullable Expression<Object> dialog;
	private Expression<Player> players;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		close = matchedPattern == 1;
		if (close) players = (Expression<Player>) expressions[0];
		else {
			dialog = (Expression<Object>) expressions[0];
			players = (Expression<Player>) expressions[1];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		if (close) {
			for (Player player : players.getArray(event)) {
				player.closeDialog();
			}
			return;
		}
		Object value = dialog.getSingle(event);
		if (value == null) return;
		Dialog resolved = resolve(value);
		if (resolved == null) return;
		for (Player player : players.getArray(event)) {
			player.showDialog(resolved);
		}
	}

	private @Nullable Dialog resolve(Object value) {
		if (value instanceof DialogWrapper wrapper) return wrapper.toMinestom();
		if (!(value instanceof String id)) return null;
		if (!Key.parseable(id)) {
			SkriptLogger.LOGGER.error("'{}' is not a valid dialog key. Format is 'namespace:value'.", id);
			return null;
		}
		Key key = Key.key(id);
		DialogWrapper own = SkriptDialogs.get(key);
		if (own != null) return own.toMinestom();
		Dialog registered = MinecraftServer.getDialogRegistry().get(key);
		if (registered == null) {
			SkriptLogger.LOGGER.error("No dialog is registered under '{}'.", id);
			return null;
		}
		return registered;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (close) return "close dialog of " + players.toString(event, debug);
		return "show dialog " + dialog.toString(event, debug) + " to " + players.toString(event, debug);
	}

}
