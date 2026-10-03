package ch.njol.skript.util;

import net.minestom.server.entity.Entity;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import org.eclipse.jdt.annotation.Nullable;

public final class Potions {

	public static final int DEFAULT_DURATION_TICKS = 600;

	private Potions() { }

	public static int amplifierOf(@Nullable Number level) {
		if (level == null) return 0;
		double value = level.doubleValue();
		if (Double.isNaN(value)) return 0;
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value - 1));
	}

	public static int levelOf(Potion potion) {
		return potion.amplifier() + 1;
	}

	public static int durationTicksOf(@Nullable Timespan timespan) {
		if (timespan == null) return DEFAULT_DURATION_TICKS;
		if (timespan.isInfinite()) return Potion.INFINITE_DURATION;
		return (int) Math.max(0, Math.min(Integer.MAX_VALUE, timespan.getAs(Timespan.TimePeriod.TICK)));
	}

	public static Timespan timespanOf(long ticks) {
		if (ticks == Potion.INFINITE_DURATION) return Timespan.infinite();
		return new Timespan(Timespan.TimePeriod.TICK, Math.max(0, ticks));
	}

	public static byte flagsOf(boolean particles, boolean icon, boolean ambient, boolean blend) {
		int flags = 0;
		if (particles) flags |= Potion.PARTICLES_FLAG;
		if (icon) flags |= Potion.ICON_FLAG;
		if (ambient) flags |= Potion.AMBIENT_FLAG;
		if (blend) flags |= Potion.BLEND_FLAG;
		return (byte) flags;
	}

	public static long remainingTicksOf(Entity entity, TimedPotion potion) {
		int duration = potion.potion().duration();
		if (duration == Potion.INFINITE_DURATION) return Potion.INFINITE_DURATION;
		return Math.max(0, potion.startingTicks() + duration - entity.getAliveTicks());
	}

	public static @Nullable PotionEffect effectOf(@Nullable Object object) {
		if (object instanceof PotionEffect effect) return effect;
		if (object instanceof Potion potion) return potion.effect();
		return null;
	}

}
