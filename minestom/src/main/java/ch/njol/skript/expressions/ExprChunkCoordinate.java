package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.instance.Chunk;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;


@Name("Chunk Coordinate")
@Description("The x or z coordinate of a chunk.")
@Examples("broadcast \"Chunk X: %chunk x of chunk at player%\"")
public class ExprChunkCoordinate extends PropertyExpression<Chunk, Integer> {

	static {
		register(ExprChunkCoordinate.class, Integer.class, "chunk (:x|z)", "chunks");
	}

	private boolean x;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Chunk>) expressions[0]);
		x = parseResult.hasTag("x");
		return true;
	}

	@Override
	protected Integer[] get(Event event, Chunk[] source) {
		Integer[] coords = new Integer[source.length];
		for (int i = 0; i < source.length; i++) {
			Chunk chunk = source[i];
			coords[i] = x ? chunk.getChunkX() : chunk.getChunkZ();
		}
		return coords;
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "chunk " + (x ? "x" : "z") + " of " + getExpr().toString(event, debug);
	}

}
