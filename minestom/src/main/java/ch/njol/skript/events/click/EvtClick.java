package ch.njol.skript.events.click;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.*;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Item;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Map;

public class EvtClick extends SkriptEvent {

	/**
	 * Click types.
	 */
	private final static int RIGHT = 1, LEFT = 2, ANY = RIGHT | LEFT;

	/**
	 * Tracks PlayerInteractEvents to deduplicate them.
	 */
	public final static ClickEventTracker INTERACT_TRACKER = new ClickEventTracker();


	static {
		Class<? extends Event>[] eventTypes = CollectionUtils.array(PlayerBlockInteractWrapper.class, PlayerEntityInteractWrapper.class,
			PlayerUseItemWrapper.class, PlayerStartDiggingWrapper.class, PlayerHandAnimationWrapper.class);
		Skript.registerEvent("Click", EvtClick.class, eventTypes,
				  "[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] [on %-entitytypes/blocks%] [(with|using|holding) %-items%]",
				  "[(" + RIGHT + ":right|" + LEFT + ":left)(| |-)][mouse(| |-)]click[ing] (with|using|holding) %items% on %entitytypes/blocks%")
			  .description("""
			  	Called when a user clicks on a block, an entity or air with or without an item in their hand.
			  	Please note that rightclick events with an empty hand while not looking at a block are not sent to the server, so there's no way to detect them.
			  	Also note that a leftclick on an entity is an attack and thus not covered by the 'click' event, but the 'damage' event.""")
			  .examples("""
			  	on click:
			  	on rightclick holding fishing rod:
			  	on leftclick on stone or obsidian:
			  	on rightclick on creeper:
			  	on click with a sword:
			  	on click on chest[facing=north]:
			  	on click on campfire[lit=true]:""")
			  .since("1.0, 2.10 (blockdata)");
	}

	/**
	 * Only trigger when one of these is interacted with.
	 */
	private @Nullable Literal<?> type;

	/**
	 * Only trigger when the item that the player clicks with is one of these.
	 */
	private @Nullable Literal<Item> tools;

	/**
	 * Click types to trigger.
	 */
	private int click = ANY;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		click = parseResult.mark == 0 ? ANY : parseResult.mark;
		type = args[matchedPattern];
		if (type != null && !type.canReturn(Item.class) && !type.canReturn(Block.class)) {
			if (click != RIGHT) {
				Skript.error("A leftclick on an entity is an attack and thus not covered by the 'click' event. " +
					"Change this event to a rightclick to fix this warning message.");
				return false;
			}
		}
		tools = (Literal<Item>) args[1 - matchedPattern];
		return true;
	}

	@Override
	public boolean check(Event event) {
		Object o = type == null ? null : type.getSingle();
		return switch (event) {
			case PlayerBlockInteractWrapper wr -> {
				if (click == LEFT) yield false;
				PlayerBlockInteractEvent e = wr.getEvent();
				Player player = e.getPlayer();
				PlayerHand hand = e.getHand();
				if (!INTERACT_TRACKER.checkEvent(player, e, hand)) yield false;
				yield verifyEvent(o, e.getBlock(), player, hand);
			}
			case PlayerEntityInteractWrapper wr -> {
				if (click == LEFT) yield false;
				PlayerEntityInteractEvent e = wr.getEvent();
				Player player = e.getPlayer();
				PlayerHand hand = e.getHand();
				if (!INTERACT_TRACKER.checkEvent(player, e, hand)) yield false;
				yield verifyEvent(o, e.getTarget().getEntityType(), player, hand);
			}
			case PlayerUseItemWrapper wr -> {
				if (click == LEFT) yield false;
				PlayerUseItemEvent e = wr.getEvent();
				Player player = e.getPlayer();
				ItemStack itemStack = e.getItemStack();
				PlayerHand hand = e.getHand();
				if (!INTERACT_TRACKER.checkEvent(player, e, hand)) yield false;
				yield verifyEvent(o, itemStack, player, hand);
			}
			case PlayerStartDiggingWrapper wr -> {
				if (click == RIGHT) yield false;
				PlayerStartDiggingEvent e = wr.getEvent();
				Block block = e.getBlock();
				Player player = e.getPlayer();
				if (!INTERACT_TRACKER.checkEvent(player, e, PlayerHand.MAIN)) yield false;
				yield verifyEvent(o, block, player, PlayerHand.MAIN);
			}
			case PlayerHandAnimationWrapper wr -> {
				if (click == RIGHT) yield false;
				PlayerHandAnimationEvent e = wr.getEvent();
				Player player = e.getPlayer();
				PlayerHand hand = e.getHand();
				if (hand == PlayerHand.OFF) yield false;
				if (!INTERACT_TRACKER.checkEvent(player, e, hand)) yield false;
				yield verifyEvent(o, null, e.getPlayer(), hand);
			}
			default -> false;
		};
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return switch (click) {
			case LEFT -> "left";
			case RIGHT -> "right";
			default -> "";
		} + "click" + (type != null ? " on " + type.toString(event, debug) : "") +
			(tools != null ? " holding " + tools.toString(event, debug) : "");
	}

	private boolean verifyEvent(Object blockOrEntity, Object eventObject, Player player, PlayerHand playerHand) {
		boolean passesEventObject = true;
		if (blockOrEntity != null) {
			if (blockOrEntity instanceof Block block1 && eventObject instanceof Block block2) passesEventObject = verifyBlock(block1, block2);
			else passesEventObject = blockOrEntity.equals(eventObject);
		}
		return passesEventObject && verifyItem(player, playerHand);
	}

	private boolean verifyBlock(Block blockToCheck, Block eventBlock) {
		if (eventBlock.stateId() == eventBlock.defaultState().stateId()) return blockToCheck.compare(eventBlock);
		Map<String, String> eventBlockProperties = eventBlock.properties();
		Map<String, String> blockToCheckProperties = blockToCheck.properties();
		for (String s : eventBlockProperties.keySet()) {
			if (!blockToCheckProperties.containsKey(s) || !eventBlockProperties.get(s).equals(blockToCheckProperties.get(s))) return false;
		}
		return true;
	}

	private boolean verifyItem(Player player, PlayerHand hand) {
		if (tools == null) return true;
		return tools.getSingle().getItem().material().equals(player.getItemInHand(hand).material());
	}

}
