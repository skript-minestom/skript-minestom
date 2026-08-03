package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Item;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.block.Block;
import org.jspecify.annotations.Nullable;


@Name("Translation Key")
@Description("The translation key of an entity type, item, attribute, block, or sound.")
@Examples("""
	broadcast "%translation key of zombie%\"""")
public class ExprTranslationKey extends SimplePropertyExpression<Object, String> {

	static {
		register(ExprTranslationKey.class, String.class, "translation key",
			"entitytypes/items/attributetypes/blocks");
	}

	@Override
	public @Nullable String convert(Object from) {
		return switch (from) {
			case EntityType type -> type.registry().translationKey();
			case Item item -> item.getItem().material().registry().translationKey();
			case Attribute attribute -> attribute.registry().translationKey();
			case Block block -> block.registry().translationKey();
			default -> null;
		};
	}

	@Override
	protected String getPropertyName() {
		return "translation key";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
