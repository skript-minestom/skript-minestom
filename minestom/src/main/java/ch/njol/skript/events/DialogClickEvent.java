package ch.njol.skript.events;

import ch.njol.skript.registrations.EventValues;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class DialogClickEvent extends Event {

	private static final HandlerList HANDLERS = new HandlerList();

	static {
		EventValues.registerEventValue(EventValue.simple(DialogClickEvent.class, Player.class, DialogClickEvent::getPlayer));
	}

	private final Player player;
	private final @Nullable CompoundBinaryTag payload;

	public DialogClickEvent(Player player, @Nullable CompoundBinaryTag payload) {
		this.player = player;
		this.payload = payload;
	}

	public Player getPlayer() {
		return player;
	}

	public @Nullable CompoundBinaryTag getPayload() {
		return payload;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
