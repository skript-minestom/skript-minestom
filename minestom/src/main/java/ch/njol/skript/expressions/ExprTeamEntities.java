package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Team;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Name("Team Entities")
@Description("""
	Every non-player entity on a team, resolved from the uuids the team stores.
	Entities that no longer exist are skipped.""")
@Examples("""
	loop entities of {_team}:
		set glowing of loop-value to true""")
@Keywords({"team", "scoreboard", "entity"})
public class ExprTeamEntities extends PropertyExpression<Team, Entity> {

	static {
		register(ExprTeamEntities.class, Entity.class, "[team] entities", "teams");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Team>) expressions[0]);
		return true;
	}

	@Override
	protected Entity[] get(Event event, Team[] source) {
		List<Entity> entities = new ArrayList<>();
		for (Team team : source) {
			for (String member : MinecraftServer.getTeamManager().getEntities(team)) {
				Entity entity = findEntity(member);
				if (entity != null) entities.add(entity);
			}
		}
		return entities.toArray(new Entity[0]);
	}

	private static @Nullable Entity findEntity(String member) {
		UUID uuid;
		try {
			uuid = UUID.fromString(member);
		} catch (IllegalArgumentException e) {
			return null;
		}
		for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
			Entity entity = instance.getEntityByUuid(uuid);
			if (entity != null) return entity;
		}
		return null;
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
		return "team entities of " + getExpr().toString(event, debug);
	}

}
