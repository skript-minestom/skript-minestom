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
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("New Potion")
@Description("""
	Creates a potion effect that can be stored and applied later. A level of 2 is what the game shows \
	as II, and leaving the level out makes it level 1. Leaving the duration out lasts 30 seconds.""")
@Examples("""
	set {_potion} to new potion of poison 3 for 5 seconds
	set {_potion} to a new potion of speed for 1 minute without particles
	apply {_potion} to all players""")
public class ExprNewPotion extends SimpleExpression<Potion> {

	static {
		Skript.registerExpression(ExprNewPotion.class, Potion.class, ExpressionType.COMBINED,
			"[a] [new] potion [effect] of %potioneffecttype% [[of] (tier|level)] [%-number%] [(for %-timespan%|infinite:forever)] "
				+ "[noparticles:[and] without particles] [noicon:[and] without icon] "
				+ "[ambient:[and] [as] ambient] [blend:[and] with blend[ing]]");
	}

	private Expression<PotionEffect> effect;
	private @Nullable Expression<Number> level;
	private @Nullable Expression<Timespan> duration;
	private boolean infinite;
	private byte flags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		effect = (Expression<PotionEffect>) expressions[0];
		level = (Expression<Number>) expressions[1];
		duration = (Expression<Timespan>) expressions[2];
		infinite = parseResult.hasTag("infinite");
		flags = Potions.flagsOf(!parseResult.hasTag("noparticles"), !parseResult.hasTag("noicon"),
			parseResult.hasTag("ambient"), parseResult.hasTag("blend"));
		return true;
	}

	@Override
	protected Potion @Nullable [] get(Event event) {
		PotionEffect effect = this.effect.getSingle(event);
		if (effect == null) return new Potion[0];
		int amplifier = Potions.amplifierOf(level == null ? null : level.getSingle(event));
		int duration = infinite
			? Potion.INFINITE_DURATION
			: Potions.durationTicksOf(this.duration == null ? null : this.duration.getSingle(event));
		return new Potion[]{new Potion(effect, amplifier, duration, flags)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Potion> getReturnType() {
		return Potion.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("new potion of", effect);
		if (level != null) sb.append("of level", level);
		if (duration != null) sb.append("for", duration);
		else if (infinite) sb.append("forever");
		return sb.toString();
	}

}
