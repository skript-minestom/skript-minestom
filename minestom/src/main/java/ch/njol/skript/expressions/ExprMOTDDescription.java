package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.ServerListPingWrapper;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;
import static com.github.hapily04.skriptminestom.util.MessageUtils.BASIC_MINI_MESSAGE;


@Name("MOTD Description")
@Description("The description shown in the server list ping event.")
@Examples("""
	set motd description to "<green>Welcome!\"""")
public class ExprMOTDDescription extends SimpleExpression<ComponentWrapper> implements EventRestrictedSyntax {

	static {
		Skript.registerExpression(ExprMOTDDescription.class, ComponentWrapper.class, ExpressionType.EVENT,
			"motd description");
	}

	private static final Component DEFAULT_DESCRIPTION = BASIC_MINI_MESSAGE.deserialize("<gray>Minestom Server");

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected @Nullable ComponentWrapper[] get(Event event) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		return new ComponentWrapper[]{toWrapper(e.getStatus().description())};
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.SET) return CollectionUtils.array(ComponentWrapper.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		Status currentStatus = e.getStatus();
		Component description;
		if (mode == Changer.ChangeMode.RESET) description = DEFAULT_DESCRIPTION;
		else {
			ComponentWrapper wrapper = (ComponentWrapper) delta[0];
			if (wrapper == null) return;
			description = wrapper.getComponent();
		}
		Status newStatus = Status.builder(currentStatus)
			.description(description)
			.build();
		e.setStatus(newStatus);
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "motd description";
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return new Class[]{ServerListPingWrapper.class};
	}

}
