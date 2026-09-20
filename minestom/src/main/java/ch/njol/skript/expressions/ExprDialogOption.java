package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Dialog Option")
@Description("""
	Creates one entry for a single option input.
	The id is what comes back in the click payload; the label is what the player sees and
	defaults to the id. Marking an option as selected makes it the initial choice.""")
@Examples("""
	set {_easy} to new dialog option "easy" labeled mm("<green>Easy") selected
	set {_hard} to new dialog option "hard" labeled mm("<red>Hard")""")
@Keywords({"dialog", "option"})
public class ExprDialogOption extends SimpleExpression<DialogInput.SingleOption.Option> {

	static {
		Skript.registerExpression(ExprDialogOption.class, DialogInput.SingleOption.Option.class, ExpressionType.COMBINED,
			"[new] [dialog] option %string% [labeled %-component%] [selected:selected]");
	}

	private Expression<String> id;
	private @Nullable Expression<ComponentWrapper> label;
	private boolean selected;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		id = (Expression<String>) expressions[0];
		label = (Expression<ComponentWrapper>) expressions[1];
		selected = parseResult.hasTag("selected");
		return true;
	}

	@Override
	protected DialogInput.SingleOption.Option @Nullable [] get(Event event) {
		String id = this.id.getSingle(event);
		if (id == null) return null;
		Component label = ComponentWrapper.getOrElse(this.label, event, Component.text(id));
		return new DialogInput.SingleOption.Option[]{new DialogInput.SingleOption.Option(id, label, selected)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends DialogInput.SingleOption.Option> getReturnType() {
		return DialogInput.SingleOption.Option.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "dialog option " + id.toString(event, debug);
	}

}
