package ch.njol.skript.util.dialog;

import ch.njol.skript.lang.Trigger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.dialog.DialogActionButton;
import org.jetbrains.annotations.Nullable;

public class ButtonWrapper {

	private volatile Component label;
	private volatile @Nullable Component tooltip;
	private volatile int width = DialogActionButton.DEFAULT_WIDTH;
	private volatile @Nullable DialogAction action;
	private volatile @Nullable Trigger trigger;
	private volatile @Nullable Key callbackKey;

	public ButtonWrapper(Component label) {
		this.label = label;
	}

	public Component getLabel() {
		return label;
	}

	public void setLabel(Component label) {
		this.label = label;
	}

	public void setTooltip(@Nullable Component tooltip) {
		this.tooltip = tooltip;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setAction(@Nullable DialogAction action) {
		this.action = action;
		this.trigger = null;
		this.callbackKey = null;
	}

	public void setTrigger(Trigger trigger, Key callbackKey) {
		this.trigger = trigger;
		this.callbackKey = callbackKey;
		this.action = new DialogAction.DynamicCustom(callbackKey, null);
	}

	public @Nullable Trigger getTrigger() {
		return trigger;
	}

	public @Nullable Key getCallbackKey() {
		return callbackKey;
	}

	public DialogActionButton toMinestom() {
		return new DialogActionButton(label, tooltip, width, action);
	}

}
