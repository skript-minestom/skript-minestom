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
import net.minestom.server.scoreboard.TeamManager;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("New Team")
@Description("""
	Creates and registers a team with the given name.
	If a team with that name already exists it is returned unchanged, so reloading a script does not fail.""")
@Examples("""
	set {_team} to a new team named "red"
	set team color of {_team} to dark red
	add player to members of {_team}""")
@Keywords({"team", "scoreboard", "create"})
public class ExprNewTeam extends SimpleExpression<Team> {

	static {
		Skript.registerExpression(ExprNewTeam.class, Team.class, ExpressionType.COMBINED,
			"[a] new team [named] %string%");
	}

	private Expression<String> name;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		name = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected Team @Nullable [] get(Event event) {
		String name = this.name.getSingle(event);
		if (name == null) return new Team[0];
		TeamManager teamManager = MinecraftServer.getTeamManager();
		Team existing = teamManager.getTeam(name);
		if (existing != null) return new Team[]{existing};
		return new Team[]{teamManager.createTeam(name)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Team> getReturnType() {
		return Team.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "a new team named " + name.toString(event, debug);
	}

}
