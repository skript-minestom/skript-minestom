package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

@Name("Make Say")
@Description("""
	Forces a player to send a message to the chat.
	If the message starts with a slash the player is made to run it as a command instead.

	A chat message goes through the normal chat pipeline, so the chat event fires and any format or recipient
	changes made there apply, exactly as if the player had typed it.""")
@Examples("""
	make the player say "Hello."

	force all players to send the message "I love this server"

	make player say "/spawn\"""")
@Keywords({"chat", "say", "force"})
public class EffMakeSay extends Effect {

	static {
		Skript.registerEffect(EffMakeSay.class,
			"make %players% (say|send [the] message[s]) %strings%",
			"force %players% to (say|send [the] message[s]) %strings%");
	}

	private Expression<Player> players;
	private Expression<String> messages;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		players = (Expression<Player>) expressions[0];
		messages = (Expression<String>) expressions[1];
		return true;
	}

	@Override
	protected void execute(Event event) {
		String[] messages = this.messages.getArray(event);
		if (messages.length == 0) return;
		for (Player player : players.getArray(event)) {
			for (String message : messages) {
				if (message.isEmpty()) continue;
				if (message.startsWith("/")) {
					// the command manager expects the command without its leading slash
					MinecraftServer.getCommandManager().execute(player, message.substring(1));
				} else {
					chat(player, message);
				}
			}
		}
	}

	private static void chat(Player player, String message) {
		List<Player> recipients = List.copyOf(MinecraftServer.getConnectionManager().getOnlinePlayers());
		PlayerChatEvent chatEvent = new PlayerChatEvent(player, recipients, message);
		MinecraftServer.getGlobalEventHandler().call(chatEvent);
		if (chatEvent.isCancelled()) return;
		Component formattedMessage = chatEvent.getFormattedMessage();
		for (Player recipient : chatEvent.getRecipients()) {
			recipient.sendMessage(formattedMessage);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "make " + players.toString(event, debug) + " say " + messages.toString(event, debug);
	}

}
