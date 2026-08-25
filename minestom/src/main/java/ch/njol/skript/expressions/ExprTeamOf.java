package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Of")
@Description("The team a player, entity or raw member name belongs to, if any.")
@Examples("""
	if team of player is set:
		broadcast team name of team of player""")
@Keywords({"team", "scoreboard"})
public class ExprTeamOf extends SimplePropertyExpression<Entity, Team> {

	static {
		register(ExprTeamOf.class, Team.class, "team", "entities");
	}

	@Override
	public @Nullable Team convert(Entity from) {
		return from.getTeam();
	}

	@Override
	public Class<?> @org.jetbrains.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) return CollectionUtils.array(Team.class);
		return null;
	}

	@Override
	public void change(Event event, Object @org.jetbrains.annotations.Nullable [] delta, Changer.ChangeMode mode) {
		Team team = delta == null ? null : (Team) delta[0];
		for (Entity entity : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.DELETE) entity.setTeam(null);
			else if (team != null) entity.setTeam(team);
		}
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
