package ch.njol.skript.util;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates through blocks in a straight line from a start to end point (inclusive).
 * <p>
 * Given start and end points may not be block-centered.
 * Iterates through all blocks the line passes through in order from start to end.
 */
public final class BlockLineIterator implements Iterator<BlockVec> {

	private Vec current;
	private final Vec end;
	private final Vec centeredEnd;
	final Vec step; // package-private for tests
	private boolean finished;

	/**
	 * @param start start point
	 * @param end end point
	 */
	public BlockLineIterator(@NotNull Point start, @NotNull Point end) {
		this.current = new Vec(start.x(), start.y(), start.z());
		this.end = new Vec(end.x(), end.y(), end.z());
		this.centeredEnd = centered(this.end);
		this.step = this.end.sub(this.current).normalize();
	}

	/**
	 * @param start first block
	 * @param end last block
	 */
	public BlockLineIterator(@NotNull BlockVec start, @NotNull BlockVec end) {
		this(centerOf(start), centerOf(end));
	}

	/**
	 * @param start start point
	 * @param direction direction to travel in
	 * @param distance maximum distance to travel
	 */
	public BlockLineIterator(@NotNull Point start, @NotNull Vec direction, double distance) {
		this(start, start.add(direction.normalize().mul(distance)));
	}

	/**
	 * @param start first block
	 * @param direction direction to travel in
	 * @param distance maximum distance to travel
	 */
	public BlockLineIterator(@NotNull BlockVec start, @NotNull Vec direction, double distance) {
		this(centerOf(start), direction, distance);
	}

	@Override
	public boolean hasNext() {
		return !finished;
	}

	@Override
	public BlockVec next() {
		if (!hasNext()) throw new NoSuchElementException("Reached the final block destination");
		// sanity check (is the current->end vector pointing away from step)
		if (end.sub(current).dot(step) < -1) throw new NoSuchElementException("Overshot the final block!");
		// get block and check end
		Vec center = centered(current);
		BlockVec block = center.asBlockVec();
		if (center.equals(centeredEnd)) finished = true;
		// calculate next position
		double t = stepsToNextFace(current, step, center) + Math.ulp(1);
		current = current.add(step.mul(t));
		return block;
	}

	/**
	 * Calculates the number of steps to the next closest block face this ray, defined by start and step, will encounter.
	 * Block faces are determined by the center vector, which is interpreted as the center of the block.
	 *
	 * @param start  the current location of the ray to check
	 * @param step   the direction of the ray
	 * @param center the center location of the block the ray is currently within
	 * @return a scalar floating point number representing the number of times step must be added to start in order
	 * to arrive at the closest block face
	 */
	static double stepsToNextFace(@NotNull Vec start, @NotNull Vec step, @NotNull Vec center) {
		// signum(step) * 0.5 + center - start, then component-wise / step (JOML-equivalent)
		double nx = (center.x() + 0.5 * Math.signum(step.x()) - start.x()) / step.x();
		double ny = (center.y() + 0.5 * Math.signum(step.y()) - start.y()) / step.y();
		double nz = (center.z() + 0.5 * Math.signum(step.z()) - start.z()) / step.z();
		// get min component, ignoring NaN
		if (Double.isNaN(nx)) nx = Double.POSITIVE_INFINITY;
		if (Double.isNaN(ny)) ny = Double.POSITIVE_INFINITY;
		if (Double.isNaN(nz)) nz = Double.POSITIVE_INFINITY;
		return Math.min(nx, Math.min(ny, nz));
	}

	/**
	 * Creates vector at the center of a block at the coordinates provided by {@code vector}.
	 *
	 * @param vector point
	 * @return coordinates at the center of a block at given point
	 */
	@Contract("_ -> new")
	private static @NotNull Vec centered(@NotNull Vec vector) {
		return new Vec(Math.floor(vector.x()) + 0.5, Math.floor(vector.y()) + 0.5, Math.floor(vector.z()) + 0.5);
	}

	private static @NotNull Point centerOf(@NotNull BlockVec b) {
		return new Vec(b.blockX() + 0.5, b.blockY() + 0.5, b.blockZ() + 0.5);
	}

}
