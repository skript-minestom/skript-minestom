package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * @author Peter Güttinger
 */
@Name("Speed")
@Description("""
	A player's walking or flying speed. Both can be changed, but values must be between -1 and 1 (excessive values will be changed to -1 or 1 respectively). Negative values reverse directions.
	Please note that changing a player's speed will change their FOV just like potions do.""")
@Example("set the player's walk speed to 1")
@Example("increase the argument's fly speed by 0.1")
public class ExprSpeed extends SimplePropertyExpression<Player, Number> {

	static {
		register(ExprSpeed.class, Number.class, "(0¦walk[ing]|1¦fl(y[ing]|ight))[( |-)]speed", "players");
	}

	private boolean walk;

	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		super.init(exprs, matchedPattern, isDelayed, parseResult);
		walk = parseResult.mark == 0;
		return true;
	}

	@Override
	public Number convert(final Player p) {
		return getSpeed(p);
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(final ChangeMode mode) {
		if (mode == ChangeMode.SET || mode == ChangeMode.RESET || mode == ChangeMode.ADD || mode == ChangeMode.REMOVE)
			return new Class[] {Number.class};
		return null;
	}

	@Override
	public void change(final Event e, final @Nullable Object[] delta, final ChangeMode mode) throws UnsupportedOperationException {
		float input = delta == null ? 0 : ((Number) delta[0]).floatValue();

		for (final Player p : getExpr().getArray(e)) {
			float oldSpeed = getSpeed(p);

			float newSpeed = switch (mode) {
				case SET -> input;
				case ADD -> oldSpeed + input;
				case REMOVE -> oldSpeed - input;
				default -> walk ? 0.1f : 0.05f;
			};

			final float d = Math2.fit(-1, newSpeed, 1);

			setSpeed(p, d);
		}
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return walk ? "walk speed" : "fly speed";
	}

	private Attribute getSpeedType() {
		return walk ? Attribute.MOVEMENT_SPEED : Attribute.FLYING_SPEED;
	}

	private void setSpeed(Player player, float amount) {
		Attribute speedType = getSpeedType();
		if (speedType == Attribute.FLYING_SPEED) player.setFlyingSpeed(amount);
		player.getAttribute(speedType).setBaseValue(amount);
	}

	private float getSpeed(Player player) {
		return (float) player.getAttributeValue(getSpeedType());
	}

}
