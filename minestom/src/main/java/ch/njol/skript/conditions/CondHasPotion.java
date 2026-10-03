package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Potions;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.PotionEffect;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Has Potion Effect")
@Description("Checks whether entities currently have the given potion effects.")
@Examples("""
	if player has speed:
	if player doesn't have potion effect poison:""")
public class CondHasPotion extends Condition {

	static {
		Skript.registerCondition(CondHasPotion.class,
			"%entities% (has|have) [[the] potion [effect]] %potions/potioneffecttypes%",
			"%entities% (doesn't|does not|do not|don't) have [[the] potion [effect]] %potions/potioneffecttypes%");
	}

	private Expression<Entity> entities;
	private Expression<?> effects;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		entities = (Expression<Entity>) expressions[0];
		effects = expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		return entities.check(event,
			entity -> effects.check(event,
				object -> {
					PotionEffect effect = Potions.effectOf(object);
					return effect != null && entity.hasEffect(effect);
				}), isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, entities,
			"the potion effect " + effects.toString(event, debug));
	}

}
