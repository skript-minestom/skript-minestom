package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.InputKey;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Player Input Keys")
@Description("Get the current input keys of a player.")
@Examples("""
	broadcast "%player% is pressing %current input keys of player%\"""")
public class ExprCurrentInputKeys extends PropertyExpression<Player, InputKey> {

	static {
		register(ExprCurrentInputKeys.class, InputKey.class, "[current] (inputs|input keys)", "players");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		setExpr((Expression<? extends Player>) expressions[0]);
		return true;
	}

	@Override
	protected InputKey[] get(Event event, Player[] source) {

		List<InputKey> inputKeys = new ArrayList<>();
		for (Player player : source) {
			inputKeys.addAll(InputKey.fromInput(player.inputs()));
		}
		return inputKeys.toArray(new InputKey[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends InputKey> getReturnType() {
		return InputKey.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "the current input keys of " + getExpr().toString(event, debug);
	}

}
