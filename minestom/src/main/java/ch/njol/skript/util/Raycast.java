package ch.njol.skript.util;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public record Raycast(Kind kind, Point origin, Vec direction, double range, Instance instance,
					  @Nullable Point hitPosition, @Nullable BlockVec hitBlockPosition, @Nullable Block hitBlock,
					  @Nullable Entity hitEntity, @Nullable Vec hitNormal, double distance) {

	public enum Kind {
		BLOCK, ENTITY, ANY
	}

	public static final double DEFAULT_RANGE = 50;

	private static final double EPSILON = 1.0E-9;

	public boolean hitAnything() {
		return hitBlock != null || hitEntity != null;
	}

	public static Raycast cast(Kind kind, Instance instance, Point origin, Vec direction, double range,
							   Collection<Entity> ignored, boolean stopAtAnyBlock) {
		Vec normalized = direction.normalize();
		if (Double.isNaN(normalized.x()) || Double.isNaN(normalized.y()) || Double.isNaN(normalized.z()))
			return miss(kind, instance, origin, direction, range);

		Hit block = kind == Kind.ENTITY ? null : castBlock(instance, origin, normalized, range, stopAtAnyBlock);
		Hit entity = kind == Kind.BLOCK ? null : castEntity(instance, origin, normalized, range, ignored);

		Hit hit = block;
		if (entity != null && (block == null || entity.distance() < block.distance())) hit = entity;
		if (hit == null) return miss(kind, instance, origin, normalized, range);

		return new Raycast(kind, origin, normalized, range, instance,
			origin.add(normalized.mul(hit.distance())), hit.blockPosition(), hit.block(), hit.entity(),
			hit.normal(), hit.distance());
	}

	private static Raycast miss(Kind kind, Instance instance, Point origin, Vec direction, double range) {
		return new Raycast(kind, origin, direction, range, instance, null, null, null, null, null, range);
	}

	private static @Nullable Hit castBlock(Instance instance, Point origin, Vec direction, double range,
										   boolean stopAtAnyBlock) {
		BlockLineIterator iterator = new BlockLineIterator(origin, direction, range);
		while (iterator.hasNext()) {
			BlockVec position = iterator.next();
			if (instance.getChunkAt(position) == null) return null;
			Block block = instance.getBlock(position);
			if (block.isAir()) continue;

			List<BoundingBox> boxes = stopAtAnyBlock
				? List.of(new BoundingBox(1, 1, 1, Vec.ZERO))
				: block.collisionShape().boundingBoxes();
			if (boxes.isEmpty()) continue;

			Hit closest = null;
			for (BoundingBox box : boxes) {
				Hit hit = intersect(origin, direction, position.add(box.relativeStart()), position.add(box.relativeEnd()));
				if (hit == null || hit.distance() > range) continue;
				if (closest == null || hit.distance() < closest.distance()) closest = hit;
			}
			if (closest != null) return closest.withBlock(position, block);
		}
		return null;
	}

	private static @Nullable Hit castEntity(Instance instance, Point origin, Vec direction, double range,
											Collection<Entity> ignored) {
		Set<Entity> skipped = Set.copyOf(ignored);
		Hit closest = null;
		for (Entity entity : instance.getNearbyEntities(origin, range)) {
			if (skipped.contains(entity)) continue;
			BoundingBox box = entity.getBoundingBox();
			Point position = entity.getPosition();
			Hit hit = intersect(origin, direction, position.add(box.relativeStart()), position.add(box.relativeEnd()));
			if (hit == null || hit.distance() > range) continue;
			if (closest == null || hit.distance() < closest.distance()) closest = hit.withEntity(entity);
		}
		return closest;
	}

	private static @Nullable Hit intersect(Point origin, Vec direction, Point min, Point max) {
		double near = Double.NEGATIVE_INFINITY;
		double far = Double.POSITIVE_INFINITY;
		int axis = -1;
		double sign = 0;

		for (int current = 0; current < 3; current++) {
			double start = component(origin, current);
			double step = component(direction, current);
			double low = component(min, current);
			double high = component(max, current);

			if (Math.abs(step) < EPSILON) {
				if (start < low || start > high) return null;
				continue;
			}

			double entry = (low - start) / step;
			double exit = (high - start) / step;
			if (entry > exit) {
				double swap = entry;
				entry = exit;
				exit = swap;
			}
			if (entry > near) {
				near = entry;
				axis = current;
				sign = step > 0 ? -1 : 1;
			}
			if (exit < far) far = exit;
			if (near > far) return null;
		}

		if (far < 0) return null;
		if (near < 0) return new Hit(0, null, null, null, null);
		return new Hit(near, normalOf(axis, sign), null, null, null);
	}

	private static @Nullable Vec normalOf(int axis, double sign) {
		return switch (axis) {
			case 0 -> new Vec(sign, 0, 0);
			case 1 -> new Vec(0, sign, 0);
			case 2 -> new Vec(0, 0, sign);
			default -> null;
		};
	}

	private static double component(Point point, int axis) {
		return switch (axis) {
			case 0 -> point.x();
			case 1 -> point.y();
			default -> point.z();
		};
	}

	private record Hit(double distance, @Nullable Vec normal, @Nullable BlockVec blockPosition,
					   @Nullable Block block, @Nullable Entity entity) {

		Hit withBlock(BlockVec blockPosition, Block block) {
			return new Hit(distance, normal, blockPosition, block, null);
		}

		Hit withEntity(Entity entity) {
			return new Hit(distance, normal, null, null, entity);
		}

	}

}
