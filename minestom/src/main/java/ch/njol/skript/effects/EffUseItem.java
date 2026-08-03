package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.item.PlayerFinishItemUseEvent;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Consumable;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Use Item")
@Description("Makes living entities begin, stop, or complete using an item in their hand. Dispatches the corresponding Minestom item use events for players.")
@Examples("""
	make player begin using item
	make player stop using active item
	make player complete using active item""")
public class EffUseItem extends Effect {

	static {
		Skript.registerEffect(EffUseItem.class,
			"make %livingentities% (begin|start) using item [in %-equipmentslot%]",
			"make %livingentities% (stop|cancel) using [active] item",
			"make %livingentities% complete using [active] item");
	}

	private Expression<LivingEntity> entities;
	@Nullable
	private Expression<EquipmentSlot> slot;

	private int pattern;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		entities = (Expression<LivingEntity>) expressions[0];
		if (matchedPattern == 0) slot = (Expression<EquipmentSlot>) expressions[1];
		pattern = matchedPattern;
		return true;
	}

	@Override
	protected void execute(Event event) {
		EquipmentSlot slot = this.slot == null ? null : this.slot.getSingle(event);
		if (slot != null && !slot.isHand()) return;
		for (LivingEntity entity : entities.getArray(event)) {
			LivingEntityMeta meta = entity.getLivingEntityMeta();
			if (meta == null) continue;
			ItemEvent e;
			if (pattern == 0) {
				if (slot != null) meta.setActiveHand(slot == EquipmentSlot.MAIN_HAND ? PlayerHand.MAIN : PlayerHand.OFF);
				if (meta.isHandActive()) continue;
				meta.setHandActive(true);
				if (!(entity instanceof Player player)) continue;
				PlayerHand activeHand = meta.getActiveHand();
				ItemStack itemInHand = player.getItemInHand(activeHand);
				Consumable consumable = itemInHand.get(DataComponents.CONSUMABLE);
				Consumable consumableBackup = itemInHand.material().prototype().get(DataComponents.CONSUMABLE);
				ItemAnimation animation;
				long useDuration;
				if (consumable != null) {
					animation = consumable.animation();
					useDuration = consumable.consumeTicks();
				} else if (consumableBackup != null) {
					animation = consumableBackup.animation();
					useDuration = consumableBackup.consumeTicks();
				} else {
					animation = ItemAnimation.NONE;
					useDuration = 0;
				}
				e = new PlayerBeginItemUseEvent(player, activeHand, itemInHand, animation, useDuration);
			} else {
				if (!meta.isHandActive()) continue;
				PlayerHand activeHand = meta.getActiveHand();
				meta.setHandActive(false);
				if (!(entity instanceof Player player)) continue;
				ItemStack itemInHand = player.getItemInHand(activeHand);
				long currentItemUseTime = player.getCurrentItemUseTime();
				if (pattern == 1) e = new PlayerCancelItemUseEvent(player, activeHand, itemInHand, currentItemUseTime);
				else e = new PlayerFinishItemUseEvent(player, activeHand, itemInHand, currentItemUseTime);
			}
			EventDispatcher.call(e);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String extra = switch (pattern) {
			case 0 -> "begin using item" + (slot == null ? "" : slot.toString(event, debug));
			case 1 -> "stop using item";
			default -> "complete using item";
		};
		return "make " + entities.toString(event, debug) + " " + extra;
	}

}
