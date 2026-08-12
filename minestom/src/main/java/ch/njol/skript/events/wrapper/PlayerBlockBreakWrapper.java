package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.BlockEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.instance.block.Block;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerBlockBreakWrapper extends EventWrapper<PlayerBlockBreakEvent> implements PlayerInstanceEventMarker, BlockEventMarker {

	static {
		EventValues.registerEventValue(EventValue.builder(PlayerBlockBreakWrapper.class, Block.class)
			.patterns("result-block")
			.getter(from -> from.event.getResultBlock())
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setResultBlock(value))
			.build());
	}

	public PlayerBlockBreakWrapper(PlayerBlockBreakEvent event) {
		super(event);
	}

}
