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

@Name("New Number Range Input")
@Description("""
	Creates a slider for a dialog. The label format is the vanilla format string used to render
	the current value, defaulting to "options.generic_value" which shows 'label: value'.
	Step defaults to none, which lets the slider move continuously.""")
@Examples("set {_qty} to new number range input named \"qty\" from 1 to 64 labeled \"Amount\" with initial value 1 with step 1")
@Keywords({"dialog", "input", "slider", "number"})
public class ExprNumberRangeInput extends SimpleExpression<DialogInput> {

	private static final String DEFAULT_LABEL_FORMAT = "options.generic_value";

	static {
		Skript.registerExpression(ExprNumberRangeInput.class, DialogInput.class, ExpressionType.COMBINED,
			"[new] number range input [named] %string% from %number% to %number% [labeled %-component%] "
				+ "[with initial [value] %-number%] [with step %-number%] [with label format %-string%] [with width %-number%]");
	}

	private Expression<String> key;
	private Expression<Number> start;
	private Expression<Number> end;
	private @Nullable Expression<ComponentWrapper> label;
	private @Nullable Expression<Number> initial;
	private @Nullable Expression<Number> step;
	private @Nullable Expression<String> labelFormat;
	private @Nullable Expression<Number> width;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		start = (Expression<Number>) expressions[1];
		end = (Expression<Number>) expressions[2];
		label = (Expression<ComponentWrapper>) expressions[3];
		initial = (Expression<Number>) expressions[4];
		step = (Expression<Number>) expressions[5];
		labelFormat = (Expression<String>) expressions[6];
		width = (Expression<Number>) expressions[7];
		return DialogInputValidation.checkLiteralKey(key, "number range input");
	}

	@Override
	protected DialogInput @Nullable [] get(Event event) {
		String key = this.key.getSingle(event);
		Number start = this.start.getSingle(event);
		Number end = this.end.getSingle(event);
		if (key == null || start == null || end == null) return null;
		if (!DialogInputValidation.checkRuntimeKey(key, "number range input")) return null;
		Component label = ComponentWrapper.getOrElse(this.label, event, Component.text(key));
		String labelFormat = DEFAULT_LABEL_FORMAT;
		if (this.labelFormat != null) {
			String single = this.labelFormat.getSingle(event);
			if (single != null) labelFormat = single;
		}
		int width = DialogInput.DEFAULT_WIDTH;
		if (this.width != null) {
			Number single = this.width.getSingle(event);
			if (single != null) width = single.intValue();
		}
		return new DialogInput[]{new DialogInput.NumberRange(key, width, label, labelFormat,
			start.floatValue(), end.floatValue(), floatOrNull(initial, event), floatOrNull(step, event))};
	}

	private static @Nullable Float floatOrNull(@Nullable Expression<Number> expr, Event event) {
		if (expr == null) return null;
		Number number = expr.getSingle(event);
		return number == null ? null : number.floatValue();
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
		return "number range input " + key.toString(event, debug);
	}

}
