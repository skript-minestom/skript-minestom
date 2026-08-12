package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.AABB;
import ch.njol.skript.util.BlockLineIterator;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.ArrayIterator;
import com.google.common.collect.Lists;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

@Name("Blocks")
@Description("""
	Blocks relative to other blocks or between other blocks.
	Can be used to get blocks relative to other blocks or for looping.
	Blocks from/to and between will return a straight line whereas blocks within will return a cuboid.""")
@Examples("""
	loop blocks above the player:
	loop blocks between the block below the player and {_block}:
	set the blocks below the player, the victim and the {_block} to air
	set all blocks within {loc1} and {loc2} to stone
	set all blocks within chunk at player to air""")
@Since("1.0, 2.5.1 (within/cuboid/chunk)")
public class ExprBlocks extends SimpleExpression<BlockVec> {

	static {
		Skript.registerExpression(ExprBlocks.class, BlockVec.class, ExpressionType.COMBINED,
			"[(all [[of] the]|the)] blocks %direction% [%points%] [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks from %point% [on] %direction% [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks from %point% to %point% [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks between %point% and %point% [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks within %point% and %point% [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks (in|within) %chunk%");
	}

	@Nullable
	private Expression<Direction> direction;

	@Nullable
	private Expression<Point> end;
	@Nullable
	private Expression<Instance> instance;

	@Nullable
	private Expression<Chunk> chunk;
	private Expression<?> from;
	private int pattern;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parser) {
		this.pattern = matchedPattern;
		switch (matchedPattern) {
			case 0:
				direction = (Expression<Direction>) exprs[0];
				from = exprs[1];
				instance = (Expression<Instance>) exprs[2];
				break;
			case 1:
				from = exprs[0];
				direction = (Expression<Direction>) exprs[1];
				instance = (Expression<Instance>) exprs[2];
				break;
			case 2:
			case 3:
			case 4:
				from = exprs[0];
				end = (Expression<Point>) exprs[1];
				instance = (Expression<Instance>) exprs[2];
				break;
			case 5:
				chunk = (Expression<Chunk>) exprs[0];
				break;
			default:
				assert false : matchedPattern;
				return false;
		}
		return true;
	}

	@Override
	@Nullable
	protected BlockVec[] get(Event event) {
		if (this.direction != null && !from.isSingle()) {
			Instance instance = this.instance == null ? null : this.instance.getSingle(event);
			Direction direction = this.direction.getSingle(event);
			if (direction == null && pattern <= 1)
				return new BlockVec[0];
			return from.stream(event)
				.filter(Point.class::isInstance)
				.map(Point.class::cast)
				.filter(point -> {
					if (instance != null) { // ensure chunk is loaded if instance is provided
						Chunk chunk = instance.getChunkAt(point);
						return chunk != null && chunk.isLoaded();
					}
					return true;
				})
				.map(point -> {
					if (pattern <= 1) {
						assert direction != null; // we ensure direction isn't null above
						return direction.getRelative(point.asPos());
					}
					return point;
				})
				.map(Point::asBlockVec)
				.toArray(BlockVec[]::new);
		}
		Iterator<BlockVec> iterator = iterator(event);
		if (iterator == null)
			return new BlockVec[0];
		return Lists.newArrayList(iterator).toArray(new BlockVec[0]);
	}

	@Override
	@org.eclipse.jdt.annotation.Nullable
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET && instance != null) return CollectionUtils.array(Block.class); // can only change if instance isn't null
		return null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		if (delta[0] == null) return;
		Block block = (Block) delta[0];
		Iterator<BlockVec> it = iterator(event);
		Instance instance = chunk != null ? chunk.getOptionalSingle(event).map(Chunk::getInstance).orElse(null) : this.instance.getSingle(event);
		if (instance == null || it == null) return;
		AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
		while (it.hasNext()) {
			batch.setBlock(it.next(), block);
		}
		batch.apply(instance, null);
	}

	@Override
	@Nullable
	public Iterator<BlockVec> iterator(Event event) {
		try {
			if (chunk != null) {
				Chunk chunk = this.chunk.getSingle(event);
				if (chunk != null)
					return new AABB(chunk).iterator();
			} else if (direction != null) {
				if (!from.isSingle())
					return new ArrayIterator<>(get(event));
				Object object = from.getSingle(event);
				if (object == null)
					return null;
				Point point = (Point) from.getSingle(event);
				Direction direction = this.direction.getSingle(event);
				if (direction == null || point == null)
					return null;
				Vec vector = direction.getDirection(point.asPos());
				// Cannot be zero.
				if (vector.x() == 0 && vector.y() == 0 && vector.z() == 0)
					return null;
				// start block + (max - 1) == max
				int distance = 49;//SkriptConfig.maxTargetBlockDistance.value() - 1;
				if (this.direction instanceof ExprDirection) {
					Expression<Number> numberExpression = ((ExprDirection) this.direction).amount;
					if (numberExpression != null) {
						Number number = numberExpression.getSingle(event);
						if (number != null)
							distance = number.intValue();
					}
				}
				return new BlockLineIterator(point, vector, distance);
			} else {
				Point loc = (Point) from.getSingle(event);
				if (loc == null)
					return null;
				assert end != null;
				Point loc2 = end.getSingle(event);
				if (loc2 == null)
					return null;
				Instance instance = this.instance == null ? null : this.instance.getSingle(event);
				if (pattern == 4)
					return new AABB(loc, loc2, instance).iterator();
				return new BlockLineIterator(loc, loc2);
			}
		} catch (IllegalStateException e) {
			if (e.getMessage().equals("Start block missed in BlockIterator"))
				return null;
			throw e;
		}
		return null;
	}

	@Override
	public Class<? extends BlockVec> getReturnType() {
		return BlockVec.class;
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String instancePart = "";
		if (instance != null) {
			instancePart = " in instance " + instance.toString(event, debug);
		}
		if (chunk != null) {
			return "blocks within chunk " + chunk.toString(event, debug);
		} else if (pattern == 4) {
			assert end != null;
			return "blocks within " + from.toString(event, debug) + " and " + end.toString(event, debug) + instancePart;
		} else if (end != null) {
			return "blocks from " + from.toString(event, debug) + " to " + end.toString(event, debug) + instancePart;
		} else {
			assert direction != null;
			return "blocks " + direction.toString(event, debug) + " " + from.toString(event, debug) + instancePart;
		}
	}

}
