package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Potions;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Potion Level")
@Description("""
	The level of a potion effect, either of a potion itself or of an effect an entity currently has. \
	A level of 2 is what the game shows as II. An entity that doesn't have the effect gives nothing.""")
@Examples("""
	set {_level} to level of speed of player
	set {_level} to potion level of {_potion}""")
public class ExprPotionLevel extends SimpleExpression<Number> {

	static {
		Skript.registerExpression(ExprPotionLevel.class, Number.class, ExpressionType.PROPERTY,
			"[the] (potion|effect) (level|tier) of %potions%",
			"[the] [potion] (level|tier) of %potioneffecttypes% (of|on) %entities%");
	}

	private @Nullable Expression<Potion> potions;
	private @Nullable Expression<PotionEffect> effects;
	private @Nullable Expression<Entity> entities;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) {
			potions = (Expression<Potion>) expressions[0];
		} else {
			effects = (Expression<PotionEffect>) expressions[0];
			entities = (Expression<Entity>) expressions[1];
		}
		return true;
	}

	@Override
	protected Number @Nullable [] get(Event event) {
		List<Number> levels = new ArrayList<>();
		if (potions != null) {
			for (Potion potion : potions.getArray(event))
				levels.add(Potions.levelOf(potion));
			return levels.toArray(new Number[0]);
		}
		for (Entity entity : entities.getArray(event)) {
			for (PotionEffect effect : effects.getArray(event)) {
				TimedPotion timedPotion = entity.getEffect(effect);
				if (timedPotion != null) levels.add(Potions.levelOf(timedPotion.potion()));
			}
		}
		return levels.toArray(new Number[0]);
	}

	@Override
	public boolean isSingle() {
		if (potions != null) return potions.isSingle();
		return effects.isSingle() && entities.isSingle();
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("potion level of", potions != null ? potions : effects);
		if (entities != null) sb.append("of", entities);
		return sb.toString();
	}

}
