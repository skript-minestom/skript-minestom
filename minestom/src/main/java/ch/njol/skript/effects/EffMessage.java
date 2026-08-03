package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import org.bukkit.event.Event;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Message")
@Description("""
	Sends a message to the given player. Only styles written
	in given string or in <a href=expressions.html#ExprColored>formatted expressions</a> will be parsed.
	Adding an optional sender allows the messages to be sent as if a specific player sent them.
	This is useful with Minecraft 1.16.4's new chat ignore system, in which players can choose to ignore other players,
	but for this to work, the message needs to be sent from a player.""")
@Examples("""
	message "A wild %player% appeared!"
	message "This message is a distraction. Mwahaha!"
	send "Your kill streak is %{kill streak::%uuid of player%}%." to player
	if the {_entity} exists:
		message "You're currently looking at a %type of the {_entity}%!"
	on chat:
		cancel event
		send "[%player%] >> %message%" to all players from player""")
public class EffMessage extends Effect {

	static {
		Skript.registerEffect(EffMessage.class, "(message|send [message[s]]) %objects% [to %senders%]",
			"broadcast %objects%");
	}

	private Expression<?>[] messages;
	private Expression<?> messageExpr;
	private Expression<CommandSender> recipients;
	private int pattern;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		messageExpr = LiteralUtils.defendExpression(exprs[0]);

		messages = messageExpr instanceof ExpressionList ?
			((ExpressionList<?>) messageExpr).getExpressions() : new Expression[] {messageExpr};
		if (matchedPattern == 0) recipients = (Expression<CommandSender>) exprs[1];
		pattern = matchedPattern;
		return LiteralUtils.canInitSafely(messageExpr);
	}

	@Override
	protected void execute(Event e) {
		CommandSender[] commandSenders;
		if (pattern == 0) commandSenders = recipients.getArray(e);
		else {
			List<CommandSender> senders = new ArrayList<>();
			senders.add(MinecraftServer.getCommandManager().getConsoleSender());
			senders.addAll(MinecraftServer.getConnectionManager().getOnlinePlayers());
			commandSenders = senders.toArray(new CommandSender[0]);
		}

		List<Component> components = new ArrayList<>();
		for (Expression<?> expression : getMessages()) {
			if (expression instanceof VariableString vs) {
				for (String s : vs.getArray(e)) {
					components.add(Component.text(onlyValid(s)));
				}
			} else {
				for (Object o : expression.getArray(e)) {
					if (o instanceof ComponentWrapper wrapper) components.add(wrapper.getComponent());
					else components.add(Component.text(onlyValid(toString(o))));
				}
			}
		}
		for (Component component : components) {
			for (CommandSender commandSender : commandSenders) {
				commandSender.sendMessage(component);
			}
		}
	}

	private Expression<?>[] getMessages() {
		if (messageExpr instanceof ExpressionList && !messageExpr.getAnd()) {
			return new Expression[] {CollectionUtils.getRandom(messages)};
		}
		return messages;
	}

	// NetworkBufferTypeImpl @ 979
	@SuppressWarnings({"ConstantValue", "UnnecessaryLocalVariable"})
	private String onlyValid(String s) {
		final int strlen = s.length();
		int utflen = strlen;
		if (utflen > 65000 || /* overflow */ utflen < strlen) return s.substring(0, 65000);
		return s;
	}

	private String toString(Object object) {
		return object instanceof String ? (String) object : Classes.toString(object);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "send " + messageExpr.toString(e, debug) + " to " + recipients.toString(e, debug);
	}

}
