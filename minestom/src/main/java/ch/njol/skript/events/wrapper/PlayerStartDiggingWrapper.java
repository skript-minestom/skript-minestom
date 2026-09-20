package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.BlockEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.BlockFace;
import ch.njol.skript.util.Direction;
import net.minestom.server.event.player.PlayerStartDiggingEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerStartDiggingWrapper extends EventWrapper<PlayerStartDiggingEvent> implements PlayerInstanceEventMarker, BlockEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerStartDiggingWrapper.class, Direction.class, from -> {
			BlockFace face = BlockFace.from(from.event.getBlockFace());
			if (face == null) return null;
			return new Direction(face, 1);
		}));
	}

	public PlayerStartDiggingWrapper(PlayerStartDiggingEvent event) {
		super(event);
	}

}
