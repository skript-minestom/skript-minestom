package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Name Tag Visibility")
@Description("""
	Who can see the name tags of a team.
	Changing it updates every client that can see the team.""")
@Examples("set name tag visibility of {_team} to hide for other teams")
@Keywords({"team", "scoreboard", "nametag"})
public class ExprTeamNameTagVisibility extends SimplePropertyExpression<Team, TeamsPacket.NameTagVisibility> {

	static {
		register(ExprTeamNameTagVisibility.class, TeamsPacket.NameTagVisibility.class, "name[ ]tag visibility", "teams");
	}

	@Override
	public TeamsPacket.NameTagVisibility convert(Team from) {
		return from.getNameTagVisibility();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(TeamsPacket.NameTagVisibility.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (delta == null || delta[0] == null) return;
		TeamsPacket.NameTagVisibility visibility = (TeamsPacket.NameTagVisibility) delta[0];
		for (Team team : getExpr().getArray(event)) {
			team.updateNameTagVisibility(visibility);
		}
	}

	@Override
	protected String getPropertyName() {
		return "name tag visibility";
	}

	@Override
	public Class<? extends TeamsPacket.NameTagVisibility> getReturnType() {
		return TeamsPacket.NameTagVisibility.class;
	}

}
