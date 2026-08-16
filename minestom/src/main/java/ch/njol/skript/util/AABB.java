package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.util.Math2;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

// almost entirely converted using chatgpt to minestom
/**
 * AABB = Axis-Aligned Bounding Box
 *
 * Minestom port:
 * - world -> Instance
 * - Location/Vector -> Point/Vec
 * - Block -> BlockVec (block coordinates)
 */
public final class AABB implements Iterable<BlockVec> {

	final @Nullable Instance instance;
	final Point lowerBound, upperBound;

	/**
	 * Supports optional instance for two points.
	 * If instance is null, no Y clamping is performed.
	 */
	public AABB(@NotNull Point p1, @NotNull Point p2, @Nullable Instance instance) {
		this.instance = instance;

		// block coordinates (same as Bukkit getBlockX/Y/Z) so ceil/floor iterator bounds stay valid
		int x1 = p1.blockX(), y1 = p1.blockY(), z1 = p1.blockZ();
		int x2 = p2.blockX(), y2 = p2.blockY(), z2 = p2.blockZ();

		double minX = Math.min(x1, x2);
		double maxX = Math.max(x1, x2);
		double minY = Math.min(y1, y2);
		double maxY = Math.max(y1, y2);
		double minZ = Math.min(z1, z2);
		double maxZ = Math.max(z1, z2);

		if (instance != null) {
			DimensionType type = instance.getCachedDimensionType();
			double dimMinY = type.minY();
			double dimMaxY = type.minY() + type.height() - 1; // correct even if minY != 0
			minY = Math.max(minY, dimMinY);
			maxY = Math.min(maxY, dimMaxY);
		}

		this.lowerBound = new Vec(minX, minY, minZ);
		this.upperBound = new Vec(maxX, maxY, maxZ);
	}

	public AABB(@NotNull BlockVec b1, @NotNull BlockVec b2, @Nullable Instance instance) {
		this.instance = instance;

		double minX = Math.min(b1.x(), b2.x());
		double maxX = Math.max(b1.x(), b2.x());
		double minY = Math.min(b1.y(), b2.y());
		double maxY = Math.max(b1.y(), b2.y());
		double minZ = Math.min(b1.z(), b2.z());
		double maxZ = Math.max(b1.z(), b2.z());

		if (instance != null) {
			DimensionType type = instance.getCachedDimensionType();
			double dimMinY = type.minY();
			double dimMaxY = type.minY() + type.height() - 1;
			minY = Math.max(minY, dimMinY);
			maxY = Math.min(maxY, dimMaxY);
		}

		this.lowerBound = new Vec(minX, minY, minZ);
		this.upperBound = new Vec(maxX, maxY, maxZ);
	}

	public AABB(@NotNull Point center, double rX, double rY, double rZ, @Nullable Instance instance) {
		assert rX >= 0 && rY >= 0 && rZ >= 0 : rX + "," + rY + "," + rZ;
		this.instance = instance;

		double minY = center.y() - rY;
		double maxY = center.y() + rY;
		if (instance != null) {
			DimensionType type = instance.getCachedDimensionType();
			double dimMinY = type.minY();
			double dimMaxY = type.minY() + type.height() - 1; // correct even if minY != 0
			minY = Math.max(minY, dimMinY);
			maxY = Math.min(maxY, dimMaxY);
		}

		this.lowerBound = new Vec(center.x() - rX, minY, center.z() - rZ);
		this.upperBound = new Vec(center.x() + rX, maxY, center.z() + rZ);
	}

	public AABB(@NotNull Instance instance, @NotNull Vec v1, @NotNull Vec v2) {
		this.instance = instance;
		this.lowerBound = new Vec(
			Math.min(v1.x(), v2.x()),
			Math.min(v1.y(), v2.y()),
			Math.min(v1.z(), v2.z())
		);
		this.upperBound = new Vec(
			Math.max(v1.x(), v2.x()),
			Math.max(v1.y(), v2.y()),
			Math.max(v1.z(), v2.z())
		);
	}

	public AABB(@NotNull Chunk chunk) {
		this.instance = chunk.getInstance();

		DimensionType type = instance.getCachedDimensionType();
		int minY = type.minY();
		int maxY = type.minY() + type.height() - 1; // correct even if minY != 0

		int chunkX = chunk.getChunkX();
		int chunkZ = chunk.getChunkZ();

		this.lowerBound = new Vec((chunkX << 4), minY, (chunkZ << 4));
		this.upperBound = new Vec((chunkX << 4) + 15, maxY, (chunkZ << 4) + 15);
	}

	public boolean contains(@NotNull Point p) {
		// inclusive bounds (with EPS) to avoid edge/boundary weirdness
		return p.x() >= lowerBound.x() - Skript.EPSILON && p.x() <= upperBound.x() + Skript.EPSILON
			&& p.y() >= lowerBound.y() - Skript.EPSILON && p.y() <= upperBound.y() + Skript.EPSILON
			&& p.z() >= lowerBound.z() - Skript.EPSILON && p.z() <= upperBound.z() + Skript.EPSILON;
	}

	public boolean contains(@NotNull BlockVec b) {
		// same logic as Bukkit version: block is inside if both corners are inside
		Vec p1 = new Vec(b.x(), b.y(), b.z());
		Vec p2 = new Vec(b.x() + 1, b.y() + 1, b.z() + 1);
		return contains(p1) && contains(p2);
	}

	public Vec getDimensions() {
		return new Vec(
			upperBound.x() - lowerBound.x(),
			upperBound.y() - lowerBound.y(),
			upperBound.z() - lowerBound.z()
		);
	}

	public @Nullable Instance getInstance() {
		return instance;
	}

	/**
	 * Returns an iterator over all block positions in this AABB (inclusive).
	 *
	 * Note: this does NOT check chunk loaded state (matches old behavior of "getBlockAt").
	 * Your caller already filters chunk loaded in ExprBlocks for the line case; for AABB you
	 * may want to add loaded checks if needed.
	 */
	@Override
	public @NonNull Iterator<BlockVec> iterator() {
		return new Iterator<>() {
			private final int minX = (int) Math2.ceil(lowerBound.x());
			private final int minY = (int) Math2.ceil(lowerBound.y());
			private final int minZ = (int) Math2.ceil(lowerBound.z());
			private final int maxX = (int) Math2.floor(upperBound.x());
			private final int maxY = (int) Math2.floor(upperBound.y());
			private final int maxZ = (int) Math2.floor(upperBound.z());

			private int x = minX - 1; // next() increments immediately
			private int y = minY;
			private int z = minZ;

			@Override
			public boolean hasNext() {
				return y <= maxY && (x != maxX || y != maxY || z != maxZ);
			}

			@Override
			public BlockVec next() {
				if (!hasNext()) throw new NoSuchElementException();
				x++;
				if (x > maxX) {
					x = minX;
					z++;
					if (z > maxZ) {
						z = minZ;
						y++;
					}
				}
				if (y > maxY) throw new NoSuchElementException();
				return new BlockVec(x, y, z);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + lowerBound.hashCode();
		result = 31 * result + upperBound.hashCode();
		result = 31 * result + Objects.hashCode(instance);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof AABB other)) return false;
		if (!lowerBound.equals(other.lowerBound)) return false;
		if (!upperBound.equals(other.upperBound)) return false;
		return Objects.equals(instance, other.instance);
	}
}