package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Team Prefix/Suffix")
@Description("""
	The prefix or suffix shown around the name of every member of a team.
	Deliberately prefixed with team so it does not collide with the LuckPerms prefix of a player.
	Changing it updates every client that can see the team.""")
@Examples("""
	set team prefix of {_team} to mm("<red>[R] ")
	set team suffix of {_team} to mm(" <gray>(red)")""")
@Keywords({"team", "scoreboard", "prefix", "suffix"})
public class ExprTeamPrefixSuffix extends SimplePropertyExpression<Team, ComponentWrapper> {

	static {
		register(ExprTeamPrefixSuffix.class, ComponentWrapper.class, "team (prefix|suffix:suffix)", "teams");
	}

	private boolean suffix;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		suffix = parseResult.hasTag("suffix");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable ComponentWrapper convert(Team from) {
		return ComponentWrapper.toWrapper(suffix ? from.getSuffix() : from.getPrefix());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(ComponentWrapper.class);
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
			if (suffix) team.updateSuffix(component);
			else team.updatePrefix(component);
		}
	}

	@Override
	protected String getPropertyName() {
		return "team " + (suffix ? "suffix" : "prefix");
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

}
