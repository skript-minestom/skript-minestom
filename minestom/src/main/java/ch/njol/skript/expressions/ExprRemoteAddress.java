package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.ServerListPingWrapper;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.network.player.PlayerConnection;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.expressions.ExprProtocolVersion.verifyMOTDEvent;


@Name("Remote Address")
@Description("A player's IP address or remote address.")
@Examples("""
	broadcast "%ip of player%\"""")
public class ExprRemoteAddress extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprRemoteAddress.class, String.class, ExpressionType.PROPERTY,
			"%players%'[s] (ip[s]|remote address[es])",
			"(ip[s]|remote address[es]) of %players%",
			"motd player's (ip|remote address)");
	}

	@Nullable
	private Expression<Player> players;

	private boolean motd;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		motd = matchedPattern == 2;

		if (motd) return verifyMOTDEvent(getParser(), "protocol version");

		players = (Expression<Player>) expressions[0];
		return true;
	}

	@Override
	protected @Nullable String[] get(Event event) {
		if (motd) {
			ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
			PlayerConnection connection = e.getConnection();
			if (connection == null) return new String[0];
			return new String[]{connection.getRemoteAddress().toString()};
		}
		assert this.players != null;
		Player[] players = this.players.getArray(event);
		String[] ips = new String[players.length];
		for (int i = 0; i < players.length; i++) {
			String ip = players[i].getPlayerConnection().getRemoteAddress().toString().replace("/", "");
			ips[i] = ip.contains(":") ? ip.substring(0, ip.indexOf(":")) : ip;
		}
		return ips;
	}

	@Override
	public boolean isSingle() {
		if (motd) return true;
		assert players != null;
		return (players.isSingle());
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		StringBuilder builder = new StringBuilder();
		if (motd) builder.append("motd player's ");
		else {
			assert players != null;
			builder.append(players.toString(event, debug));
			builder.append("'s ");
		}
		builder.append("ip");
		return builder.toString();
	}

}
