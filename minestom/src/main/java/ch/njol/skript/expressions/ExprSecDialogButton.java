package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.events.DialogClickEvent;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.ButtonWrapper;
import ch.njol.skript.util.dialog.DialogCallbacks;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

@Name("New Dialog Button Running Code")
@Description("""
	Creates a dialog button whose code runs when a player clicks it.
	The button sends the dialog's input values back with the click, so 'dialog input' works
	inside the section. The player who clicked is the event-player.
	The section runs on the server, so it can do anything a normal trigger can.
	Use 'new dialog button' without 'running code' for a button with a fixed client-side action.
	Warning: the button's callback key is live from parse time onward, and any player can send
	a click for it at any time with arbitrary input values, not just the player it was shown to.
	Always validate anything read from 'dialog input' before trusting it.""")
@Examples("""
	set {_buy} to new dialog button labeled "<green>Buy" running code:
		send "You bought %dialog input ""qty""% items!" to player""")
@Keywords({"dialog", "button"})
public class ExprSecDialogButton extends SectionExpression<ButtonWrapper> {

	static {
		Skript.registerExpression(ExprSecDialogButton.class, ButtonWrapper.class, ExpressionType.COMBINED,
			"[new] dialog button labeled %component% [with tooltip %-component%] [with width %-number%] running code");
	}

	private Expression<ComponentWrapper> label;
	private @Nullable Expression<ComponentWrapper> tooltip;
	private @Nullable Expression<Number> width;
	private Trigger trigger;
	private Key callbackKey;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		label = (Expression<ComponentWrapper>) expressions[0];
		tooltip = (Expression<ComponentWrapper>) expressions[1];
		width = (Expression<Number>) expressions[2];
		if (sectionNode == null || sectionNode.isEmpty()) {
			Skript.error("A dialog button running code needs code inside it.");
			return false;
		}
		callbackKey = DialogCallbacks.nextKey(getParser().getCurrentScript(), sectionNode);
		trigger = loadCode(sectionNode, "dialog button", (Runnable) null, (Runnable) null, DialogClickEvent.class);
		DialogCallbacks.register(callbackKey, trigger, getParser().getCurrentScript());
		return true;
	}

	@Override
	public boolean isSectionOnly() {
		return true;
	}

	@Override
	protected ButtonWrapper @Nullable [] get(Event event) {
		ComponentWrapper labelValue = label.getSingle(event);
		if (labelValue == null) return null;
		ButtonWrapper button = new ButtonWrapper(labelValue.getComponent());
		if (tooltip != null) {
			ComponentWrapper single = tooltip.getSingle(event);
			if (single != null) button.setTooltip(single.getComponent());
		}
		if (width != null) {
			Number single = width.getSingle(event);
			if (single != null) button.setWidth(single.intValue());
		}
		button.setTrigger(trigger, callbackKey);
		return new ButtonWrapper[]{button};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends ButtonWrapper> getReturnType() {
		return ButtonWrapper.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "dialog button labeled " + label.toString(event, debug) + " running code";
	}

}
