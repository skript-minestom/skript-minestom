package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Of")
@Description("The team a player, entity or raw member name belongs to, if any.")
@Examples("""
	if team of player is set:
		broadcast team name of team of player""")
@Keywords({"team", "scoreboard"})
public class ExprTeamOf extends SimplePropertyExpression<Object, Team> {

	static {
		register(ExprTeamOf.class, Team.class, "team", "players/entities/strings");
	}

	@Override
	public @Nullable Team convert(Object from) {
		String member = toMemberName(from);
		if (member == null) return null;
		for (Team team : MinecraftServer.getTeamManager().getTeams()) {
			if (team.getMembers().contains(member)) return team;
		}
		return null;
	}

	private static @Nullable String toMemberName(Object object) {
		if (object instanceof Player player) return player.getUsername();
		if (object instanceof Entity entity) return entity.getUuid().toString();
		if (object instanceof String string) return string;
		return null;
	}

	@Override
	protected String getPropertyName() {
		return "team";
	}

	@Override
	public Class<? extends Team> getReturnType() {
		return Team.class;
	}

}
