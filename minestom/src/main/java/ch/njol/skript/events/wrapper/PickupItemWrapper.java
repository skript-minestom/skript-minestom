package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.EntityInstanceEventMarker;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import net.minestom.server.event.item.PickupItemEvent;

public class PickupItemWrapper extends EventWrapper<PickupItemEvent> implements EntityInstanceEventMarker, ItemEventMarker {

	public PickupItemWrapper(PickupItemEvent event) {
		super(event);
	}

}
