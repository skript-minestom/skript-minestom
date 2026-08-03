package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.ServerListPingWrapper;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.ping.Status;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;


@Name("Protocol Version")
@Description("A player's protocol version, or the protocol version in a server list ping event.")
@Examples("""
	broadcast "Version: %protocol version of player%\"""")
public class ExprProtocolVersion extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprProtocolVersion.class, Object.class, ExpressionType.PROPERTY,
			"%players%'[s] [protocol] version",
			"[protocol] version of %players%",
			"motd [(player:player's)] [:protocol] version");
	}

	@Nullable
	private Expression<Player> players;

	private boolean motd;
	private boolean player;
	private boolean protocol;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		motd = matchedPattern == 2;
		player = parseResult.hasTag("player");
		protocol = parseResult.hasTag("protocol");

		if (motd) return verifyMOTDEvent(getParser(), "protocol version");

		players = (Expression<Player>) expressions[0];
		return true;
	}

	@Override
	protected @Nullable Object[] get(Event event) {
		if (motd) {
			ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
			Status.VersionInfo versionInfo = e.getStatus().versionInfo();
			if (player) {
				PlayerConnection connection = e.getConnection();
				// 80 as that was the highest non-modern server list ping protocol version
				return new Integer[]{connection == null ? 80 : connection.getProtocolVersion()};
			}
			if (protocol) return new Integer[]{versionInfo.protocolVersion()};
			return new String[]{versionInfo.name()};
		}
		assert this.players != null;
		Player[] players = this.players.getArray(event);
		Integer[] versions = new Integer[players.length];
		for (int i = 0; i < players.length; i++) {
			versions[i] = players[i].getPlayerConnection().getProtocolVersion();
		}
		return versions;
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (motd && !player && (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET)) return CollectionUtils.array(getExpectedType());
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Object o = delta == null ? null : delta[0];
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		Status oldStatus = e.getStatus();
		Status.VersionInfo versionInfo = oldStatus.versionInfo();
		String versionName = versionInfo.name();
		int protocolVersion = versionInfo.protocolVersion();
		if (mode == Changer.ChangeMode.RESET) {
			if (protocol) protocolVersion = MinecraftServer.PROTOCOL_VERSION;
			else versionName = MinecraftServer.VERSION_NAME;
		} else {
			if (o == null) return;
			if (protocol) protocolVersion = (Integer) o;
			else versionName = (String) o;
		}
		Status newStatus = Status.builder(oldStatus)
			.versionInfo(new Status.VersionInfo(versionName, protocolVersion))
			.build();
		e.setStatus(newStatus);
	}

	@Override
	public boolean isSingle() {
		if (motd) return true;
		assert players != null;
		return (players.isSingle());
	}

	@Override
	public Class<?> getReturnType() {
		return getExpectedType();
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		StringBuilder builder = new StringBuilder();
		if (motd) {
			builder.append("motd ");
			if (player) builder.append("player's ");
		} else {
			assert players != null;
			builder.append(players.toString(event, debug));
			builder.append("'s ");
		}
		if (protocol) builder.append("protocol ");
		builder.append("version");
		return builder.toString();
	}

	private Class<?> getExpectedType() {
		if (protocol || !motd || player) return Integer.class;
		return String.class;
	}

	static boolean verifyMOTDEvent(ParserInstance parser, String elementName) {
		if (!parser.isCurrentEvent(ServerListPingWrapper.class)) {
			Skript.error("Cannot use 'motd " + elementName + "' element outside of the server list ping event!");
			return false;
		}
		return true;
	}

}
