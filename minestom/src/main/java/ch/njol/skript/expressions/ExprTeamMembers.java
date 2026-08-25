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
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.utils.entity.EntityFinder;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.*;

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
public class ExprTeamMembers extends PropertyExpression<Team, Entity> {

	static {
		register(ExprTeamMembers.class, Entity.class, "[team] members", "teams");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Team>) expressions[0]);
		return true;
	}

	@Override
	protected Entity[] get(Event event, Team[] source) {
		List<Entity> members = new ArrayList<>();
		for (Team team : source) {
			members.addAll(getMembers(team));
		}
		return members.toArray(new Entity[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case ADD, REMOVE, SET, DELETE -> CollectionUtils.array(Entity[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Team[] teams = getExpr().getArray(event);
		if (teams.length == 0) return;

		if (mode == Changer.ChangeMode.DELETE) {
			for (Team team : teams) {
				clearMembers(team);
			}
			return;
		}
		if (delta == null) return;

		List<Object> entities = new ArrayList<>();
		Collections.addAll(entities, delta);
		if (entities.isEmpty() && mode != Changer.ChangeMode.SET) return;

		for (Team team : teams) {
			switch (mode) {
				case SET -> {
					clearMembers(team);
					addMember(team, entities);
				}
				case ADD -> addMember(team, entities);
				default -> removeTeam(team, entities);
			}
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "team members of " + getExpr().toString(event, debug);
	}

	private void addMember(Team team, List<Object> objects) {
		for (Object o : objects) {
			if (o instanceof Entity entity) entity.setTeam(team);
		}
	}

	private void removeTeam(Team team, List<Object> objects) {
		for (Object object : objects) {
			if (object instanceof Entity entity && team.equals(entity.getTeam())) entity.setTeam(null);
		}
	}

	private void clearMembers(Team team) {
		for (Entity entity : getMembers(team)) {
			if (entity != null && team.equals(entity.getTeam())) entity.setTeam(null);
		}
	}

	private Set<Entity> getMembers(Team team) {
		Set<Entity> members = new HashSet<>();
		TeamManager teamManager = MinecraftServer.getTeamManager();
		InstanceManager instanceManager = MinecraftServer.getInstanceManager();
		for (String entity : teamManager.getEntities(team)) {
			for (Instance instance : instanceManager.getInstances()) {
				Entity e = instance.getEntityByUuid(UUID.fromString(entity)); // safe parse because getEntities() checks that
				if (e != null) members.add(e);
			}
		}
		ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
		for (String username : teamManager.getPlayers(team)) {
			Player player = connectionManager.findOnlinePlayer(username);
			if (player != null) members.add(player);
		}
		return members;
	}

}
