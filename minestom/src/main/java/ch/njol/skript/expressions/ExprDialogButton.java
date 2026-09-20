package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.NBTCompound;
import ch.njol.skript.util.dialog.ButtonWrapper;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.dialog.DialogAction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Dialog Button")
@Description("""
	Creates a button for a dialog with a fixed action.
	A button with no action just closes the dialog according to its after action setting.
	Add 'running code' with a section instead when the button should run script code.
	A custom action key must be in 'namespace:value' format and is what 'on dialog click' matches on.
	A dynamic custom action key is the same, but the client sends the dialog's input values back
	with the click, so 'dialog input' works in an 'on dialog click' handler for it; 'with additions'
	merges extra fixed NBT into that payload.""")
@Examples("""
	set {_buy} to new dialog button labeled "<green>Buy" with tooltip "Costs 10 coins" running command "buy"
	set {_wiki} to new dialog button labeled "Wiki" opening url "https://example.com"
	set {_next} to new dialog button labeled "More" showing dialog {_other}
	set {_confirm} to new dialog button labeled "Confirm" with dynamic custom action "myserver:confirm\"""")
@Keywords({"dialog", "button"})
public class ExprDialogButton extends SimpleExpression<ButtonWrapper> {

	static {
		Skript.registerExpression(ExprDialogButton.class, ButtonWrapper.class, ExpressionType.COMBINED,
			"[new] dialog button labeled %component% [with tooltip %-component%] [with width %-number%] "
				+ "[(command:running command %-string%|url:opening url %-string%|suggest:suggesting %-string%"
				+ "|copy:copying %-string%|show:showing dialog %-dialog%|page:changing page %-number%"
				+ "|custom:with custom action %-string% [with payload %-nbtcompound%]"
				+ "|dynamic:with dynamic custom action %-string% [with additions %-nbtcompound%])]");
	}

	private Expression<ComponentWrapper> label;
	private @Nullable Expression<ComponentWrapper> tooltip;
	private @Nullable Expression<Number> width;
	private @Nullable Expression<String> command;
	private @Nullable Expression<String> url;
	private @Nullable Expression<String> suggest;
	private @Nullable Expression<String> copy;
	private @Nullable Expression<DialogWrapper> show;
	private @Nullable Expression<Number> page;
	private @Nullable Expression<String> custom;
	private @Nullable Expression<NBTCompound> payload;
	private @Nullable Expression<String> dynamicCustom;
	private @Nullable Expression<NBTCompound> additions;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		label = (Expression<ComponentWrapper>) expressions[0];
		tooltip = (Expression<ComponentWrapper>) expressions[1];
		width = (Expression<Number>) expressions[2];
		command = (Expression<String>) expressions[3];
		url = (Expression<String>) expressions[4];
		suggest = (Expression<String>) expressions[5];
		copy = (Expression<String>) expressions[6];
		show = (Expression<DialogWrapper>) expressions[7];
		page = (Expression<Number>) expressions[8];
		custom = (Expression<String>) expressions[9];
		payload = (Expression<NBTCompound>) expressions[10];
		dynamicCustom = (Expression<String>) expressions[11];
		additions = (Expression<NBTCompound>) expressions[12];
		if (custom instanceof Literal<String> literal) {
			String key = literal.getSingle();
			if (key != null && !Key.parseable(key)) {
				Skript.error("'" + key + "' is not a valid custom action key. Format is 'namespace:value'.");
				return false;
			}
		}
		if (dynamicCustom instanceof Literal<String> literal) {
			String key = literal.getSingle();
			if (key != null && !Key.parseable(key)) {
				Skript.error("'" + key + "' is not a valid dynamic custom action key. Format is 'namespace:value'.");
				return false;
			}
		}
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
		button.setAction(buildAction(event));
		return new ButtonWrapper[]{button};
	}

	private @Nullable DialogAction buildAction(Event event) {
		if (command != null) {
			String value = command.getSingle(event);
			return value == null ? null : new DialogAction.RunCommand(value);
		}
		if (url != null) {
			String value = url.getSingle(event);
			return value == null ? null : new DialogAction.OpenUrl(value);
		}
		if (suggest != null) {
			String value = suggest.getSingle(event);
			return value == null ? null : new DialogAction.SuggestCommand(value);
		}
		if (copy != null) {
			String value = copy.getSingle(event);
			return value == null ? null : new DialogAction.CopyToClipboard(value);
		}
		if (show != null) {
			DialogWrapper value = show.getSingle(event);
			return value == null ? null : new DialogAction.ShowDialog(value.toHolder());
		}
		if (page != null) {
			Number value = page.getSingle(event);
			return value == null ? null : new DialogAction.ChangePage(value.intValue());
		}
		if (custom != null) {
			String value = custom.getSingle(event);
			if (value == null) return null;
			if (!Key.parseable(value)) {
				SkriptLogger.LOGGER.error("Couldn't use '{}' as a custom dialog button action because it is not a valid key. Format is 'namespace:value'.", value);
				return null;
			}
			BinaryTag tag = null;
			if (payload != null) {
				NBTCompound compound = payload.getSingle(event);
				if (compound != null) tag = compound.getCompound();
			}
			return new DialogAction.Custom(Key.key(value), tag);
		}
		if (dynamicCustom != null) {
			String value = dynamicCustom.getSingle(event);
			if (value == null) return null;
			if (!Key.parseable(value)) {
				SkriptLogger.LOGGER.error("Couldn't use '{}' as a dynamic custom dialog button action because it is not a valid key. Format is 'namespace:value'.", value);
				return null;
			}
			CompoundBinaryTag tag = null;
			if (additions != null) {
				NBTCompound compound = additions.getSingle(event);
				if (compound != null) tag = compound.getCompound();
			}
			return new DialogAction.DynamicCustom(Key.key(value), tag);
		}
		return null;
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
		return "dialog button labeled " + label.toString(event, debug);
	}

}
