package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.CustomChatCompletionPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Name("Player Chat Completions")
@Description("""
	The custom chat completion suggestions. You can add, set, remove, and clear them. Removing the names of online players with this expression is ineffective.
	This expression will not return anything due to server limitations.""")
@Example("add \"Skript\" and \"Njol\" to chat completions of all players")
@Example("remove \"text\" from {_p}'s chat completions")
@Example("clear player's chat completions")
public class ExprPlayerChatCompletions extends SimplePropertyExpression<Player, String> {

	static {
		register(ExprPlayerChatCompletions.class, String.class, "[custom] chat completion[s]", "players");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!ExprCamera.inEffChange(this)) {
			Skript.error("You can't get the chat completions of a player.");
			return false;
		}
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable String convert(Player player) {
		return null;
	}

	@Override
	public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
		return switch (mode) {
			case ADD, SET, REMOVE, DELETE, RESET -> CollectionUtils.array(String[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
		Player[] players = getExpr().getArray(event);
		if (players.length == 0)
			return;
		List<String> completions = new ArrayList<>();
		if (delta != null && (mode == ChangeMode.ADD || mode == ChangeMode.REMOVE || mode == ChangeMode.SET)) {
			completions = Arrays.stream(delta)
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.collect(Collectors.toList());
		}

		CustomChatCompletionPacket packet = new CustomChatCompletionPacket(getAction(mode), completions);

		for (Player player : players) {
			player.sendPacket(packet);
		}
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	protected String getPropertyName() {
		return "custom chat completions";
	}

	private CustomChatCompletionPacket.Action getAction(ChangeMode mode) {
		return switch (mode) {
			case DELETE, RESET, SET -> CustomChatCompletionPacket.Action.SET;
			case ADD -> CustomChatCompletionPacket.Action.ADD;
			default -> CustomChatCompletionPacket.Action.REMOVE;
		};
	}

}
