package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;


@Name("Block With Property")
@Description("A block with a specific block state property set.")
@Examples("set {_b} to oak stairs with block \"facing\" property of \"north\"")
public class ExprBlockWithProperty extends SimpleExpression<Block> {

	static {
		Skript.registerExpression(ExprBlockWithProperty.class, Block.class, ExpressionType.COMBINED,
			"%blocks% with [block] %string% property [of] %string%");
	}

	private Expression<Block> blocks;
	private Expression<String> property;
	private Expression<String> value;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		blocks = (Expression<Block>) expressions[0];
		property = (Expression<String>) expressions[1];
		value = (Expression<String>) expressions[2];
		return true;
	}

	@Override
	protected Block @Nullable [] get(Event event) {
		String property = this.property.getSingle(event);
		String value = this.value.getSingle(event);
		Block[] blocks = this.blocks.getArray(event);
		if (property != null && value != null) {
			for (int i = 0; i < blocks.length; i++) {
				Block block = blocks[i];
				if (block.getProperty(property) == null) continue;
				try {
					blocks[i] = block.withProperty(property, value);
				} catch (IllegalArgumentException ex) {
					SkriptLogger.LOGGER.error("Invalid property ({}) or value ({}) provided whilst creating a block ({}) with a property.",
						property, value, block);
				}
			}
		}
		return blocks;
	}

	@Override
	public boolean isSingle() {
		return blocks.isSingle();
	}

	@Override
	public Class<? extends Block> getReturnType() {
		return Block.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return blocks.toString(event, debug) + " with block " + property.toString(event, debug) + " property of " + value.toString(event, debug);
	}

}
