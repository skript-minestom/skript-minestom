package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.InventoryType;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Open/Close Inventory")
@Description("""
	Opens an inventory to a player. The player can then access and modify the inventory as if it was a chest that they just opened.
	Please note that currently 'show' and 'open' have the same effect, but 'show' will eventually show an unmodifiable view of the inventory in the future.""")
@Examples("open the player's inventory for the player")
@Since("2.0, 2.1.1 (closing), 2.2-Fixes-V10 (anvil), 2.4 (hopper, dropper, dispenser)")
public class EffOpenInventory extends Effect {

	static {
		Skript.registerEffect(EffOpenInventory.class,
			"close %players%'[s] inventory [view]",
			"close [the] inventory [view] (to|of|for) %players%",
			"open %inventory/inventorytype% (to|for) %players%");
	}

	private boolean open = false;
	private @Nullable Expression<?> inventoryExpr = null;

	private Expression<Player> players;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 2) {
			open = true;
			inventoryExpr = exprs[0];
			if (inventoryExpr.getReturnType() == PlayerInventory.class) {
				Skript.error("Cannot open player inventories.");
				return false;
			}
		}
		players = (Expression<Player>) exprs[exprs.length - 1];

		if (exprs[0] instanceof Literal<?> lit && lit.getSingle() instanceof InventoryType type && type == InventoryType.PLAYER) {
			Skript.error("Cannot create an inventory of type " + Classes.toString(type));
			return false;
		}

		return true;
	}

	@Override
	protected void execute(final Event event) {
		if (inventoryExpr != null) {
			Object object = inventoryExpr.getSingle(event);
			openForPlayers(event, object);
		} else {
			for (Player player : players.getArray(event)) {
				player.closeInventory();
			}
		}
	}

	private void openForPlayers(Event event, Object target) {
		if (target == null)
			return;

		Player[] targetPlayers = this.players.getArray(event);

		if (target instanceof Inventory inventory) {
			for (Player player : targetPlayers) {
				player.openInventory(inventory);
			}
		} else if (target instanceof InventoryType type) {
			if (type == InventoryType.PLAYER) return;
			Component defaultTitle = getDefaultTitle(type);
			net.minestom.server.inventory.InventoryType minestomType = type.getMinestomType();
			assert minestomType != null;
			for (Player player : targetPlayers) {
				player.openInventory(new Inventory(minestomType, defaultTitle));
			}
		}
	}

	@Override
	public String toString(final @Nullable Event event, final boolean debug) {
		if (!open) {
			return "close inventory view of " + players.toString(event, debug);
		}
		assert inventoryExpr != null;
		return "open " + inventoryExpr.toString(event, debug) + " to " + players.toString(event, debug);
	}

	public static boolean isChest(InventoryType inventoryType) {
		return switch (inventoryType) {
			case CHEST_1_ROW, CHEST_2_ROW, CHEST_3_ROW, CHEST_4_ROW, CHEST_5_ROW, CHEST_6_ROW -> true;
			default -> false;
		};
	}

	public static Component getDefaultTitle(InventoryType type) {
		String name = switch (type) {
			case CHEST_1_ROW, CHEST_2_ROW, CHEST_3_ROW, CHEST_4_ROW, CHEST_5_ROW, CHEST_6_ROW -> "Chest";
			case WINDOW_3X3 -> "Dropper";
			case CRAFTER_3X3 -> "Crafter";
			case ANVIL -> "Repair & Name";
			case BEACON, LECTERN, PLAYER -> "";
			case BLAST_FURNACE -> "Blast Furnace";
			case BREWING_STAND -> "Brewing Stand";
			case CRAFTING -> "Crafting";
			case ENCHANTMENT -> "Enchant";
			case FURNACE -> "Furnace";
			case GRINDSTONE -> "Repair & Disenchant";
			case HOPPER -> "Item Hopper";
			case LOOM -> "Loom";
			case MERCHANT -> "Merchant";
			case SHULKER_BOX -> "Shulker Box";
			case SMITHING -> "Upgrade Gear";
			case SMOKER -> "Smoker";
			case CARTOGRAPHY -> "Cartography Table";
			case STONE_CUTTER -> "Stonecutter";
		};
		return Component.text(name).color(NamedTextColor.DARK_GRAY);
	}

}
