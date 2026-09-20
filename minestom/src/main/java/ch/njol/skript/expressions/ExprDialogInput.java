package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.events.DialogClickEvent;
import ch.njol.skript.events.wrapper.PlayerCustomClickWrapper;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.kyori.adventure.nbt.*;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Dialog Input")
@Description("""
	The value a player entered into a named dialog input, available inside a dialog button's
	code section and inside the dialog click event.
	Text inputs return text, checkboxes return their true or false value as text, sliders return
	a number, and dropdowns return the id of the chosen option.
	Returns nothing if the dialog had no input under that name.""")
@Examples("""
	set {_buy} to new dialog button labeled "Buy" running code:
		send "You asked for %dialog input ""qty""%" to player""")
@Keywords({"dialog", "input"})
public class ExprDialogInput extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprDialogInput.class, Object.class, ExpressionType.SIMPLE,
			"[the] [dialog] input %string%");
	}

	private Expression<String> key;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected Object @Nullable [] get(Event event) {
		CompoundBinaryTag payload = payloadOf(event);
		if (payload == null) return null;
		String inputKey = key.getSingle(event);
		if (inputKey == null) return null;
		BinaryTag tag = payload.get(inputKey);
		if (tag == null) return null;
		Object value = switch (tag) {
			case StringBinaryTag string -> string.value();
			case ByteBinaryTag b -> b.value() != 0;
			case IntBinaryTag i -> i.value();
			case FloatBinaryTag f -> (double) f.value();
			case DoubleBinaryTag d -> d.value();
			case LongBinaryTag l -> l.value();
			case ShortBinaryTag s -> (int) s.value();
			default -> tag.toString();
		};
		return new Object[]{value};
	}

	private static @Nullable CompoundBinaryTag payloadOf(Event event) {
		if (event instanceof DialogClickEvent clickEvent) return clickEvent.getPayload();
		if (event instanceof PlayerCustomClickWrapper wrapper
			&& wrapper.getEvent().getPayload() instanceof CompoundBinaryTag compound) return compound;
		return null;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "dialog input " + key.toString(event, debug);
	}

}
