package ch.njol.skript.util.blockchange;

import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://github.com/KloonInnovations/GameServer-Public/blob/915e44d1d22a1e2338a242daab8b73a7f0d473b7/Minestom/src/main/java/io/kloon/gameserver/minestom/blockchange/SectionChange.java
public class SectionChange {
	private static final Logger LOG = LoggerFactory.getLogger(SectionChange.class);

	private final Short2IntOpenHashMap stateIds = new Short2IntOpenHashMap(64, Short2IntOpenHashMap.FAST_LOAD_FACTOR);

	public void set(int sectionX, int sectionY, int sectionZ, Block block) {
		int index = sectionX << 8 | sectionZ << 4 | sectionY;
		short indexShort = (short) index;
		stateIds.put(indexShort, block.stateId());
	}

	public void applyToServer(Chunk chunk, int section) {
		if (chunk instanceof LightingChunk lighting) {
			lighting.setFreezeInvalidation(true);
		}

		try {
			for (Short2IntMap.Entry e : stateIds.short2IntEntrySet()) {
				short position = e.getShortKey();
				int state = e.getIntValue();

				int sectionX = position >> 8;
				int sectionZ = (position >> 4) & 0xF;
				int sectionY = position & 0xF;

				int worldX = chunk.getChunkX() * 16 + sectionX;
				int worldY = section * 16 + sectionY;
				int worldZ = chunk.getChunkZ() * 16 + sectionZ;

				Block block = Block.fromStateId(state);

				assert block != null;
				chunk.setBlock(worldX, worldY, worldZ, block);
			}
		} finally {
			if (chunk instanceof LightingChunk lighting) {
				lighting.setFreezeInvalidation(false);

				lighting.invalidate();
				lighting.invalidateNeighborsSection(section);
				lighting.invalidateResendDelay();
			}
		}
	}

	public MultiBlockChangePacket getStateChangePacket(int chunkX, int section, int chunkZ) {
		long[] entries = new long[stateIds.size()];
		int index = 0;
		for (Short2IntMap.Entry e : stateIds.short2IntEntrySet()) {
			short position = e.getShortKey();
			long state = e.getIntValue();
			long entry = state << 12 | position;
			entries[index] = entry;
			++index;
		}

		return new MultiBlockChangePacket(chunkX, section, chunkZ, entries);
	}
}
