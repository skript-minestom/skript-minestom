package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.dialog.DialogInput;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Dialog Inputs")
@Description("The inputs of a dialog. Supports set, add, remove, and clear.")
@Examples("add {_qty} to inputs of {_d}")
@Keywords({"dialog", "input"})
public class ExprDialogInputs extends SimpleExpression<DialogInput> {

	static {
		Skript.registerExpression(ExprDialogInputs.class, DialogInput.class, ExpressionType.PROPERTY,
			"[the] [dialog] inputs of %dialogs%", "%dialogs%'[s] [dialog] inputs");
	}

	private Expression<DialogWrapper> dialogs;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		dialogs = (Expression<DialogWrapper>) expressions[0];
		return true;
	}

	@Override
	protected DialogInput @Nullable [] get(Event event) {
		List<DialogInput> all = new ArrayList<>();
		for (DialogWrapper dialog : dialogs.getArray(event)) all.addAll(dialog.getInputs());
		return all.toArray(new DialogInput[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(DialogInput[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		for (DialogWrapper dialog : dialogs.getArray(event)) {
			List<DialogInput> inputs = dialog.getInputs();
			switch (mode) {
				case DELETE, RESET -> inputs.clear();
				case SET -> {
					inputs.clear();
					addAll(inputs, delta);
				}
				case ADD -> addAll(inputs, delta);
				case REMOVE -> {
					if (delta == null) break;
					for (Object o : delta) inputs.remove(o);
				}
				default -> {}
			}
		}
	}

	private static void addAll(List<DialogInput> inputs, @Nullable Object[] delta) {
		if (delta == null) return;
		for (Object o : delta) {
			if (o instanceof DialogInput input) inputs.add(input);
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends DialogInput> getReturnType() {
		return DialogInput.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "inputs of " + dialogs.toString(event, debug);
	}

}
