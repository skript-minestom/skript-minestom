package ch.njol.skript.util.blockchange;

import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.world.DimensionType;

import java.util.ArrayList;
import java.util.List;

// https://github.com/KloonInnovations/GameServer-Public/blob/915e44d1d22a1e2338a242daab8b73a7f0d473b7/Minestom/src/main/java/io/kloon/gameserver/minestom/blockchange/ChunkChange.java
public class ChunkChange {
	private static final int HEIGHT_PER_SECTION = 16;

	private final int chunkX;
	private final int chunkZ;
	private final SectionChange[] sections;
	private final int minY;

	ChunkChange(MultiBlockChange multi, int chunkX, int chunkZ) {
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;

		DimensionType dimension = multi.getDimension();

		int sectionsCount = dimension.height() / HEIGHT_PER_SECTION;
		this.sections = new SectionChange[sectionsCount];

		this.minY = dimension.minY();
//        this.minY = 0;
	}

	public void set(int worldX, int worldY, int worldZ, Block block) {
		int sectionX = CoordConversion.globalToSectionRelative(worldX);
		int sectionY = CoordConversion.globalToSectionRelative(worldY);
		int sectionZ = CoordConversion.globalToSectionRelative(worldZ);

		SectionChange section = getSectionAt(worldY);
		section.set(sectionX, sectionY, sectionZ, block);
	}

	private SectionChange getSectionAt(int worldY) {
		int sectionY = (worldY - minY) >> 4;
		SectionChange section = sections[sectionY];
		if (section == null) {
			section = new SectionChange();
			sections[sectionY] = section;
		}
		return section;
	}

	public void applyToServer(Chunk chunk) {
		int offset = minY / 16;
		for (int i = 0; i < sections.length; ++i) {
			SectionChange section = sections[i];
			if (section == null) continue;
			section.applyToServer(chunk, i + offset);
		}
	}

	public List<ServerPacket> getPackets() {
		int offset = minY / 16;
		List<ServerPacket> packets = new ArrayList<>();
		for (int i = 0; i < sections.length; ++i) {
			SectionChange section = sections[i];
			if (section == null) continue;
			packets.add(section.getStateChangePacket(chunkX, i + offset, chunkZ));
		}
		return packets;
	}
}