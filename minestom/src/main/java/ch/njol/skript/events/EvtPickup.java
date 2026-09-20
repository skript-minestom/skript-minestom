package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.PickupItemWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Item;
import net.minestom.server.entity.Player;
import net.minestom.server.event.item.PickupItemEvent;
import net.minestom.server.item.Material;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtPickup extends SkriptEvent {

	static {
		Skript.registerEvent("Item Pickup", EvtPickup.class, PickupItemWrapper.class,
				"[(player|1:entity)] [item] (pick[ ]up|picking up) [[of] %-item%]")
			.description("""
				Called when a living entity walks over a dropped item.
				Note that entities cannot pick items up unless their item pickup state is enabled, \
				so this event never runs on its own. Note also that Minestom does not put the item \
				into the entity's inventory; it only removes the dropped item and plays the pickup \
				animation, so the script decides what the entity actually receives.
				Cancelling the event leaves the dropped item on the ground.""")
			.examples("""
				on item pickup:
					give event-item to event-entity

				on player pick up of diamond:
					broadcast "%event-entity% found a diamond!"

				on entity pickup:
					cancel event""");
	}

	private @Nullable Literal<Item> item;
	private boolean anyEntity;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		item = (Literal<Item>) args[0];
		anyEntity = parseResult.mark == 1;
		return true;
	}

	// todo maybe support item name/lore later, same as the item drop event
	@Override
	public boolean check(Event event) {
		if (!(event instanceof PickupItemWrapper wrapper)) return false;
		PickupItemEvent pickupEvent = wrapper.getEvent();
		if (!anyEntity && !(pickupEvent.getLivingEntity() instanceof Player)) return false;
		if (item == null) return true;
		Material picked = pickupEvent.getItemStack().material();
		return item.check(event, i -> i.getItem().material().equals(picked));
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String toString = (anyEntity ? "entity" : "player") + " item pickup";
		if (item != null) toString += " of " + item.toString(event, debug);
		return toString;
	}

}
