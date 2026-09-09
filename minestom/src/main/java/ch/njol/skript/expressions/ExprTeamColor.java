package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.TeamColors;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.color.TeamColor;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Color")
@Description("""
	The color of a team. This is what tints member name tags and their glow outline.
	Changing it updates every client that can see the team.""")
@Examples("""
	set team color of {_team} to dark red

	set glowing of player to true
	add player to members of {_team}""")
@Keywords({"team", "scoreboard", "color", "glow"})
public class ExprTeamColor extends SimplePropertyExpression<Team, NamedTextColor> {

	static {
		register(ExprTeamColor.class, NamedTextColor.class, "team colo[u]r", "teams");
	}

	@Override
	public @Nullable NamedTextColor convert(Team from) {
		return TeamColors.toNamedTextColor(from.getTeamColor());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(NamedTextColor.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		if (delta == null || delta[0] == null) return;
		TeamColor color = TeamColors.toTeamColor((NamedTextColor) delta[0]);
		if (color == null) return;
		for (Team team : getExpr().getArray(event)) {
			team.updateTeamColor(color);
		}
	}

	@Override
	protected String getPropertyName() {
		return "team color";
	}

	@Override
	public Class<? extends NamedTextColor> getReturnType() {
		return NamedTextColor.class;
	}

}
