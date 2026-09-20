package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.DialogInputValidation;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Boolean Input")
@Description("""
	Creates a checkbox for a dialog. It starts unchecked unless 'initially true' is given,
	matching the vanilla default. The true and false values are the strings sent back in the
	click payload, defaulting to "true" and "false".""")
@Examples("set {_agree} to new boolean input named \"agree\" labeled \"I agree\" initially false")
@Keywords({"dialog", "input", "boolean", "checkbox"})
public class ExprBooleanInput extends SimpleExpression<DialogInput> {

	static {
		Skript.registerExpression(ExprBooleanInput.class, DialogInput.class, ExpressionType.COMBINED,
			"[new] boolean input [named] %string% [labeled %-component%] [initially %-boolean%] "
				+ "[with true value %-string%] [with false value %-string%]");
	}

	private Expression<String> key;
	private @Nullable Expression<ComponentWrapper> label;
	private @Nullable Expression<String> onTrue;
	private @Nullable Expression<String> onFalse;
	private @Nullable Expression<Boolean> initial;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		label = (Expression<ComponentWrapper>) expressions[1];
		initial = (Expression<Boolean>) expressions[2];
		onTrue = (Expression<String>) expressions[3];
		onFalse = (Expression<String>) expressions[4];
		return DialogInputValidation.checkLiteralKey(key, "boolean input");
	}

	@Override
	protected DialogInput @Nullable [] get(Event event) {
		String key = this.key.getSingle(event);
		if (key == null) return null;
		if (!DialogInputValidation.checkRuntimeKey(key, "boolean input")) return null;
		Component label = ComponentWrapper.getOrElse(this.label, event, Component.text(key));
		return new DialogInput[]{new DialogInput.Boolean(key, label, initial != null && Boolean.TRUE.equals(initial.getSingle(event)),
			stringOr(onTrue, event, "true"), stringOr(onFalse, event, "false"))};
	}

	private static String stringOr(@Nullable Expression<String> expr, Event event, String fallback) {
		if (expr == null) return fallback;
		String value = expr.getSingle(event);
		return value == null ? fallback : value;
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends DialogInput> getReturnType() {
		return DialogInput.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "boolean input " + key.toString(event, debug);
	}

}
