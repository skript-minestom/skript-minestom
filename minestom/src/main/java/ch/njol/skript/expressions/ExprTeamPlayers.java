package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Team Players")
@Description("""
	Every online player on a team.
	Members who are offline, or who are entities rather than players, are not included.""")
@Examples("""
	loop players of {_team}:
		send "your team is winning" to loop-value""")
@Keywords({"team", "scoreboard", "player"})
public class ExprTeamPlayers extends PropertyExpression<Team, Player> {

	static {
		register(ExprTeamPlayers.class, Player.class, "[team] players", "teams");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Team>) expressions[0]);
		return true;
	}

	@Override
	protected Player[] get(Event event, Team[] source) {
		List<Player> players = new ArrayList<>();
		for (Team team : source) {
			players.addAll(team.getPlayers());
		}
		return players.toArray(new Player[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Player> getReturnType() {
		return Player.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "team players of " + getExpr().toString(event, debug);
	}

}
