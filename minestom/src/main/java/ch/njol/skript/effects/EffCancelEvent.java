package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.events.wrapper.EventWrapper;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import net.minestom.server.event.trait.CancellableEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Peter Güttinger
 */
@Name("Cancel Event")
@Description("Cancels the event (e.g. prevent blocks from being placed, or damage being taken).")
@Examples("""
	on chat:
		cancel event""")
@Since("1.0")
public class EffCancelEvent extends Effect {
	static {
		Skript.registerEffect(EffCancelEvent.class, "cancel [the] event", "uncancel [the] event");
	}

	private boolean cancel;

	@SuppressWarnings("null")
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		if (isDelayed == Kleenean.TRUE) {
			Skript.error("Can't cancel an event anymore after it has already passed", ErrorQuality.SEMANTIC_ERROR);
			return false;
		}
		cancel = matchedPattern == 0;
		final Class<? extends Event>[] currentEvents = getParser().getCurrentEvents();
		if (currentEvents == null)
			return false;
		for (final Class<? extends Event> e : currentEvents) {
			if (isCancellable(e)) return true;
		}
		Skript.error(Utils.A(getParser().getCurrentEventName()) + " event cannot be cancelled", ErrorQuality.SEMANTIC_ERROR);
		return false;
	}

	@Override
	public void execute(final Event e) {
		if (e instanceof EventWrapper<?> wrapper) {
			if (wrapper.getEvent() instanceof CancellableEvent cancellable) cancellable.setCancelled(cancel);
		} else if (e instanceof CancellableEvent cancellable) {
			cancellable.setCancelled(cancel);
		}
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return (cancel ? "" : "un") + "cancel event";
	}

	public static boolean isCancellable(Class<? extends Event> event) {
		if (Cancellable.class.isAssignableFrom(event)) return true;
		if (CancellableEvent.class.isAssignableFrom(event)) return true;
		Class<? extends net.minestom.server.event.Event> minestomEventClass = getMinestomEventType(event);
		return minestomEventClass != null && CancellableEvent.class.isAssignableFrom(minestomEventClass);
	}

	// chatgpt
	@SuppressWarnings("unchecked")
	private static Class<? extends net.minestom.server.event.Event> getMinestomEventType(Class<? extends org.bukkit.event.Event> wrapperClass) {
		Class<?> c = wrapperClass;

		while (c != null && c != Object.class) {
			Type gs = c.getGenericSuperclass();

			if (gs instanceof ParameterizedType pt) {
				Type arg0 = pt.getActualTypeArguments()[0];
				if (arg0 instanceof Class<?> eventClass
					&& net.minestom.server.event.Event.class.isAssignableFrom(eventClass)) {
					return (Class<? extends net.minestom.server.event.Event>) eventClass;
				}
			}

			c = c.getSuperclass();
		}

		return null;
	}

}
