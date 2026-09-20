package ch.njol.skript.events.wrapper;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.events.wrapper.marker.ItemEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import ch.njol.skript.util.Timespan;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerCancelItemUseWrapper extends EventWrapper<PlayerCancelItemUseEvent> implements PlayerInstanceEventMarker, ItemEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerCancelItemUseWrapper.class, Slot.class, from -> {
			PlayerCancelItemUseEvent event = from.event;
			return new Slot(event.getItemStack(), event.getPlayer(), event.getHand());
		}));
		EventValues.registerEventValue(EventValue.simple(PlayerCancelItemUseWrapper.class, PlayerHand.class, from -> from.event.getHand()));
		EventValues.registerEventValue(EventValue.builder(PlayerCancelItemUseWrapper.class, Timespan.class)
			.patterns("use-duration")
			.getter(from -> NumberUtils.timespanFrom(from.event.getUseDuration()))
			.build());
		EventValues.registerEventValue(EventValue.builder(PlayerCancelItemUseWrapper.class, Boolean.class)
			.patterns("riptide-spin")
			.getter(from -> from.event.isRiptideSpinAttack())
			.registerChanger(Changer.ChangeMode.SET, (event, value) -> event.event.setRiptideSpinAttack(value))
			.build());
	}

	public PlayerCancelItemUseWrapper(PlayerCancelItemUseEvent event) {
		super(event);
	}

}
