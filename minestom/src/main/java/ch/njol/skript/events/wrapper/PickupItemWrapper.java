package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.EntityInstanceEventMarker;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.registrations.EventValues;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.item.PickupItemEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PickupItemWrapper extends EventWrapper<PickupItemEvent> implements EntityInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.builder(PickupItemWrapper.class, Entity.class)
			.patterns("item-entity")
			.getter(from -> from.event.getItemEntity())
			.build());
	}

	public PickupItemWrapper(PickupItemEvent event) {
		super(event);
	}

}
