package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.EntityInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.entity.EntityVelocityEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class EntityVelocityWrapper extends EventWrapper<EntityVelocityEvent> implements EntityInstanceEventMarker {

	static {
		EventValues.registerEventValue(EventValue.builder(EntityVelocityWrapper.class, Vec.class)
			.getter(from -> from.event.getVelocity())
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setVelocity(value))
			.build());
	}

	public EntityVelocityWrapper(EntityVelocityEvent event) {
		super(event);
	}

}
