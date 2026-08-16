package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.util.NullableChecker;
import ch.njol.util.coll.iterator.CheckedIterator;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// chatgpt conversion
/**
 * Minestom version of BlockSphereIterator
 */
public class BlockSphereIterator extends CheckedIterator<BlockVec> {

	public BlockSphereIterator(@NotNull Point center, double radius, @Nullable Instance instance) {
		super(
			new AABB(
				center,
				radius + 0.5001,
				radius + 0.5001,
				radius + 0.5001,
				instance
			).iterator(),
			new NullableChecker<>() {

				private final double rSquared = radius * radius * Skript.EPSILON_MULT;
				private final Vec centerVec = new Vec(center.x(), center.y(), center.z());

				@Override
				public boolean check(@Nullable BlockVec b) {
					if (b == null) return false;

					Vec blockCenter = new Vec(
						b.x() + 0.5,
						b.y() + 0.5,
						b.z() + 0.5
					);

					return blockCenter.distanceSquared(centerVec) < rSquared;
				}
			}
		);
	}
}