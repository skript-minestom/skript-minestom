package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.EntityEquipWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.minestom.server.entity.EquipmentSlot;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Locale;

public class EvtArmorChange extends SkriptEvent {

	static {
		Skript.registerEvent("Armor Change", EvtArmorChange.class, EntityEquipWrapper.class,
				"[entity] armo[u]r chang(e|ing)",
				"[entity] (1:helmet|2:chestplate|3:leggings|4:boots) chang(e|ing)")
			.description("""
				Called when an entity equips or unequips a piece of armor.
				This is fired before the armor is applied, so 'past event-item' is the piece being replaced \
				and 'event-item' is the piece being put on. Setting 'event-item' changes what ends up equipped.
				This event cannot be cancelled.""")
			.examples("""
				on armor change:
					broadcast "%event-entity% swapped %past event-item% for %event-item% in their %event-equipment slot%"

				on helmet change:
					if event-item is a diamond helmet:
						send "nice helmet" to event-entity""");
	}

	private @Nullable EquipmentSlot slot;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		slot = switch (parseResult.mark) {
			case 1 -> EquipmentSlot.HELMET;
			case 2 -> EquipmentSlot.CHESTPLATE;
			case 3 -> EquipmentSlot.LEGGINGS;
			case 4 -> EquipmentSlot.BOOTS;
			default -> null;
		};
		return true;
	}

	@Override
	public boolean check(Event event) {
		if (!(event instanceof EntityEquipWrapper wrapper)) return false;
		EquipmentSlot changedSlot = wrapper.getEvent().getSlot();
		if (slot != null) return changedSlot == slot;
		return changedSlot.isArmor();
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (slot == null ? "armor" : Classes.toString(slot)) + " change";
	}

}
