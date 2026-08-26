package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import ch.njol.skript.util.Timespan;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.PlayerFinishItemUseEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerFinishItemUseWrapper extends EventWrapper<PlayerFinishItemUseEvent> implements PlayerInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerFinishItemUseWrapper.class, Slot.class, from -> {
			PlayerFinishItemUseEvent event = from.event;
			return new Slot(event.getItemStack(), event.getPlayer(), event.getHand());
		}));
		EventValues.registerEventValue(EventValue.simple(PlayerFinishItemUseWrapper.class, PlayerHand.class, from -> from.event.getHand()));
		EventValues.registerEventValue(EventValue.builder(PlayerFinishItemUseWrapper.class, Timespan.class)
			.patterns("use-duration")
			.getter(from -> NumberUtils.timespanFrom(from.event.getUseDuration()))
			.build());
		EventValues.registerEventValue(EventValue.builder(PlayerFinishItemUseWrapper.class, Boolean.class)
			.patterns("riptide-spin")
			.getter(from -> from.event.isRiptideSpinAttack())
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setRiptideSpinAttack(value))
			.build());
	}

	public PlayerFinishItemUseWrapper(PlayerFinishItemUseEvent event) {
		super(event);
	}

}
