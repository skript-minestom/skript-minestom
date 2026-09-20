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

@Name("New Text Input")
@Description("""
	Creates a text field for a dialog. The name is the key the value comes back under in
	'dialog input'. Width defaults to 200 and max length to 32.
	A multiline field with no max lines given grows freely.""")
@Examples("""
	set {_name} to new text input named "nick" labeled "Nickname" with max length 16
	set {_essay} to new text input named "reason" labeled "Reason" multiline with max lines 5""")
@Keywords({"dialog", "input", "text"})
public class ExprTextInput extends SimpleExpression<DialogInput> {

	private static final int DEFAULT_MAX_LENGTH = 32;

	static {
		Skript.registerExpression(ExprTextInput.class, DialogInput.class, ExpressionType.COMBINED,
			"[new] text input [named] %string% [labeled %-component%] [with initial [value] %-string%] "
				+ "[with max length %-number%] [with width %-number%] [multiline:(multiline|multi-line) [with max lines %-number%]]");
	}

	private Expression<String> key;
	private @Nullable Expression<ComponentWrapper> label;
	private @Nullable Expression<String> initial;
	private @Nullable Expression<Number> maxLength;
	private @Nullable Expression<Number> width;
	private @Nullable Expression<Number> maxLines;
	private boolean multiline;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		label = (Expression<ComponentWrapper>) expressions[1];
		initial = (Expression<String>) expressions[2];
		maxLength = (Expression<Number>) expressions[3];
		width = (Expression<Number>) expressions[4];
		maxLines = (Expression<Number>) expressions[5];
		multiline = parseResult.hasTag("multiline");
		return DialogInputValidation.checkLiteralKey(key, "text input");
	}

	@Override
	protected DialogInput @Nullable [] get(Event event) {
		String key = this.key.getSingle(event);
		if (key == null) return null;
		if (!DialogInputValidation.checkRuntimeKey(key, "text input")) return null;
		Component label = ComponentWrapper.getOrElse(this.label, event, Component.text(key));
		String initial = this.initial == null ? "" : this.initial.getSingle(event);
		if (initial == null) initial = "";
		DialogInput.Text.Multiline multilineConfig = null;
		if (this.multiline) {
			Integer maxLines = null;
			if (this.maxLines != null) {
				Number number = this.maxLines.getSingle(event);
				if (number != null) maxLines = number.intValue();
			}
			multilineConfig = new DialogInput.Text.Multiline(maxLines, null);
		}
		return new DialogInput[]{new DialogInput.Text(key, intOr(width, event, DialogInput.DEFAULT_WIDTH), label, true,
			initial, intOr(maxLength, event, DEFAULT_MAX_LENGTH), multilineConfig)};
	}

	private static int intOr(@Nullable Expression<Number> expr, Event event, int fallback) {
		if (expr == null) return fallback;
		Number number = expr.getSingle(event);
		return number == null ? fallback : number.intValue();
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
		return "text input " + key.toString(event, debug);
	}

}
