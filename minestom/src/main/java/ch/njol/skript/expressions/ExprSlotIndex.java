package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Slot;
import org.jspecify.annotations.Nullable;


@Name("Slot Index")
@Description("The index/number of a slot.")
@Examples("broadcast \"%slot index of player's tool%\"")
public class ExprSlotIndex extends SimplePropertyExpression<Slot, Integer> {

	static {
		register(ExprSlotIndex.class, Integer.class, "([slot] index|slot number[s])", "slots");
	}

	@Override
	public @Nullable Integer convert(Slot from) {
		return from.getIndex();
	}

	@Override
	protected String getPropertyName() {
		return "slot index";
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

}
