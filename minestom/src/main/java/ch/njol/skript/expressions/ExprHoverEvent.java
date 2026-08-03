package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("rawtypes")

@Name("Hover Event")
@Description("The hover event of a component.")
@Examples("""
	set hover event of {_component} to hover event showing "Hello\"""")
public class ExprHoverEvent extends SimplePropertyExpression<ComponentWrapper, HoverEvent> {

	static {
		register(ExprHoverEvent.class, HoverEvent.class, "hover event", "components");
	}

	@Override
	public @Nullable HoverEvent convert(ComponentWrapper from) {
		return from.getComponent().hoverEvent();
	}

	@Override
	public Class<?> @org.jetbrains.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) return CollectionUtils.array(HoverEvent.class);
		return null;
	}

	@Override
	public void change(Event event, Object @org.jetbrains.annotations.Nullable [] delta, Changer.ChangeMode mode) {
		HoverEvent hoverEvent = (HoverEvent) delta[0];
		if (hoverEvent == null) return;
		for (ComponentWrapper component : getExpr().getArray(event)) {
			component.modify(c -> c.hoverEvent(hoverEvent));
		}
	}

	@Override
	protected String getPropertyName() {
		return "hover event";
	}

	@Override
	public Class<? extends HoverEvent> getReturnType() {
		return HoverEvent.class;
	}

}
