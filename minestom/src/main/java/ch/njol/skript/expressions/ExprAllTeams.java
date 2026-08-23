package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.minestom.server.MinecraftServer;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("All Teams")
@Description("Every team currently registered on the server.")
@Examples("""
	loop all teams:
		broadcast "%team name of loop-value%\"""")
@Keywords({"team", "scoreboard"})
public class ExprAllTeams extends SimpleExpression<Team> {

	static {
		Skript.registerExpression(ExprAllTeams.class, Team.class, ExpressionType.SIMPLE,
			"[all [[of] the]] teams");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected Team @Nullable [] get(Event event) {
		return MinecraftServer.getTeamManager().getTeams().toArray(new Team[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Team> getReturnType() {
		return Team.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "all teams";
	}

}
