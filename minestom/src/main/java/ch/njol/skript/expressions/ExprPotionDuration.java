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
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Potion Duration")
@Description("""
	The duration a potion effect was applied with, or the time an effect an entity has still has left. \
	An effect applied with an infinite duration gives an infinite timespan back.""")
@Examples("""
	set {_duration} to duration of {_potion}
	set {_left} to remaining time of speed of player""")
public class ExprPotionDuration extends SimpleExpression<Timespan> {

	static {
		Skript.registerExpression(ExprPotionDuration.class, Timespan.class, ExpressionType.PROPERTY,
			"[the] [potion] duration of %potions%",
			"[the] remaining [potion] (time|duration) of %potioneffecttypes% (of|on) %entities%");
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
	protected Timespan @Nullable [] get(Event event) {
		List<Timespan> durations = new ArrayList<>();
		if (potions != null) {
			for (Potion potion : potions.getArray(event))
				durations.add(Potions.timespanOf(potion.duration()));
			return durations.toArray(new Timespan[0]);
		}
		for (Entity entity : entities.getArray(event)) {
			for (PotionEffect effect : effects.getArray(event)) {
				TimedPotion timedPotion = entity.getEffect(effect);
				if (timedPotion != null)
					durations.add(Potions.timespanOf(Potions.remainingTicksOf(entity, timedPotion)));
			}
		}
		return durations.toArray(new Timespan[0]);
	}

	@Override
	public boolean isSingle() {
		if (potions != null) return potions.isSingle();
		return effects.isSingle() && entities.isSingle();
	}

	@Override
	public Class<? extends Timespan> getReturnType() {
		return Timespan.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		if (potions != null) {
			sb.append("potion duration of", potions);
		} else {
			sb.append("remaining potion time of", effects, "of", entities);
		}
		return sb.toString();
	}

}
