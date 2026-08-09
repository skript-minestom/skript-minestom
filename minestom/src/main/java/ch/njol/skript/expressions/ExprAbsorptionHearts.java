package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.avatar.PlayerMeta;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Absorption Hearts")
@Description("The absorption hearts of a player.")
@Examples("set absorption hearts of player to 20")
public class ExprAbsorptionHearts extends SimplePropertyExpression<Player, Number> {

	static {
		register(ExprAbsorptionHearts.class, Number.class, "absorption hearts", "players");
	}

	@Override
	public @Nullable Number convert(Player from) {
		return from.getPlayerMeta().getAdditionalHearts();
	}

	@Override
	public Class<?> @org.jspecify.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, REMOVE, ADD, RESET -> CollectionUtils.array(Number.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		Player[] players = getExpr().getArray(event);
		Float absorptionHearts = delta == null ? null : ((Number) delta[0]).floatValue();
		for (Player player : players) {
			PlayerMeta playerMeta = player.getPlayerMeta();
			if (mode == Changer.ChangeMode.RESET) {
				playerMeta.setAdditionalHearts(0);
				continue;
			}
			if (absorptionHearts == null) return;
			float current = playerMeta.getAdditionalHearts();
			switch (mode) {
				case ADD -> playerMeta.setAdditionalHearts(current+absorptionHearts);
				case REMOVE -> playerMeta.setAdditionalHearts(current-absorptionHearts);
				case SET -> playerMeta.setAdditionalHearts(absorptionHearts);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "absorption hearts";
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

}
