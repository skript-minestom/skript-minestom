package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.ConvertedExpression;
import ch.njol.skript.util.Enchantment;
import ch.njol.skript.util.InventoryType;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.util.ArrayList;
import java.util.List;

@Name("Type of")
@Description("""
	Type of a block, item, entity, inventory, potion effect or enchantment type.
	Types of items, blocks and block datas are item types similar to them but have amounts
	of one, no display names and, on Minecraft 1.13 and newer versions, are undamaged.
	Types of entities and inventories are entity types and inventory types known to Skript.
	Types of potion effects are potion effect types.
	Types of enchantment types are enchantments.""")
@Example("""
	on rightclick on an entity:
		message "This is a %type of clicked entity%!"
	""")
@Since("1.4, 2.5.2 (potion effect), 2.7 (block datas), 2.10 (enchantment type)")
public class ExprTypeOf extends SimplePropertyExpression<Object, Object> {

	static {
		register(ExprTypeOf.class, Object.class, "type",
			"entities/items/inventories/blocks/enchantments");
	}

	private Class<?>[] returnTypes;
	private Class<?> superReturnType;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		Expression<?> expression = expressions[0];
		List<Class<?>> returnTypes = new ArrayList<>();
		if (expression.canReturn(Entity.class))
			returnTypes.add(EntityType.class);
		if (expression.canReturn(Item.class) || expression.canReturn(Block.class))
			returnTypes.add(Item.class);
		if (expression.canReturn(AbstractInventory.class))
			returnTypes.add(InventoryType.class);
		if (expression.canReturn(Enchantment.class))
			returnTypes.add(Enchantment.class);
		this.returnTypes = returnTypes.toArray(new Class<?>[0]);
		this.superReturnType = Utils.getSuperType(this.returnTypes);

		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	@Nullable
	public Object convert(Object object) {
		return switch (object) {
			case Entity entity -> entity.getEntityType();
			case Item item -> new Item(ItemStack.of(item.getItem().material()));
			case AbstractInventory inventory -> inventory instanceof PlayerInventory ? InventoryType.PLAYER : InventoryType.of(((Inventory) inventory).getInventoryType());
			case Block block -> {
				Material material = block.registry().material();
				if (block == Block.AIR) material = Material.AIR; // edge case in minestom rn
				yield material == null ? null : new Item(ItemStack.of(material));
			}
			case Enchantment enchantment -> new Enchantment(enchantment.enchantment(), -1);
			default -> null;
		};
	}

	@Override
	public Class<?> getReturnType() {
		return superReturnType;
	}

	@Override
	public Class<?>[] possibleReturnTypes() {
		return returnTypes;
	}

	@SafeVarargs
	@Override
	@Nullable
	protected final <R> ConvertedExpression<Object, ? extends R> getConvertedExpr(final Class<R>... to) {
		if (!Converters.converterExists(Entity.class, to) && !Converters.converterExists(Item.class, to))
			return null;
		return super.getConvertedExpr(to);
	}

	@Override
	protected String getPropertyName() {
		return "type";
	}

}
