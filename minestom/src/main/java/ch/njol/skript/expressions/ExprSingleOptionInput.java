package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.DialogInputValidation;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("New Single Option Input")
@Description("""
	Creates a dropdown for a dialog. Options come from the 'dialog option' expression.
	If no option is marked as selected, the client shows the first one.""")
@Examples("""
	set {_mode} to new single option input named "mode" with options {_easy}, {_hard} labeled "Difficulty\"""")
@Keywords({"dialog", "input", "option", "dropdown"})
public class ExprSingleOptionInput extends SimpleExpression<DialogInput> {

	static {
		Skript.registerExpression(ExprSingleOptionInput.class, DialogInput.class, ExpressionType.COMBINED,
			"[new] single option input [named] %string% with options %dialogoptions% [labeled %-component%] [with width %-number%]");
	}

	private Expression<String> key;
	private Expression<DialogInput.SingleOption.Option> options;
	private @Nullable Expression<ComponentWrapper> label;
	private @Nullable Expression<Number> width;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		options = (Expression<DialogInput.SingleOption.Option>) expressions[1];
		label = (Expression<ComponentWrapper>) expressions[2];
		width = (Expression<Number>) expressions[3];
		return DialogInputValidation.checkLiteralKey(key, "single option input");
	}

	@Override
	protected DialogInput @Nullable [] get(Event event) {
		String key = this.key.getSingle(event);
		if (key == null) return null;
		if (!DialogInputValidation.checkRuntimeKey(key, "single option input")) return null;
		List<DialogInput.SingleOption.Option> options = List.of(this.options.getArray(event));
		if (options.isEmpty()) return null;
		options = dropExtraInitial(key, options);
		Component label = ComponentWrapper.getOrElse(this.label, event, Component.text(key));
		int width = DialogInput.DEFAULT_WIDTH;
		if (this.width != null) {
			Number single = this.width.getSingle(event);
			if (single != null) width = single.intValue();
		}
		return new DialogInput[]{new DialogInput.SingleOption(key, width, options, label, true)};
	}

	private static List<DialogInput.SingleOption.Option> dropExtraInitial(String key, List<DialogInput.SingleOption.Option> options) {
		boolean seenInitial = false;
		List<DialogInput.SingleOption.Option> corrected = null;
		for (int i = 0; i < options.size(); i++) {
			DialogInput.SingleOption.Option option = options.get(i);
			if (!option.initial()) continue;
			if (!seenInitial) {
				seenInitial = true;
				continue;
			}
			if (corrected == null) corrected = new ArrayList<>(options);
			corrected.set(i, new DialogInput.SingleOption.Option(option.id(), option.display(), false));
		}
		if (corrected == null) return options;
		SkriptLogger.LOGGER.warn("Single option input '{}' had more than one selected option; only the first is kept.", key);
		return corrected;
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
		return "single option input " + key.toString(event, debug);
	}

}
