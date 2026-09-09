package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import ch.njol.skript.util.Timespan;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerUseItemEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

import java.time.temporal.ChronoUnit;

public class PlayerUseItemWrapper extends EventWrapper<PlayerUseItemEvent> implements PlayerInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerUseItemWrapper.class, Slot.class, from -> {
			PlayerUseItemEvent event = from.event;
			return new Slot(event.getItemStack(), event.getPlayer(), event.getHand());
		}));
		EventValues.registerEventValue(EventValue.simple(PlayerUseItemWrapper.class, PlayerHand.class,
			from -> from.event.getHand()));
		EventValues.registerEventValue(EventValue.builder(PlayerUseItemWrapper.class, Timespan.class)
			.patterns("use-time")
			.getter(from -> NumberUtils.timespanFrom(from.event.getItemUseTime()/ ServerFlag.SERVER_TICKS_PER_SECOND))
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setItemUseTime(value.get(ChronoUnit.MILLIS)))
			.build());
	}

	public PlayerUseItemWrapper(PlayerUseItemEvent event) {
		super(event);
	}

}
