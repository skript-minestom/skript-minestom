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
import ch.njol.skript.util.BlockSphereIterator;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.EmptyIterator;
import ch.njol.util.coll.iterator.IteratorIterable;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Peter Güttinger
 */
@Name("Block Sphere")
@Description("All blocks in a sphere around a center, mostly useful for looping.")
@Examples("set all blocks in radius 3 around player in player's instance to stone")
@Since("1.0")
public class ExprBlockSphere extends SimpleExpression<BlockVec> {
	static {
		Skript.registerExpression(ExprBlockSphere.class, BlockVec.class, ExpressionType.COMBINED,
			"[(all [[of] the]|the)] blocks in radius %number% [(of|around) %point%] [in [(world|instance)] %-instance%]",
			"[(all [[of] the]|the)] blocks around %point% in radius %number% [in [(world|instance)] %-instance%]");
	}

	@SuppressWarnings("null")
	private Expression<Number> radius;
	@SuppressWarnings("null")
	private Expression<Point> center;
	private Expression<Instance> instance;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
		radius = (Expression<Number>) exprs[matchedPattern];
		center = (Expression<Point>) exprs[1 - matchedPattern];
		instance = (Expression<Instance>) exprs[2];
		return true;
	}

	@Override
	public Iterator<BlockVec> iterator(final Event e) {
		final Point l = center.getSingle(e);
		final Number r = radius.getSingle(e);
		final Instance i = instance == null ? null :instance.getSingle(e);
		if (l == null || r == null)
			return new EmptyIterator<>();
		return new BlockSphereIterator(l, r.doubleValue(), i);
	}

	@Override
	@Nullable
	protected BlockVec[] get(final Event e) {
		final Number r = radius.getSingle(e);
		if (r == null)
			return new BlockVec[0];
		final ArrayList<BlockVec> list = new ArrayList<>((int) (1.1 * 4 / 3. * Math.PI * Math.pow(r.doubleValue(), 3)));
		for (final BlockVec b : new IteratorIterable<>(iterator(e)))
			list.add(b);
		return list.toArray(new BlockVec[0]);
	}

	@Override
	@org.eclipse.jdt.annotation.Nullable
	public Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET) return CollectionUtils.array(Block.class);
		return null;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		if (delta[0] == null) return;
		Block block = (Block) delta[0];
		Iterator<BlockVec> it = iterator(event);
		Instance instance = this.instance.getSingle(event);
		if (instance == null) return;
		AbsoluteBlockBatch batch = new AbsoluteBlockBatch();
		while (it.hasNext()) {
			batch.setBlock(it.next(), block);
		}
		batch.apply(instance, null);
	}

	@Override
	public Class<? extends BlockVec> getReturnType() {
		return BlockVec.class;
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return "the blocks in radius " + radius + " around " + center.toString(e, debug) + (instance != null ? " in instance " + instance.toString(e, debug) : "");
	}

	@Override
	public boolean isLoopOf(final String s) {
		return s.equalsIgnoreCase("block");
	}

	@Override
	public boolean isSingle() {
		return false;
	}

}
