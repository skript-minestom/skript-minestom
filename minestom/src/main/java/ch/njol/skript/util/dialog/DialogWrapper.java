package ch.njol.skript.util.dialog;

import ch.njol.skript.log.SkriptLogger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.*;
import net.minestom.server.registry.Holder;
import net.minestom.server.registry.HolderSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DialogWrapper {

	private final DialogKind kind;

	private Component title = Component.empty();
	private @Nullable Component externalTitle;
	private boolean canCloseWithEscape = true;
	private boolean pause = false;
	private DialogAfterAction afterAction = DialogAfterAction.CLOSE;
	private final List<DialogBody> bodies = new CopyOnWriteArrayList<>();
	private final List<DialogInput> inputs = new CopyOnWriteArrayList<>();

	private @Nullable ButtonWrapper button;
	private @Nullable ButtonWrapper yesButton;
	private @Nullable ButtonWrapper noButton;
	private final List<ButtonWrapper> buttons = new CopyOnWriteArrayList<>();
	private @Nullable ButtonWrapper exitButton;
	private int columns = 2;
	private int buttonWidth = 150;
	private final List<DialogWrapper> children = new CopyOnWriteArrayList<>();

	private @Nullable Key key;

	public DialogWrapper(DialogKind kind) {
		this.kind = kind;
	}

	public DialogKind getKind() {
		return kind;
	}

	public Component getTitle() {
		return title;
	}

	public void setTitle(Component title) {
		this.title = title;
	}

	public void setExternalTitle(@Nullable Component externalTitle) {
		this.externalTitle = externalTitle;
	}

	public void setCanCloseWithEscape(boolean canCloseWithEscape) {
		this.canCloseWithEscape = canCloseWithEscape;
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	public void setAfterAction(DialogAfterAction afterAction) {
		this.afterAction = afterAction;
	}

	public List<DialogBody> getBodies() {
		return bodies;
	}

	public List<DialogInput> getInputs() {
		return inputs;
	}

	public List<ButtonWrapper> getButtons() {
		return buttons;
	}

	public @Nullable ButtonWrapper getButton() {
		return button;
	}

	public void setButton(@Nullable ButtonWrapper button) {
		this.button = button;
	}

	public void setYesButton(@Nullable ButtonWrapper yesButton) {
		this.yesButton = yesButton;
	}

	public void setNoButton(@Nullable ButtonWrapper noButton) {
		this.noButton = noButton;
	}

	public void setExitButton(@Nullable ButtonWrapper exitButton) {
		this.exitButton = exitButton;
	}

	public void setColumns(int columns) {
		this.columns = columns;
	}

	public void setButtonWidth(int buttonWidth) {
		this.buttonWidth = buttonWidth;
	}

	public List<DialogWrapper> getChildren() {
		return children;
	}

	public @Nullable Key getKey() {
		return key;
	}

	public void setKey(@Nullable Key key) {
		this.key = key;
	}

	public DialogMetadata toMetadata() {
		boolean pause = this.pause;
		if (pause && afterAction == DialogAfterAction.NONE) {
			SkriptLogger.LOGGER.warn("A dialog had 'pause: true' together with 'after action: none'; forcing pause off.");
			pause = false;
		}
		return new DialogMetadata(title, externalTitle, canCloseWithEscape, pause, afterAction,
			List.copyOf(bodies), List.copyOf(inputs));
	}

	public Dialog toMinestom() {
		DialogMetadata metadata = toMetadata();
		return switch (kind) {
			case NOTICE -> new Dialog.Notice(metadata, buttonOr(button, "gui.ok"));
			case CONFIRMATION -> new Dialog.Confirmation(metadata, buttonOr(yesButton, "gui.yes"), buttonOr(noButton, "gui.no"));
			case MULTI_ACTION -> new Dialog.MultiAction(metadata, toButtons(buttons), exit(), columns);
			case DIALOG_LIST -> new Dialog.DialogList(metadata, toHolderSet(), exit(), columns, buttonWidth);
			case SERVER_LINKS -> new Dialog.ServerLinks(metadata, exit(), columns, buttonWidth);
		};
	}

	public Holder<Dialog> toHolder() {
		return toMinestom();
	}

	private HolderSet<Dialog> toHolderSet() {
		List<Dialog> dialogs = new ArrayList<>(children.size());
		for (DialogWrapper child : children) dialogs.add(child.toMinestom());
		return new HolderSet.Direct<>(dialogs);
	}

	private List<DialogActionButton> toButtons(List<ButtonWrapper> wrappers) {
		List<DialogActionButton> list = new ArrayList<>(wrappers.size());
		for (ButtonWrapper wrapper : wrappers) list.add(wrapper.toMinestom());
		return list;
	}

	private @Nullable DialogActionButton exit() {
		return exitButton == null ? null : exitButton.toMinestom();
	}

	private DialogActionButton buttonOr(@Nullable ButtonWrapper wrapper, String fallbackTranslationKey) {
		if (wrapper != null) return wrapper.toMinestom();
		return new DialogActionButton(Component.translatable(fallbackTranslationKey), null, DialogActionButton.DEFAULT_WIDTH, null);
	}

}
