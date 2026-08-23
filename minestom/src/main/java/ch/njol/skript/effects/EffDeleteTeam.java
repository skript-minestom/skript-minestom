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
import net.minestom.server.MinecraftServer;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Delete Team")
@Description("Unregisters a team and removes it from every client that can see it.")
@Examples("delete team named \"red\"")
@Keywords({"team", "scoreboard", "delete"})
public class EffDeleteTeam extends Effect {

	static {
		Skript.registerEffect(EffDeleteTeam.class, "(delete|unregister) team[s] %teams%");
	}

	private Expression<Team> teams;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		teams = (Expression<Team>) expressions[0];
		return true;
	}

	@Override
	protected void execute(Event event) {
		for (Team team : teams.getArray(event)) {
			MinecraftServer.getTeamManager().deleteTeam(team);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "delete team " + teams.toString(event, debug);
	}

}
