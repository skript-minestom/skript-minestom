package ch.njol.skript.events;

import ch.njol.skript.registrations.EventValues;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class EffectCommandEvent extends Event implements CancellableEvent {

	private static final HandlerList HANDLERS = new HandlerList();

	static {
		EventValues.registerEventValue(EventValue.simple(EffectCommandEvent.class, CommandSender.class, EffectCommandEvent::getExecutor));
		EventValues.registerEventValue(EventValue.simple(EffectCommandEvent.class, Instance.class, from -> from.executor instanceof Player player ? player.getInstance() : null));
		EventValues.registerEventValue(EventValue.builder(EffectCommandEvent.class, String.class)
			.patterns("command")
			.getter(EffectCommandEvent::getCommand)
			.build());
	}

	private final CommandSender executor;
	private final String command;
	private boolean cancelled = false;

	public EffectCommandEvent(CommandSender executor, String command) {
		this.executor = executor;
		this.command = command;
	}

	public CommandSender getExecutor() {
		return executor;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

}
