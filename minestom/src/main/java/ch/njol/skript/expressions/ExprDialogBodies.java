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
import net.minestom.server.dialog.DialogBody;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Dialog Bodies")
@Description("The body elements of a dialog. Supports set, add, remove, and clear.")
@Examples("""
	add (new plain message body mm("<gray>Sold out")) to bodies of {_d}
	clear bodies of {_d}""")
@Keywords({"dialog", "body"})
public class ExprDialogBodies extends SimpleExpression<DialogBody> {

	static {
		Skript.registerExpression(ExprDialogBodies.class, DialogBody.class, ExpressionType.PROPERTY,
			"[the] [dialog] bod(y|ies) of %dialogs%", "%dialogs%'[s] [dialog] bod(y|ies)");
	}

	private Expression<DialogWrapper> dialogs;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		dialogs = (Expression<DialogWrapper>) expressions[0];
		return true;
	}

	@Override
	protected DialogBody @Nullable [] get(Event event) {
		List<DialogBody> all = new ArrayList<>();
		for (DialogWrapper dialog : dialogs.getArray(event)) all.addAll(dialog.getBodies());
		return all.toArray(new DialogBody[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, ADD, REMOVE, DELETE, RESET -> CollectionUtils.array(DialogBody[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		for (DialogWrapper dialog : dialogs.getArray(event)) {
			List<DialogBody> bodies = dialog.getBodies();
			switch (mode) {
				case DELETE, RESET -> bodies.clear();
				case SET -> {
					bodies.clear();
					addAll(bodies, delta);
				}
				case ADD -> addAll(bodies, delta);
				case REMOVE -> {
					if (delta == null) break;
					for (Object o : delta) bodies.remove(o);
				}
				default -> {}
			}
		}
	}

	private static void addAll(List<DialogBody> bodies, @Nullable Object[] delta) {
		if (delta == null) return;
		for (Object o : delta) {
			if (o instanceof DialogBody body) bodies.add(body);
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends DialogBody> getReturnType() {
		return DialogBody.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "bodies of " + dialogs.toString(event, debug);
	}

}
