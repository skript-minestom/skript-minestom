package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Name("Chunk")
@Description("Returns the <a href='#chunk'>chunk</a> of a block, location or entity is in, or a list of the loaded chunks of a world.")
@Examples("""
	add the chunk at the player to {protected chunks::*}
	set {_chunks::*} to the loaded chunks of the player's world""")
@Since("2.0, 2.8.0 (loaded chunks)")
public class ExprChunk extends SimpleExpression<Chunk> {

	static {
		Skript.registerExpression(ExprChunk.class, Chunk.class, ExpressionType.COMBINED,
			"[(all [[of] the]|the)] chunk[s] (of|%-directions%) %points% [in [(world|instance)[s]] %instances%]",
			"%points%'[s] chunk[s] [in [(world|instance)[s]] %instances%]",
			"[(all [[of] the]|the)] loaded chunks (of|in) %instances%"
		);
	}

	private int pattern;
	private Expression<Point> points;
	private Expression<Instance> instances;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		pattern = matchedPattern;
		if (pattern == 0) {
			points = (Expression<Point>) exprs[1];
			if (exprs[0] != null) {
				points = Direction.combine((Expression<? extends Direction>) exprs[0], points);
			}
			instances = (Expression<Instance>) exprs[2];
		} else if (pattern == 1) {
			points = (Expression<Point>) exprs[0];
			instances = (Expression<Instance>) exprs[1];
		} else {
			instances = ((Expression<Instance>) exprs[0]);
		}
		return true;
	}

	@Override
	protected Chunk[] get(Event event) {
		Instance[] instances = this.instances.getArray(event);
		if (pattern != 2) {
			return points.stream(event)
				.map(point -> {
					for (Instance instance : instances) {
						return instance.getChunkAt(point);
					}
					return null;
				})
				.toArray(Chunk[]::new);
		}
		return Arrays.stream(instances)
			.flatMap(world -> world.getChunks().stream())
			.toArray(Chunk[]::new);
	}

	@Override
	public boolean isSingle() {
		if (pattern == 2)
			return false;
		return points.isSingle();
	}

	@Override
	public Class<? extends Chunk> getReturnType() {
		return Chunk.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (pattern == 2)
			return "loaded chunks of " + instances.toString(event, debug);
		return "chunk at " + points.toString(event, debug);
	}

}
