package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.BlockEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import net.minestom.server.event.player.PlayerFinishDiggingEvent;

public class PlayerFinishDiggingWrapper extends EventWrapper<PlayerFinishDiggingEvent> implements PlayerInstanceEventMarker, BlockEventMarker {

	public PlayerFinishDiggingWrapper(PlayerFinishDiggingEvent event) {
		super(event);
	}

}
