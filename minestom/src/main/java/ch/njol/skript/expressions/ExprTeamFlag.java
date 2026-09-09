package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Friendly Fire / See Invisible Teammates")
@Description("""
	Whether members of a team can damage each other, and whether they can see invisible teammates.
	Changing either updates every client that can see the team.""")
@Examples("""
	set friendly fire of {_team} to false
	set see invisible teammates of {_team} to true""")
@Keywords({"team", "scoreboard", "friendly fire", "invisible"})
public class ExprTeamFlag extends SimplePropertyExpression<Team, Boolean> {

	static {
		register(ExprTeamFlag.class, Boolean.class, "(friendly fire|invisible:see invisible [teammates])", "teams");
	}

	private boolean seeInvisible;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		seeInvisible = parseResult.hasTag("invisible");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable Boolean convert(Team from) {
		return seeInvisible ? from.isSeeInvisiblePlayers() : from.isAllowFriendlyFire();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(Boolean.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (delta == null || delta[0] == null) return;
		boolean value = (Boolean) delta[0];
		for (Team team : getExpr().getArray(event)) {
			if (seeInvisible) team.updateSeeInvisiblePlayers(value);
			else team.updateAllowFriendlyFire(value);
		}
	}

	@Override
	protected String getPropertyName() {
		return seeInvisible ? "see invisible teammates" : "friendly fire";
	}

	@Override
	public Class<? extends Boolean> getReturnType() {
		return Boolean.class;
	}

}
