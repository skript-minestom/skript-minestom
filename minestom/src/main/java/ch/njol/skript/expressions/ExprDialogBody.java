package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Item;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogBody;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Dialog Body")
@Description("""
	Creates a body element for a dialog: either a block of text or an item with an optional description.
	Width defaults to 200 and item size defaults to 16 by 16.""")
@Examples("""
	set {_text} to new plain message body mm("<gray>Choose an item")
	set {_icon} to new item body of diamond sword with description mm("<gold>Legendary")""")
@Keywords({"dialog", "body"})
public class ExprDialogBody extends SimpleExpression<DialogBody> {

	private static final int DEFAULT_WIDTH = 200;
	private static final int DEFAULT_ITEM_SIZE = 16;

	static {
		Skript.registerExpression(ExprDialogBody.class, DialogBody.class, ExpressionType.COMBINED,
			"[new] plain [message] body %component% [with width %-number%]",
			"[new] item body of %item% [with description %-component%] [decoration:without decoration] "
				+ "[tooltip:without tooltip] [with width %-number%] [with height %-number%]");
	}

	private boolean item;
	private Expression<ComponentWrapper> text;
	private Expression<Item> itemStack;
	private @Nullable Expression<ComponentWrapper> description;
	private @Nullable Expression<Number> width;
	private @Nullable Expression<Number> height;
	private boolean showDecoration;
	private boolean showTooltip;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		item = matchedPattern == 1;
		if (item) {
			itemStack = (Expression<Item>) expressions[0];
			description = (Expression<ComponentWrapper>) expressions[1];
			width = (Expression<Number>) expressions[2];
			height = (Expression<Number>) expressions[3];
			showDecoration = !parseResult.hasTag("decoration");
			showTooltip = !parseResult.hasTag("tooltip");
		} else {
			text = (Expression<ComponentWrapper>) expressions[0];
			width = (Expression<Number>) expressions[1];
		}
		return true;
	}

	@Override
	protected DialogBody @Nullable [] get(Event event) {
		if (!item) {
			int width = intOr(this.width, event, DEFAULT_WIDTH);
			Component contents = ComponentWrapper.getOrElse(text, event, Component.empty());
			return new DialogBody[]{new DialogBody.PlainMessage(contents, width)};
		}
		Item single = itemStack.getSingle(event);
		if (single == null) return null;
		DialogBody.PlainMessage itemDescription = null;
		if (this.description != null) {
			ComponentWrapper wrapper = this.description.getSingle(event);
			if (wrapper != null) itemDescription = new DialogBody.PlainMessage(wrapper.getComponent(), DEFAULT_WIDTH);
		}
		return new DialogBody[]{new DialogBody.Item(single.getItem(), itemDescription, showDecoration, showTooltip,
			intOr(this.width, event, DEFAULT_ITEM_SIZE), intOr(height, event, DEFAULT_ITEM_SIZE))};
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
	public Class<? extends DialogBody> getReturnType() {
		return DialogBody.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return item ? "item body of " + itemStack.toString(event, debug) : "plain message body " + text.toString(event, debug);
	}

}
