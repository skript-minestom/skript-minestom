package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.TimedPotion;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Active Potion Effects")
@Description("""
	Every potion effect currently applied to an entity. The duration of one of these is the duration it \
	was applied with, not the time it has left.""")
@Examples("""
	set {_potions} to active potion effects of player
	clear active potion effects of player""")
public class ExprActivePotions extends PropertyExpression<Entity, Potion> {

	static {
		register(ExprActivePotions.class, Potion.class, "active potion[ effect]s", "entities");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Entity>) expressions[0]);
		return true;
	}

	@Override
	protected Potion[] get(Event event, Entity[] source) {
		List<Potion> potions = new ArrayList<>();
		for (Entity entity : source) {
			for (TimedPotion timedPotion : entity.getActiveEffects())
				potions.add(timedPotion.potion());
		}
		return potions.toArray(new Potion[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case DELETE, RESET -> CollectionUtils.array();
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		for (Entity entity : getExpr().getArray(event))
			entity.clearEffects();
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Potion> getReturnType() {
		return Potion.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "active potion effects of " + getExpr().toString(event, debug);
	}

}
