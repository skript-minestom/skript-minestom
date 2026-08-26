package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Display Name")
@Description("""
	The display name of a team. Named separately from the plain name expression so it does not shadow it.
	Changing it updates every client that can see the team.""")
@Examples("set team display name of {_team} to mm(\"<red>Red Team\")")
@Keywords({"team", "scoreboard"})
public class ExprTeamDisplayName extends SimplePropertyExpression<Team, ComponentWrapper> {

	static {
		register(ExprTeamDisplayName.class, ComponentWrapper.class, "team display name", "teams");
	}

	@Override
	public @Nullable ComponentWrapper convert(Team from) {
		return ComponentWrapper.toWrapper(from.getTeamDisplayName());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET)
			return CollectionUtils.array(ComponentWrapper.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Component component = Component.empty();
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta[0] == null) return;
			component = ((ComponentWrapper) delta[0]).getComponent();
		}
		for (Team team : getExpr().getArray(event)) {
			team.updateTeamDisplayName(component);
		}
	}

	@Override
	protected String getPropertyName() {
		return "team display name";
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

}
