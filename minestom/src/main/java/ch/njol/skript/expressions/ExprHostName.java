package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import org.jspecify.annotations.Nullable;


@Name("Host Name")
@Description("The hostname a player connected with.")
@Examples("broadcast \"%host name of player%\"")
public class ExprHostName extends SimplePropertyExpression<Player, String> {

	static {
		register(ExprHostName.class, String.class, "host[ ]name", "players");
	}

	@Override
	public @Nullable String convert(Player from) {
		PlayerConnection playerConnection = from.getPlayerConnection();
		int port = playerConnection.getServerPort();
		return playerConnection.getServerAddress() + (port != 25565 ? (":" + port) : "");
	}

	@Override
	protected String getPropertyName() {
		return "host name";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
