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

@Name("Team Collision Rule")
@Description("""
	Whether members of a team push each other, push other teams, or never push at all.
	Changing it updates every client that can see the team.""")
@Examples("set collision rule of {_team} to never")
@Keywords({"team", "scoreboard", "collision", "push"})
public class ExprTeamCollisionRule extends SimplePropertyExpression<Team, TeamsPacket.CollisionRule> {

	static {
		register(ExprTeamCollisionRule.class, TeamsPacket.CollisionRule.class, "collision rule", "teams");
	}

	@Override
	public TeamsPacket.CollisionRule convert(Team from) {
		return from.getCollisionRule();
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(TeamsPacket.CollisionRule.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (delta == null || delta[0] == null) return;
		TeamsPacket.CollisionRule rule = (TeamsPacket.CollisionRule) delta[0];
		for (Team team : getExpr().getArray(event)) {
			team.updateCollisionRule(rule);
		}
	}

	@Override
	protected String getPropertyName() {
		return "collision rule";
	}

	@Override
	public Class<? extends TeamsPacket.CollisionRule> getReturnType() {
		return TeamsPacket.CollisionRule.class;
	}

}
