package ch.njol.skript.util;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;

public enum BlockFace {

	UP(net.minestom.server.instance.block.BlockFace.TOP.toDirection()),
	DOWN(net.minestom.server.instance.block.BlockFace.BOTTOM.toDirection()),
	NORTH(net.minestom.server.instance.block.BlockFace.NORTH.toDirection()),
	SOUTH(net.minestom.server.instance.block.BlockFace.SOUTH.toDirection()),
	EAST(net.minestom.server.instance.block.BlockFace.EAST.toDirection()),
	WEST(net.minestom.server.instance.block.BlockFace.WEST.toDirection()),
	NORTH_EAST(new Vec(1, 0, -1)),
	NORTH_WEST(new Vec(-1, 0, -1)),
	SOUTH_EAST(new Vec(1, 0, 1)),
	SOUTH_WEST(new Vec(-1, 0, 1));

	private final Vec direction;

	BlockFace(Vec vec) {
		direction = vec;
	}

	BlockFace(Direction direction) {
		this(direction.vec());
	}

	public double x() {
		return direction.x();
	}

	public double y() {
		return direction.y();
	}

	public double z() {
		return direction.z();
	}

	public Vec toDirection() {
		return direction;
	}

	public BlockFace getOppositeFace() {
		return switch (this) {
			case DOWN -> UP;
			case UP -> DOWN;
			case NORTH -> SOUTH;
			case SOUTH -> NORTH;
			case WEST -> EAST;
			case EAST -> WEST;
			case NORTH_EAST -> SOUTH_WEST;
			case NORTH_WEST -> SOUTH_EAST;
			case SOUTH_EAST -> NORTH_WEST;
			case SOUTH_WEST -> NORTH_EAST;
		};
	}

	public static BlockFace from(net.minestom.server.instance.block.BlockFace blockFace) {
		Vec dir = blockFace.toDirection().vec();
		for (BlockFace face : values()) {
			if (face.direction.equals(dir)) return face;
		}
		return null;
	}

}
