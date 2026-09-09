package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.scoreboard.Team;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Name")
@Description("The name a team was registered under. This is its identity and cannot be changed.")
@Examples("broadcast \"you are on %team name of team of player%\"")
@Keywords({"team", "scoreboard"})
public class ExprTeamName extends SimplePropertyExpression<Team, String> {

	static {
		register(ExprTeamName.class, String.class, "team name", "teams");
	}

	@Override
	public @Nullable String convert(Team from) {
		return from.getTeamName();
	}

	@Override
	protected String getPropertyName() {
		return "team name";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
