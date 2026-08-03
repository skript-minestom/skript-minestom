package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.EntityAttackWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.entity.EntityAttackEvent;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtAttack extends SkriptEvent {

	static {
		Skript.registerEvent("Entity Attack", EvtAttack.class, EntityAttackWrapper.class,
			"(%-entitytypes%|entity) attack [on %-entitytypes%]")
			.description("""
				Called when one entity attacks another.
				The attacking entity can be referred to as 'attacker' and the entity being attacked as 'victim'.
				Optionally filter by the attacker's entity type and/or the victim's entity type.""")
			.examples("""
				on entity attack:
				on player attack:
				on zombie attack on villager:
				on entity attack on player:""");
	}

	@Nullable
	private Literal<EntityType> attackerTypes;
	@Nullable
	private Literal<EntityType> victimTypes;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		attackerTypes = (Literal<EntityType>) args[0];
		victimTypes = (Literal<EntityType>) args[1];
		return true;
	}

	@Override
	public boolean check(Event event) {
		EntityAttackEvent e = ((EntityAttackWrapper) event).getEvent();
		boolean attackerCheck = check(attackerTypes, e.getEntity().getEntityType());
		boolean victimCheck = check(victimTypes, e.getTarget().getEntityType());
		return attackerCheck && victimCheck;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (attackerTypes != null ? attackerTypes.toString(event, debug) : "entity") + " attack"
			+ (victimTypes != null ? " of " + victimTypes.toString(event, debug) : "");
	}

	private boolean check(@Nullable Literal<EntityType> entityTypes, EntityType type) {
		if (entityTypes == null) return true;
		for (EntityType t : entityTypes.getAll()) {
			if (t.equals(type)) {
				return true;
			}
		}
		return false;
	}

}
