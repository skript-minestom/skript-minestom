package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Item;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Item Amount")
@Description("The amount of an <a href='classes.html#itemstack'>item stack</a>.")
@Examples("""
	send "You have got %item amount of player's tool% %player's tool% in your hand!" to player""")
@Since("2.2-dev24")
public class ExprItemAmount extends SimplePropertyExpression<Item, Long> {

	static {
		register(ExprItemAmount.class, Long.class, "item[[ ]stack] (amount|size|number)", "items");
	}

	@Override
	public Long convert(final Item item) {
		return (long) item.getItem().amount();
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		switch (mode) {
			case SET:
			case ADD:
			case RESET:
			case DELETE:
			case REMOVE:
				return CollectionUtils.array(Long.class);
		}
		return null;
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		int amount = delta != null ? ((Number) delta[0]).intValue() : 0;
		switch (mode) {
			case REMOVE:
				amount = -amount;
				// fall through
			case ADD:
				for (Item item : getExpr().getArray(event)) {
					int finalAmount = amount;
					item.modify(i -> i.withAmount(i.amount() + finalAmount), true);
				}
				break;
			case RESET:
			case DELETE:
				amount = 1;
				// fall through
			case SET:
				for (Item item : getExpr().getArray(event)) {
					int finalAmount1 = amount;
					item.modify(i -> i.withAmount(finalAmount1), true);
				}
				break;
		}
	}

	@Override
	public Class<? extends Long> getReturnType() {
		return Long.class;
	}

	@Override
	protected String getPropertyName() {
		return "item[[ ]stack] (amount|size|number)";
	}
}
