package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.ButtonWrapper;
import ch.njol.skript.util.dialog.DialogKind;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogBody;
import net.minestom.server.dialog.DialogInput;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Name("New Dialog")
@Description("""
	Builds a dialog. A dialog is a plain value, so it can be stored in a variable, returned from a
	function or shown directly; use 'register dialog' instead to put one in the server dialog
	registry under a key.

	Shared entries:
	title -> component (required)
	external title -> component
	can close with escape -> boolean (default true)
	pause -> boolean (default false)
	after action -> dialog after action (close, none, wait for response; default close)
	body -> dialog bodies
	inputs -> dialog inputs

	Notice: button -> dialog button
	Confirmation: yes button, no button -> dialog button
	Multi action: buttons -> dialog buttons, exit button -> dialog button, columns -> number
	Dialog list: dialogs -> dialogs, exit button -> dialog button, columns -> number, button width -> number
	Server links: exit button -> dialog button, columns -> number, button width -> number""")
@Examples("""
	set {_shop} to new multi action dialog:
		title: mm("<gold>Shop")
		body: {_greeting}
		inputs: {_qty}
		buttons: {_buy}, {_cancel}
		columns: 2
	show {_shop} to player

	show (new notice dialog:
		title: mm("<gold>Welcome")) to player""")
@Keywords({"dialog"})
public class ExprSecDialog extends SectionExpression<DialogWrapper> {

	private static final Map<DialogKind, EntryValidator> VALIDATORS = new EnumMap<>(DialogKind.class);

	static {
		for (DialogKind kind : DialogKind.values()) VALIDATORS.put(kind, buildValidator(kind));
		Skript.registerExpression(ExprSecDialog.class, DialogWrapper.class, ExpressionType.SIMPLE,
			"[a] new (notice:notice|confirmation:confirmation|multi:multi[ ]action|links:server links) dialog",
			"[a] new dialog list");
	}

	private static EntryValidator buildValidator(DialogKind kind) {
		EntryValidator.EntryValidatorBuilder builder = EntryValidator.builder();
		builder.addEntryData(new ExpressionEntryData<>("title", null, false, ComponentWrapper.class))
			.addEntryData(new ExpressionEntryData<>("external title", null, true, ComponentWrapper.class))
			.addEntryData(new ExpressionEntryData<>("can close with escape", new SimpleLiteral<>(true, true), true, Boolean.class))
			.addEntryData(new ExpressionEntryData<>("pause", new SimpleLiteral<>(false, true), true, Boolean.class))
			.addEntryData(new ExpressionEntryData<>("after action", new SimpleLiteral<>(DialogAfterAction.CLOSE, true), true, DialogAfterAction.class))
			.addEntryData(new ExpressionEntryData<>("body", null, true, DialogBody.class))
			.addEntryData(new ExpressionEntryData<>("inputs", null, true, DialogInput.class));
		switch (kind) {
			case NOTICE -> builder.addEntryData(new ExpressionEntryData<>("button", null, true, ButtonWrapper.class));
			case CONFIRMATION -> builder
				.addEntryData(new ExpressionEntryData<>("yes button", null, true, ButtonWrapper.class))
				.addEntryData(new ExpressionEntryData<>("no button", null, true, ButtonWrapper.class));
			case MULTI_ACTION -> builder
				.addEntryData(new ExpressionEntryData<>("buttons", null, true, ButtonWrapper.class))
				.addEntryData(new ExpressionEntryData<>("exit button", null, true, ButtonWrapper.class))
				.addEntryData(new ExpressionEntryData<>("columns", null, true, Number.class));
			case DIALOG_LIST -> builder
				.addEntryData(new ExpressionEntryData<>("dialogs", null, false, DialogWrapper.class))
				.addEntryData(new ExpressionEntryData<>("exit button", null, true, ButtonWrapper.class))
				.addEntryData(new ExpressionEntryData<>("columns", null, true, Number.class))
				.addEntryData(new ExpressionEntryData<>("button width", null, true, Number.class));
			case SERVER_LINKS -> builder
				.addEntryData(new ExpressionEntryData<>("exit button", null, true, ButtonWrapper.class))
				.addEntryData(new ExpressionEntryData<>("columns", null, true, Number.class))
				.addEntryData(new ExpressionEntryData<>("button width", null, true, Number.class));
		}
		return builder.build();
	}

	public static EntryValidator validatorFor(DialogKind kind) {
		return VALIDATORS.get(kind);
	}

	public record DialogEntries(
		Expression<ComponentWrapper> title,
		@Nullable Expression<ComponentWrapper> externalTitle,
		@Nullable Expression<Boolean> canCloseWithEscape,
		@Nullable Expression<Boolean> pause,
		@Nullable Expression<DialogAfterAction> afterAction,
		@Nullable Expression<DialogBody> body,
		@Nullable Expression<DialogInput> inputs,
		@Nullable Expression<ButtonWrapper> button,
		@Nullable Expression<ButtonWrapper> yesButton,
		@Nullable Expression<ButtonWrapper> noButton,
		@Nullable Expression<ButtonWrapper> buttons,
		@Nullable Expression<ButtonWrapper> exitButton,
		@Nullable Expression<Number> columns,
		@Nullable Expression<Number> buttonWidth,
		@Nullable Expression<DialogWrapper> dialogs
	) { }

	public static @Nullable DialogEntries resolve(DialogKind kind, EntryContainer container) {
		Expression<ComponentWrapper> title = optional(container, "title");
		if (title == null) {
			Skript.error("A dialog needs a 'title' entry.");
			return null;
		}
		Expression<ComponentWrapper> externalTitle = optional(container, "external title");
		Expression<Boolean> canCloseWithEscape = optional(container, "can close with escape");
		Expression<Boolean> pause = optional(container, "pause");
		Expression<DialogAfterAction> afterAction = optional(container, "after action");
		if (pause instanceof Literal<Boolean> pauseLiteral && afterAction instanceof Literal<DialogAfterAction> afterActionLiteral) {
			Boolean pauseValue = pauseLiteral.getSingle();
			DialogAfterAction afterActionValue = afterActionLiteral.getSingle();
			if (Boolean.TRUE.equals(pauseValue) && afterActionValue == DialogAfterAction.NONE) {
				Skript.error("A dialog cannot have 'pause: true' together with 'after action: none'.");
				return null;
			}
		}
		Expression<DialogBody> body = optional(container, "body");
		Expression<DialogInput> inputs = optional(container, "inputs");

		Expression<ButtonWrapper> button = null;
		Expression<ButtonWrapper> yesButton = null;
		Expression<ButtonWrapper> noButton = null;
		Expression<ButtonWrapper> buttons = null;
		Expression<ButtonWrapper> exitButton = null;
		Expression<Number> columns = null;
		Expression<Number> buttonWidth = null;
		Expression<DialogWrapper> dialogs = null;

		switch (kind) {
			case NOTICE -> button = optional(container, "button");
			case CONFIRMATION -> {
				yesButton = optional(container, "yes button");
				noButton = optional(container, "no button");
			}
			case MULTI_ACTION -> {
				buttons = optional(container, "buttons");
				exitButton = optional(container, "exit button");
				columns = optional(container, "columns");
			}
			case DIALOG_LIST -> {
				dialogs = optional(container, "dialogs");
				if (dialogs == null) {
					Skript.error("A dialog list needs a 'dialogs' entry.");
					return null;
				}
				exitButton = optional(container, "exit button");
				columns = optional(container, "columns");
				buttonWidth = optional(container, "button width");
			}
			case SERVER_LINKS -> {
				exitButton = optional(container, "exit button");
				columns = optional(container, "columns");
				buttonWidth = optional(container, "button width");
			}
		}

		return new DialogEntries(title, externalTitle, canCloseWithEscape, pause, afterAction, body, inputs,
			button, yesButton, noButton, buttons, exitButton, columns, buttonWidth, dialogs);
	}

	private static <T> @Nullable Expression<T> optional(EntryContainer container, String key) {
		return (Expression<T>) container.getOptional(key, Expression.class, false);
	}

	public static DialogWrapper build(DialogKind kind, DialogEntries entries, Event event) {
		DialogWrapper dialog = new DialogWrapper(kind);
		dialog.setTitle(ComponentWrapper.getOrElse(entries.title(), event, Component.empty()));
		if (entries.externalTitle() != null) {
			ComponentWrapper single = entries.externalTitle().getSingle(event);
			if (single != null) dialog.setExternalTitle(single.getComponent());
		}
		Boolean canClose = single(entries.canCloseWithEscape(), Boolean.class, event);
		if (canClose != null) dialog.setCanCloseWithEscape(canClose);
		Boolean pause = single(entries.pause(), Boolean.class, event);
		if (pause != null) dialog.setPause(pause);
		DialogAfterAction afterAction = single(entries.afterAction(), DialogAfterAction.class, event);
		if (afterAction != null) dialog.setAfterAction(afterAction);
		addAll(entries.body(), DialogBody.class, event, dialog.getBodies());
		addAll(entries.inputs(), DialogInput.class, event, dialog.getInputs());
		switch (kind) {
			case NOTICE -> dialog.setButton(single(entries.button(), ButtonWrapper.class, event));
			case CONFIRMATION -> {
				dialog.setYesButton(single(entries.yesButton(), ButtonWrapper.class, event));
				dialog.setNoButton(single(entries.noButton(), ButtonWrapper.class, event));
			}
			case MULTI_ACTION -> {
				addAll(entries.buttons(), ButtonWrapper.class, event, dialog.getButtons());
				dialog.setExitButton(single(entries.exitButton(), ButtonWrapper.class, event));
				Number columns = single(entries.columns(), Number.class, event);
				if (columns != null) dialog.setColumns(columns.intValue());
			}
			case DIALOG_LIST -> {
				addAll(entries.dialogs(), DialogWrapper.class, event, dialog.getChildren());
				dialog.setExitButton(single(entries.exitButton(), ButtonWrapper.class, event));
				Number columns = single(entries.columns(), Number.class, event);
				if (columns != null) dialog.setColumns(columns.intValue());
				Number buttonWidth = single(entries.buttonWidth(), Number.class, event);
				if (buttonWidth != null) dialog.setButtonWidth(buttonWidth.intValue());
			}
			case SERVER_LINKS -> {
				dialog.setExitButton(single(entries.exitButton(), ButtonWrapper.class, event));
				Number columns = single(entries.columns(), Number.class, event);
				if (columns != null) dialog.setColumns(columns.intValue());
				Number buttonWidth = single(entries.buttonWidth(), Number.class, event);
				if (buttonWidth != null) dialog.setButtonWidth(buttonWidth.intValue());
			}
		}
		return dialog;
	}

	private static <T> @Nullable T single(@Nullable Expression<?> expression, Class<T> type, Event event) {
		if (expression == null) return null;
		Object value = expression.getSingle(event);
		return type.isInstance(value) ? type.cast(value) : null;
	}

	private static <T> void addAll(@Nullable Expression<?> expression, Class<T> type, Event event, List<? super T> target) {
		if (expression == null) return;
		for (Object value : expression.getArray(event)) {
			if (type.isInstance(value)) target.add(type.cast(value));
		}
	}

	private DialogKind kind;
	private DialogEntries entries;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		kind = matchedPattern == 1 ? DialogKind.DIALOG_LIST : kindFromTags(parseResult);
		if (sectionNode == null) {
			Skript.error("A dialog needs entries inside it.");
			return false;
		}
		EntryContainer container = validatorFor(kind).validate(sectionNode);
		if (container == null) return false;
		entries = resolve(kind, container);
		return entries != null;
	}

	private static DialogKind kindFromTags(SkriptParser.ParseResult parseResult) {
		if (parseResult.hasTag("notice")) return DialogKind.NOTICE;
		if (parseResult.hasTag("confirmation")) return DialogKind.CONFIRMATION;
		if (parseResult.hasTag("multi")) return DialogKind.MULTI_ACTION;
		return DialogKind.SERVER_LINKS;
	}

	@Override
	public boolean isSectionOnly() {
		return true;
	}

	@Override
	protected DialogWrapper @Nullable [] get(Event event) {
		return new DialogWrapper[]{build(kind, entries, event)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends DialogWrapper> getReturnType() {
		return DialogWrapper.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "new " + kind.getName() + (kind == DialogKind.DIALOG_LIST ? "" : " dialog");
	}

}
