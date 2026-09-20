package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.dialog.ButtonWrapper;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Dialog Buttons")
@Description("The action buttons of a multi action dialog. Supports set, add, remove, and clear. Does nothing for other dialog kinds, which have named buttons instead.")
@Examples("add {_buy} to buttons of {_d}")
@Keywords({"dialog", "button"})
public class ExprDialogButtons extends SimpleExpression<ButtonWrapper> {

	static {
		Skript.registerExpression(ExprDialogButtons.class, ButtonWrapper.class, ExpressionType.PROPERTY,
			"[the] [dialog] buttons of %dialogs%", "%dialogs%'[s] [dialog] buttons");
	}

	private Expression<DialogWrapper> dialogs;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		dialogs = (Expression<DialogWrapper>) expressions[0];
		return true;
	}

	@Override
	protected ButtonWrapper @Nullable [] get(Event event) {
		List<ButtonWrapper> all = new ArrayList<>();
		for (DialogWrapper dialog : dialogs.getArray(event)) all.addAll(dialog.getButtons());
		return all.toArray(new ButtonWrapper[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(ButtonWrapper[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		for (DialogWrapper dialog : dialogs.getArray(event)) {
			List<ButtonWrapper> buttons = dialog.getButtons();
			switch (mode) {
				case DELETE, RESET -> buttons.clear();
				case SET -> {
					buttons.clear();
					addAll(buttons, delta);
				}
				case ADD -> addAll(buttons, delta);
				case REMOVE -> {
					if (delta == null) break;
					for (Object o : delta) buttons.remove(o);
				}
				default -> { }
			}
		}
	}

	private static void addAll(List<ButtonWrapper> buttons, @Nullable Object[] delta) {
		if (delta == null) return;
		for (Object o : delta) {
			if (o instanceof ButtonWrapper button) buttons.add(button);
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends ButtonWrapper> getReturnType() {
		return ButtonWrapper.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "buttons of " + dialogs.toString(event, debug);
	}

}
