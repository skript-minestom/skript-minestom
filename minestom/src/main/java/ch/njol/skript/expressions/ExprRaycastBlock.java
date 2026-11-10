package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Raycast;
import net.minestom.server.instance.block.Block;
import org.eclipse.jdt.annotation.Nullable;

@Name("Raycast Hit Block")
@Description("The block a raycast hit, or nothing if it hit an entity or nothing at all.")
@Examples("""
	set {_ray} to block raycast from player
	if hit block of {_ray} is stone:""")
public class ExprRaycastBlock extends SimplePropertyExpression<Raycast, Block> {

	static {
		register(ExprRaycastBlock.class, Block.class, "hit block", "raycasts");
	}

	@Override
	public @Nullable Block convert(Raycast from) {
		return from.hitBlock();
	}

	@Override
	public Class<? extends Block> getReturnType() {
		return Block.class;
	}

	@Override
	protected String getPropertyName() {
		return "hit block";
	}

}
