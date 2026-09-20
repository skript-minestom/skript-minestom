package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.PlayerCustomClickWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.NBTCompound;
import ch.njol.skript.util.dialog.DialogCallbacks;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class EvtDialogClick extends SkriptEvent {

	static {
		Skript.registerEvent("Dialog Click", EvtDialogClick.class, PlayerCustomClickWrapper.class,
				"dialog (click|action) [%-string%]")
			.description("""
				Called when a player clicks a dialog button that carries a custom action, and for any
				other custom click packet the client sends.
				Buttons created with 'new dialog button ... running code' are handled by their own code
				instead and never reach this event.
				The input values of the dialog are available through the 'dialog input' expression when
				the button used a dynamic custom action.""")
			.examples("""
				on dialog click "myserver:buy":
					send "You clicked buy!" to player

				on dialog click:
					send "raw action: %event-string%" to player""");
		EventValues.registerEventValue(EventValue.simple(PlayerCustomClickWrapper.class, String.class,
			wrapper -> wrapper.getEvent().getKey().asString()));
		EventValues.registerEventValue(EventValue.simple(PlayerCustomClickWrapper.class, NBTCompound.class, wrapper -> {
			BinaryTag payload = wrapper.getEvent().getPayload();
			if (!(payload instanceof CompoundBinaryTag compound)) return null;
			return new NBTCompound(compound, false);
		}));
	}

	private @Nullable Literal<String> key;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		key = (Literal<String>) args[0];
		if (key != null) {
			String value = key.getSingle();
			if (value != null && !Key.parseable(value)) {
				Skript.error("'" + value + "' is not a valid action key. Format is 'namespace:value'.");
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean check(Event event) {
		if (!(event instanceof PlayerCustomClickWrapper wrapper)) return false;
		PlayerCustomClickEvent clickEvent = wrapper.getEvent();
		if (DialogCallbacks.isCallbackKey(clickEvent.getKey())) return false;
		if (key == null) return true;
		String actual = clickEvent.getKey().asString();
		return key.check(event, expected -> expected.equals(actual));
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "dialog click" + (key == null ? "" : " " + key.toString(event, debug));
	}

}
