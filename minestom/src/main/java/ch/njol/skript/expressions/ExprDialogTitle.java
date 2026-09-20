package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.dialog.DialogWrapper;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;

@Name("Dialog Title")
@Description("The title of a dialog. Changing it only affects dialogs shown after the change.")
@Examples("set title of {_d} to mm(\"<gold>Shop\")")
@Keywords({"dialog"})
public class ExprDialogTitle extends SimplePropertyExpression<DialogWrapper, ComponentWrapper> {

	static {
		register(ExprDialogTitle.class, ComponentWrapper.class, "[dialog] title", "dialogs");
	}

	@Override
	public ComponentWrapper convert(DialogWrapper from) {
		return toWrapper(from.getTitle());
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, DELETE, RESET -> CollectionUtils.array(ComponentWrapper.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Component title = Component.empty();
		if (mode == Changer.ChangeMode.SET) {
			if (delta == null || delta[0] == null) return;
			title = ((ComponentWrapper) delta[0]).getComponent();
		}
		for (DialogWrapper dialog : getExpr().getArray(event)) dialog.setTitle(title);
	}

	@Override
	protected String getPropertyName() {
		return "title";
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

}
