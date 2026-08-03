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
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;
import net.minestom.server.utils.identity.NamedAndIdentified;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ch.njol.skript.expressions.ExprMOTDPlayerCount.getPlayerInfo;


@Name("MOTD Hover Sample")
@Description("The hover/player sample text in the server list ping event.")
@Examples("""
	set motd hover sample to mm("<red>Notch") and mm("<rainbow>jeb_")""")
public class ExprMOTDHoverSample extends SimpleExpression<ComponentWrapper> implements EventRestrictedSyntax {

	static {
		Skript.registerExpression(ExprMOTDHoverSample.class, ComponentWrapper.class, ExpressionType.EVENT,
			"motd (hover|player) sample");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected @Nullable ComponentWrapper[] get(Event event) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		Status.PlayerInfo playerInfo = getPlayerInfo(e.getStatus());
		return playerInfo.sample().stream().map(NamedAndIdentified::getName).map(ComponentWrapper::new).toArray(ComponentWrapper[]::new);
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.SET) return CollectionUtils.array(ComponentWrapper[].class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		Status currentStatus = e.getStatus();
		Status.PlayerInfo oldPlayerInfo = getPlayerInfo(currentStatus);
		List<NamedAndIdentified> sample;
		if (mode == Changer.ChangeMode.RESET) {
			sample = List.of();
		} else {
			sample = new ArrayList<>();
			for (Object o : delta) {
				if (o == null) continue;
				sample.add(NamedAndIdentified.named(((ComponentWrapper) o).getComponent()));
			}
		}
		Status newStatus = Status.builder(currentStatus)
			.playerInfo(new Status.PlayerInfo(oldPlayerInfo.onlinePlayers(), oldPlayerInfo.maxPlayers(), sample))
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
		return "motd hover sample";
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return new Class[]{ServerListPingWrapper.class};
	}

}
