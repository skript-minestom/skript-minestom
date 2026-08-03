package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.luckperms.*;
import ch.njol.skript.events.wrapper.*;
import ch.njol.skript.lang.util.SimpleEvent;

public class SimpleEvents {

	static {
		Skript.registerEvent("Player Connect", SimpleEvent.class, CustomConnectWrapper.class, "[player] connect[ing]")
			.description("Called when a player is connecting to the server.")
			.examples("on player connect:");
		Skript.registerEvent("Player Leave", SimpleEvent.class, PlayerDisconnectWrapper.class, "[player] (quit[ting]|disconnect[ing]|log[ ]out|logging out|leav(e|ing))")
			.description("Called when a player leaves the server.")
			.examples("on player quit:");
		Skript.registerEvent("Player Chat", SimpleEvent.class, PlayerChatWrapper.class, "[player] chat")
			.description("Called when a player chats.")
			.examples("on chat:");
		Skript.registerEvent("Swap Hand Item", SimpleEvent.class, PlayerSwapItemWrapper.class, "swap[ping of] [(hand|held)] item[s]")
			.description("Called when a player swaps items in their hands.")
			.examples("on swap hand item:");
		Skript.registerEvent("Inventory Click", SimpleEvent.class, InventoryPreClickWrapper.class, "inventory click")
			.description("Called when a player clicks while in an inventory.")
			.examples("on inventory click:");
		Skript.registerEvent("Inventory Open", SimpleEvent.class, InventoryOpenWrapper.class, "inventory open")
			.description("Called when a player opens an inventory.")
			.examples("on inventory open:");
		Skript.registerEvent("Inventory Close", SimpleEvent.class, InventoryCloseWrapper.class, "inventory close")
			.description("Called when a player closes an inventory.")
			.examples("on inventory close:");
		Skript.registerEvent("Server List Ping", SimpleEvent.class, ServerListPingWrapper.class, "server[ ]list ping")
			.description("Called when the client pings this server from their server list.")
			.examples("on server list ping:");
		Skript.registerEvent("Player Load", SimpleEvent.class, PlayerLoadedWrapper.class, "player load[ed]")
			.description("Called when the client says it has finished loading into the world.")
			.examples("on player load");
		Skript.registerEvent("Player Command", SimpleEvent.class, PlayerCommandWrapper.class, "[player] [execute|run] command")
			.description("Called when a player attempts to run a command.")
			.examples("on command:");
		Skript.registerEvent("Tool/Held Slot Change", SimpleEvent.class, PlayerChangeHeldSlotWrapper.class, "[player['s]] (tool|item held|held item) chang(e|ing)")
			.description("Called when a player changes their held item slot.")
			.examples("""
				on held item change:
					broadcast "%event-slot%" # broadcasts the currently held slot
					broadcast "%future event-slot%" # broadcasts the held slot they're changing to""");
		Skript.registerEvent("Player GameMode Change", SimpleEvent.class, PlayerGameModeChangeWrapper.class, "game[ ]mode change")
			.description("Called when a player's gamemode is about to be changed.")
			.examples("on gamemode change:");
		Skript.registerEvent("Player GameMode Request", SimpleEvent.class, PlayerGameModeRequestWrapper.class, "game[ ]mode [change] request")
			.description("Called when a player requests to change their gamemode (F3 + F4 Menu).")
			.examples("""
				on gamemode request:
					player's permission level >= 2
					set player's gamemode to event-gamemode""");
		Skript.registerEvent("Stab Attack", SimpleEvent.class, PlayerStabWrapper.class, "[player] stab[bing] [attack]")
			.description("Called when a player attempts to use a piercing item.")
			.examples("on stab attack:");
		Skript.registerEvent("Spectate", SimpleEvent.class, PlayerSpectateWrapper.class, "[player] [start] spectat(e|ing)")
			.description("Called when a player attempts to start spectating an entity.")
			.examples("""
				on spectate:
					broadcast "%event-target%\"""");
		Skript.registerEvent("Player Respawn", SimpleEvent.class, PlayerRespawnWrapper.class, "[player] respawn")
			.description("Called when a player is about to respawn.")
			.examples("""
				on respawn:
					set event-respawn-point to position(0, 42, 0)""");
		Skript.registerEvent("Player Pick Entity", SimpleEvent.class, PlayerPickEntityWrapper.class, "[player] pick entity")
			.description("Called when a player middle clicks on an entity.")
			.examples("""
				on player pick entity:
					broadcast "%event-target%"
					broadcast "%event-includes-data%" # whether data should be included from the picked entity (ctrl + middle click)""");
		Skript.registerEvent("Player Pick Block", SimpleEvent.class, PlayerPickBlockWrapper.class, "[player] pick block")
			.description("Called when a player middle clicks on a block.")
			.examples("""
				on player pick block:
					broadcast "%event-includes-data%" # whether data should be included from the picked block (ctrl + middle click)""");
		Skript.registerEvent("Entity Equip", SimpleEvent.class, EntityEquipWrapper.class, "[entity] equip")
			.description("Called when an equipment slot changes on an entity.")
			.examples("on entity equip:");
		Skript.registerEvent("Player Move", SimpleEvent.class, PlayerMoveWrapper.class, "[player] move")
			.description("Called when a player attempts to move")
			.examples("on player move:");
		Skript.registerEvent("Cancel Item Use", SimpleEvent.class, PlayerCancelItemUseWrapper.class, "[player] cancel item us(e|age)")
			.description("Called when a player cancels their item usage before it finishes.")
			.examples("on player cancel item use:");
		Skript.registerEvent("Begin Item Use", SimpleEvent.class, PlayerBeginItemUseWrapper.class, "[player] begin item us(e|age)")
			.description("Called when a player begins to use (consume) their item.")
			.examples("on player begin item use:");
		Skript.registerEvent("Finish Item Use", SimpleEvent.class, PlayerFinishItemUseWrapper.class, "[player] finish item us(e|age)")
			.description("Called when a player completes their item usage.")
			.examples("on player finish item use:");
		Skript.registerEvent("Bundle Item Select", SimpleEvent.class, InventoryBundleItemSelectWrapper.class, "bundle item select")
			.description("Called when a player scrolls on an item to select a bundle item. Also called when the player stops hovering over the item.")
			.examples("on bundle item select:");
		Skript.registerEvent("Effect Command", SimpleEvent.class, EffectCommandEvent.class, "effect command")
			.description("Called when a player runs an effect command.")
			.examples("on effect command:");
		Skript.registerEvent("Unknown Command", SimpleEvent.class, UnknownCommandEvent.class, "unknown command [execution]")
			.description("Called when a sender executes an unknown/unregistered command.")
			.examples("on unknown command:");

		// LuckPerms
		Skript.registerEvent("Group Add", SimpleEvent.class, GroupAddEvent.class, "group add")
			.description("Called when a player receives a luckperms group.")
			.examples("on group add:");
		Skript.registerEvent("Permission Add", SimpleEvent.class, PermissionAddEvent.class, "permission add")
			.description("Called when a player receives a luckperms permission.")
			.examples("on permission add:");
		Skript.registerEvent("Prefix Add", SimpleEvent.class, PrefixAddEvent.class, "prefix add")
			.description("Called when a player receives a luckperms prefix.")
			.examples("on prefix add:");
		Skript.registerEvent("Suffix Add", SimpleEvent.class, SuffixAddEvent.class, "suffix add")
			.description("Called when a player receives a luckperms suffix.")
			.examples("on suffix add:");
		Skript.registerEvent("Group Remove", SimpleEvent.class, GroupRemoveEvent.class, "group remove")
			.description("Called when a player loses a luckperms group.")
			.examples("on group remove:");
		Skript.registerEvent("Permission Remove", SimpleEvent.class, PermissionRemoveEvent.class, "permission remove")
			.description("Called when a player loses a luckperms permission.")
			.examples("on permission remove:");
		Skript.registerEvent("Prefix Remove", SimpleEvent.class, PrefixRemoveEvent.class, "prefix remove")
			.description("Called when a player loses a luckperms prefix.")
			.examples("on prefix remove:");
		Skript.registerEvent("Suffix Remove", SimpleEvent.class, SuffixRemoveEvent.class, "suffix remove")
			.description("Called when a player loses a luckperms suffix.")
			.examples("on suffix remove:");
	}

}
