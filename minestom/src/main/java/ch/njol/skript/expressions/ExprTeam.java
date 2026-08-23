package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Team")
@Description("""
	An existing team, by name. Returns nothing if no team with that name exists.
	Deleting this unregisters the team and removes it from every client that can see it.
	Teams live in memory and are not saved across restarts, so scripts should recreate them on load.
	The word 'named' is required: without it the pattern would swallow expressions such as 'team prefix of {_t}'.""")
@Examples("""
	set {_team} to team named "red"

	if team named "red" is not set:
		set {_team} to a new team named "red\"""")
@Keywords({"team", "scoreboard"})
public class ExprTeam extends SimpleExpression<Team> {

	static {
		Skript.registerExpression(ExprTeam.class, Team.class, ExpressionType.COMBINED,
			"[the] team[s] named %strings%");
	}

	private Expression<String> names;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		names = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected Team @Nullable [] get(Event event) {
		List<Team> teams = new ArrayList<>();
		for (String name : names.getArray(event)) {
			Team team = MinecraftServer.getTeamManager().getTeam(name);
			if (team != null) teams.add(team);
		}
		return teams.toArray(new Team[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.DELETE) return CollectionUtils.array();
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (mode != Changer.ChangeMode.DELETE) return;
		for (Team team : getArray(event)) {
			MinecraftServer.getTeamManager().deleteTeam(team);
		}
	}

	@Override
	public boolean isSingle() {
		return names.isSingle();
	}

	@Override
	public Class<? extends Team> getReturnType() {
		return Team.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "team named " + names.toString(event, debug);
	}

}
