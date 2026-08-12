package ch.njol.skript.util.blockchange;

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.PacketSendingUtils;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// https://github.com/KloonInnovations/GameServer-Public/blob/915e44d1d22a1e2338a242daab8b73a7f0d473b7/Minestom/src/main/java/io/kloon/gameserver/minestom/blockchange/MultiBlockChange.java
public class MultiBlockChange {
	private static final Logger LOG = LoggerFactory.getLogger(MultiBlockChange.class);

	private final Instance instance;
	private Map<ChunkCoord, ChunkChange> changesByChunk = new HashMap<>();

	public MultiBlockChange(Instance instance) {
		this.instance = instance;
	}

	public DimensionType getDimension() {
		return instance.getCachedDimensionType();
	}

	public MultiBlockChange set(Point point, Block block) {
		return set(point.blockX(), point.blockY(), point.blockZ(), block);
	}

	public MultiBlockChange set(int x, int y, int z, Block block) {
		int chunkX = CoordConversion.globalToChunk(x);
		int chunkZ = CoordConversion.globalToChunk(z);
		ChunkCoord coord = new ChunkCoord(chunkX, chunkZ);
		ChunkChange chunkChange = changesByChunk.computeIfAbsent(coord, c -> new ChunkChange(this, c.chunkX, c.chunkZ));
		chunkChange.set(x, y, z, block);
		return this;
	}

	@Nullable
	private Chunk getAndLoadChunkFromServer(ChunkCoord coords) {
		Chunk chunk = instance.getChunk(coords.chunkX, coords.chunkZ);
		if (chunk == null) {
			chunk = instance.loadChunk(coords.chunkX, coords.chunkZ).join();
		}
		if (chunk == null || !chunk.isLoaded()) {
			LOG.warn("Couldn't load chunk at {} in MultiBlockChange", coords);
			return null;
		}
		return chunk;
	}

	public void broadcastTo(Set<Player> viewers) {
		changesByChunk.forEach((coords, changes) -> {
			Chunk chunk = getAndLoadChunkFromServer(coords);
			if (chunk == null) return;

			changes.getPackets().forEach(packet -> {
				PacketSendingUtils.sendGroupedPacket(viewers, packet);
			});
		});
		changesByChunk.clear();
		changesByChunk = null;
	}

	private record ChunkCoord(int chunkX, int chunkZ) {}
}
