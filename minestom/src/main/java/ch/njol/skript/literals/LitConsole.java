package ch.njol.skript.literals;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Console")
@Examples("""
	send "hi console" to console""")
public class LitConsole extends SimpleLiteral<ConsoleSender> {

	static {
		Skript.registerExpression(LitConsole.class, ConsoleSender.class, ExpressionType.SIMPLE, "[server] console");
	}

	public LitConsole() {
		super(MinecraftServer.getCommandManager().getConsoleSender(), true);
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "console";
	}

}
