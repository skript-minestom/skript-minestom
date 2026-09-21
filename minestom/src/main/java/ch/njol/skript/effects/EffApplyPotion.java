package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Potions;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Apply Potion Effect")
@Description("""
	Applies potion effects to entities. A level of 2 is what the game shows as II, and leaving the level \
	out applies level 1. Leaving the duration out lasts 30 seconds, the same as the effect command, \
	and an infinite duration lasts until the effect is removed. Effects come with particles and an icon \
	unless they are turned off.""")
@Examples("""
	apply speed 2 to player for 10 seconds
	apply night vision to all players for 1 minute without particles and without icon
	apply {_potion} to player""")
public class EffApplyPotion extends Effect {

	static {
		Skript.registerEffect(EffApplyPotion.class,
			"apply %potions% to %entities%",
			"apply %potioneffecttypes% [[of] (tier|level)] [%-number%] to %entities% [(for %-timespan%|infinite:forever)] "
				+ "[noparticles:[and] without particles] [noicon:[and] without icon] "
				+ "[ambient:[and] [as] ambient] [blend:[and] with blend[ing]]");
	}

	private @Nullable Expression<Potion> potions;
	private @Nullable Expression<PotionEffect> effects;
	private @Nullable Expression<Number> level;
	private @Nullable Expression<Timespan> duration;
	private Expression<Entity> entities;
	private boolean infinite;
	private byte flags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) {
			potions = (Expression<Potion>) expressions[0];
			entities = (Expression<Entity>) expressions[1];
			return true;
		}
		effects = (Expression<PotionEffect>) expressions[0];
		level = (Expression<Number>) expressions[1];
		entities = (Expression<Entity>) expressions[2];
		duration = (Expression<Timespan>) expressions[3];
		infinite = parseResult.hasTag("infinite");
		flags = Potions.flagsOf(!parseResult.hasTag("noparticles"), !parseResult.hasTag("noicon"),
			parseResult.hasTag("ambient"), parseResult.hasTag("blend"));
		return true;
	}

	@Override
	protected void execute(Event event) {
		Entity[] entities = this.entities.getArray(event);
		if (entities.length == 0) return;

		if (potions != null) {
			for (Potion potion : potions.getArray(event)) {
				for (Entity entity : entities)
					entity.addEffect(potion);
			}
			return;
		}

		int amplifier = Potions.amplifierOf(level == null ? null : level.getSingle(event));
		int duration = infinite
			? Potion.INFINITE_DURATION
			: Potions.durationTicksOf(this.duration == null ? null : this.duration.getSingle(event));
		for (PotionEffect effect : effects.getArray(event)) {
			Potion potion = new Potion(effect, amplifier, duration, flags);
			for (Entity entity : entities)
				entity.addEffect(potion);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("apply", potions != null ? potions : effects);
		if (level != null) sb.append("of level", level);
		sb.append("to", entities);
		if (duration != null) sb.append("for", duration);
		else if (infinite) sb.append("forever");
		return sb.toString();
	}

}
