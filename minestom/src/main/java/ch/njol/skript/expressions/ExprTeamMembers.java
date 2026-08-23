package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Team Members")
@Description("""
	The members of a team, as the team stores them: a username for a player, a uuid for anything else.
	Adding a player stores their username and adding an entity stores its uuid, so either can be added directly.
	See also 'players of team' and 'entities of team', which resolve these back into real objects.""")
@Examples("""
	add player to members of {_team}
	remove player from members of {_team}
	clear members of {_team}

	loop members of {_team}:
		broadcast loop-value""")
@Keywords({"team", "scoreboard", "member"})
public class ExprTeamMembers extends PropertyExpression<Team, String> {

	static {
		register(ExprTeamMembers.class, String.class, "[team] members", "teams");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Team>) expressions[0]);
		return true;
	}

	@Override
	protected String[] get(Event event, Team[] source) {
		List<String> members = new ArrayList<>();
		for (Team team : source) {
			members.addAll(team.getMembers());
		}
		return members.toArray(new String[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case ADD, REMOVE, SET, DELETE, RESET -> CollectionUtils.array(Object[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Team[] teams = getExpr().getArray(event);
		if (teams.length == 0) return;

		if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
			for (Team team : teams) {
				team.removeMembers(new ArrayList<>(team.getMembers()));
			}
			return;
		}
		if (delta == null) return;

		List<String> names = new ArrayList<>();
		for (Object object : delta) {
			String name = toMemberName(object);
			if (name != null) names.add(name);
		}
		if (names.isEmpty() && mode != Changer.ChangeMode.SET) return;

		for (Team team : teams) {
			switch (mode) {
				case SET -> {
					team.removeMembers(new ArrayList<>(team.getMembers()));
					team.addMembers(names);
				}
				case ADD -> team.addMembers(names);
				default -> team.removeMembers(names);
			}
		}
	}

	/**
	 * Minestom keys team members by name: a player by username, anything else by uuid.
	 */
	private static @Nullable String toMemberName(Object object) {
		if (object instanceof Player player) return player.getUsername();
		if (object instanceof Entity entity) return entity.getUuid().toString();
		if (object instanceof String string) return string;
		return null;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "team members of " + getExpr().toString(event, debug);
	}

}
