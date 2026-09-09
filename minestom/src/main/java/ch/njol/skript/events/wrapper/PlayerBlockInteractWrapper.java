package ch.njol.skript.events.wrapper;

import ch.njol.skript.events.wrapper.marker.BlockEventMarker;
import ch.njol.skript.events.wrapper.marker.PlayerInstanceEventMarker;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Slot;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PlayerBlockInteractWrapper extends EventWrapper<PlayerBlockInteractEvent> implements PlayerInstanceEventMarker, BlockEventMarker {

	static {
		EventValues.registerEventValue(EventValue.simple(PlayerBlockInteractWrapper.class, Slot.class, from -> {
			PlayerBlockInteractEvent e = from.event;
			Player player = e.getPlayer();
			PlayerHand hand = e.getHand();
			return new Slot(player.getItemInHand(hand), player, hand == PlayerHand.MAIN ? EquipmentSlot.MAIN_HAND : EquipmentSlot.OFF_HAND);
		}));
		EventValues.registerEventValue(EventValue.simple(PlayerBlockInteractWrapper.class, PlayerHand.class,
			from -> from.event.getHand()));
		EventValues.registerEventValue(EventValue.builder(PlayerBlockInteractWrapper.class, Point.class)
			.patterns("cursor-position")
			.getter(from -> from.event.getCursorPosition())
			.build());
	}

	public PlayerBlockInteractWrapper(PlayerBlockInteractEvent event) {
		super(event);
	}

}
