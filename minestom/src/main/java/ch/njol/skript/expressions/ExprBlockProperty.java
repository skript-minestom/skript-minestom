package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;


@Name("Block Property")
@Description("A block state property value of a block.")
@Examples("set {_facing} to block \"facing\" property of block at player")
public class ExprBlockProperty extends PropertyExpression<Block, String> {

	static {
		Skript.registerExpression(ExprBlockProperty.class, String.class, ExpressionType.PROPERTY,
			"block %string% property of %blocks%",
			"%blocks%'[s] block %string% property");
	}

	private Expression<String> property;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		int propertyIndex = 0;
		int blockIndex = 1;
		if (matchedPattern == 1) {
			propertyIndex = 1;
			blockIndex = 0;
		}
		setExpr((Expression<? extends Block>) expressions[blockIndex]);
		property = (Expression<String>) expressions[propertyIndex];
		return true;
	}

	@Override
	protected String[] get(Event event, Block[] source) {
		String property = this.property.getSingle(event);
		if (property == null) return new String[0];
		List<String> properties = new ArrayList<>();
		for (Block block : source) {
			String p = block.getProperty(property);
			if (p != null) properties.add(p);
		}
		return properties.toArray(new String[0]);
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "block property " + property.toString(event, debug) + " of " + getExpr().toString(event, debug);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
