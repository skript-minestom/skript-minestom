package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.EntitySpawnWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.minestom.server.entity.EntityType;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtSpawn extends SkriptEvent {

	static {
		Skript.registerEvent("Entity Spawn", EvtSpawn.class, EntitySpawnWrapper.class, "[%-entitytypes%] spawn[ing]")
			.description("""
				Called when an entity spawns in an instance.
				Optionally specify one or more entity types to only listen for those entities.""")
			.examples("""
				on spawn:
				on zombie spawn:
				on creeper or skeleton spawning:""");
	}

	@Nullable
	private Literal<EntityType> types;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		types = (Literal<EntityType>) args[0];
		return true;
	}

	@Override
	public boolean check(Event event) {
		if (types == null) return true;
		EntityType toCheck = ((EntitySpawnWrapper) event).getEvent().getEntity().getEntityType();
		EntityType[] types = this.types.getAll();
		for (EntityType type : types) {
			if (type.equals(toCheck)) return true;
		}
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (types != null ? types.toString(event, debug) + " " : "") + "spawn";
	}

}
