package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.events.wrapper.EventWrapper;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import net.minestom.server.event.trait.CancellableEvent;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Event Cancelled")
@Description("Checks whether or not the event is cancelled.")
@Examples("""
	on click:
		if event is cancelled:
			broadcast "no clicks allowed!\"""")
@Since("2.2-dev36")
public class CondCancelled extends Condition {

	static {
		Skript.registerCondition(CondCancelled.class,
			"[the] event is cancel[l]ed",
			"[the] event (is not|isn't) cancel[l]ed"
		);
	}

	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event e) {
		net.minestom.server.event.Event trueEvent = ((EventWrapper<?>) e).getEvent();
		if (!(trueEvent instanceof CancellableEvent cancellable)) return false;
		return cancellable.isCancelled() ^ isNegated();
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return isNegated() ? "event is not cancelled" : "event is cancelled";
	}

}
