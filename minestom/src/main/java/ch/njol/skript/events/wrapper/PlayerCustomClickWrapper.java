package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import net.minestom.server.event.player.PlayerCustomClickEvent;

public class PlayerCustomClickWrapper extends EventWrapper<PlayerCustomClickEvent> implements PlayerInstanceEventMarker {

	public PlayerCustomClickWrapper(PlayerCustomClickEvent event) {
		super(event);
	}

}
