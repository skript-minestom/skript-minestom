package ch.njol.skript.events;

import ch.njol.skript.registrations.EventValues;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.listener.PlayerActionListener;
import net.minestom.server.network.packet.client.play.ClientPlayerActionPacket;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;

public class PacketPlayerFinishDiggingEvent extends Event {


	private static final HandlerList HANDLERS = new HandlerList();

	static {
		MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, event -> {
			if (!(event.getPacket() instanceof ClientPlayerActionPacket packet) || packet.status() != ClientPlayerActionPacket.Status.FINISHED_DIGGING) return;
			Player player = event.getPlayer();
			Instance instance = event.getInstance();
			BlockVec point = packet.blockPosition().asBlockVec();

			if (!instance.isChunkLoaded(point)) return;
			Block block = instance.getBlock(point);
			//if (PlayerActionListener.shouldPreventBreaking(player, block)) return; // todo make this a condition element
			Bukkit.getPluginManager().callEvent(new PacketPlayerFinishDiggingEvent(point, instance, block, player));
		});

		EventValues.registerEventValue(EventValue.simple(PacketPlayerFinishDiggingEvent.class, Player.class, PacketPlayerFinishDiggingEvent::getPlayer));
		EventValues.registerEventValue(EventValue.simple(PacketPlayerFinishDiggingEvent.class, Instance.class, PacketPlayerFinishDiggingEvent::getInstance));
		EventValues.registerEventValue(EventValue.simple(PacketPlayerFinishDiggingEvent.class, Block.class, PacketPlayerFinishDiggingEvent::getBlock));
		EventValues.registerEventValue(EventValue.simple(PacketPlayerFinishDiggingEvent.class, BlockVec.class, PacketPlayerFinishDiggingEvent::getBlockPosition));
	}

	private final Player player;
	private final Instance instance;
	private final Block block;
	private final BlockVec blockPosition;

	public PacketPlayerFinishDiggingEvent(BlockVec blockPosition, Instance instance, Block block, Player player) {
		this.blockPosition = blockPosition;
		this.instance = instance;
		this.block = block;
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public Instance getInstance() {
		return instance;
	}

	public Block getBlock() {
		return block;
	}

	public BlockVec getBlockPosition() {
		return blockPosition;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
