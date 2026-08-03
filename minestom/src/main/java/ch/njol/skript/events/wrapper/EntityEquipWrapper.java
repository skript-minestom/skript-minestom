package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.EntityInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Item;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.item.EntityEquipEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class EntityEquipWrapper extends EventWrapper<EntityEquipEvent> implements EntityInstanceEventMarker {

	static {
		EventValues.registerEventValue(EventValue.builder(EntityEquipWrapper.class, Item.class)
			.getter(from -> new Item(from.event.getEquippedItem()))
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setEquippedItem(value.getItem()))
			.build());
		EventValues.registerEventValue(EventValue.builder(EntityEquipWrapper.class, Item.class)
			.getter(from -> {
				if (!(from.event.getEntity() instanceof LivingEntity entity)) return null;
				return new Item(entity.getEquipment(from.event.getSlot()));
			})
			.time(EventValue.Time.PAST)
			.build());
		EventValues.registerEventValue(EventValue.simple(EntityEquipWrapper.class, EquipmentSlot.class, from -> from.event.getSlot()));
	}

	public EntityEquipWrapper(EntityEquipEvent event) {
		super(event);
	}

}
