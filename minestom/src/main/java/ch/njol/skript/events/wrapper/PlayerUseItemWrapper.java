package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import net.minestom.server.event.player.PlayerUseItemEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerUseItemWrapper extends EventWrapper<PlayerUseItemEvent> implements PlayerInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerUseItemWrapper.class, Slot.class, from -> {
			PlayerUseItemEvent event = from.event;
			return new Slot(event.getItemStack(), event.getPlayer(), event.getHand());
		}));
	}

	public PlayerUseItemWrapper(PlayerUseItemEvent event) {
		super(event);
	}

}
