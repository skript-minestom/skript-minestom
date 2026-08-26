package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Item;
import org.jspecify.annotations.Nullable;

@Name("Item Material Id")
@Description("Allows you to get the material id (namespace) of an item.")
@Examples("send material id of wooden shovel to player # prints minecraft:wooden_shovel")
public class ExprMaterialId extends SimplePropertyExpression<Item, String> {

	static {
		register(ExprMaterialId.class, String.class, "material id", "items");
	}

	@Override
	public @Nullable String convert(Item from) {
		return from.getItem().material().key().asString();
	}

	@Override
	protected String getPropertyName() {
		return "material id";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
