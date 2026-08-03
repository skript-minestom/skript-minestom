package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Item;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;

@Name("Name")
@Description("The name of a player, entity, or item.")
@Examples("set name of {_entity} to \"Custom Name\"")
public class ExprName extends SimplePropertyExpression<Object, ComponentWrapper> {

	private static final Component PLAYER_INVENTORY_TITLE = Component.text("player inventory");

	static {
		register(ExprName.class, ComponentWrapper.class, "[custom[ ]]name", "entities/inventories/items");
	}

	@Override
	public @Nullable ComponentWrapper convert(Object from) {
		if (from instanceof Entity entity) {
			Component component = entity.get(DataComponents.CUSTOM_NAME);
			if (entity instanceof Player player) component = Component.text(player.getUsername());
			return toWrapper(component);
		}
		else if (from instanceof AbstractInventory abstractInventory) {
			if (abstractInventory instanceof Inventory inventory) return toWrapper(inventory.getTitle());
			else return toWrapper(PLAYER_INVENTORY_TITLE);
		}
		ItemStack item = ((Item) from).getItem();
		// todo perhaps provide the visual name with item coloring (think enchanted golden apple and steak (material is cooked_beef))
		return toWrapper(item.get(DataComponents.CUSTOM_NAME));
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		Class<?> returnType = getExpr().getReturnType();
		if (Player.class.isAssignableFrom(returnType)) return null;
		if (PlayerInventory.class.isAssignableFrom(returnType)) return null;
		return switch (mode) {
			case RESET, DELETE, SET -> CollectionUtils.array(ComponentWrapper.class);
			default -> null;
		};
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		ComponentWrapper nameWrapper = delta == null ? null : (ComponentWrapper) delta[0];
		Component name = nameWrapper == null ? null : nameWrapper.getComponent();
		for (Object o : getExpr().getArray(event)) {
			switch (o) {
				case Item item -> item.modify(i -> i.withCustomName(name), true);
				case Inventory nameableInventory -> nameableInventory.setTitle(name == null ? Component.empty() : name);
				case Entity e -> e.set(DataComponents.CUSTOM_NAME, name);
				default -> {}
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "name";
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

}
