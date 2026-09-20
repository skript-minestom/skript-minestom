package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import ch.njol.skript.util.Timespan;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.item.ItemAnimation;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerBeginItemUseWrapper extends EventWrapper<PlayerBeginItemUseEvent> implements PlayerInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerBeginItemUseWrapper.class, Slot.class, from -> {
			PlayerBeginItemUseEvent event = from.event;
			return new Slot(event.getItemStack(), event.getPlayer(), event.getHand());
		}));
		EventValues.registerEventValue(EventValue.simple(PlayerBeginItemUseWrapper.class, PlayerHand.class, from -> from.event.getHand()));
		EventValues.registerEventValue(EventValue.simple(PlayerBeginItemUseWrapper.class, ItemAnimation.class, from -> from.event.getAnimation()));
		EventValues.registerEventValue(EventValue.builder(PlayerBeginItemUseWrapper.class, Timespan.class)
			.patterns("use-duration")
			.getter(from -> NumberUtils.timespanFrom(from.event.getItemUseDuration()))
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setItemUseDuration(NumberUtils.ticksFrom(value)))
			.build());
	}

	public PlayerBeginItemUseWrapper(PlayerBeginItemUseEvent event) {
		super(event);
	}

}
