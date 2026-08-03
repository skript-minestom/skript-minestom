package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.minestom.server.MinecraftServer;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Online Player Count")
@Description("The number of online players.")
@Examples("broadcast \"Online: %number of online players%\"")
public class ExprOnlinePlayerCount extends SimpleExpression<Integer> {

	static {
		Skript.registerExpression(ExprOnlinePlayerCount.class, Integer.class, ExpressionType.SIMPLE, "[online] player count");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected @Nullable Integer[] get(Event event) {
		return new Integer[]{MinecraftServer.getConnectionManager().getOnlinePlayerCount()};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "online player count";
	}

}
