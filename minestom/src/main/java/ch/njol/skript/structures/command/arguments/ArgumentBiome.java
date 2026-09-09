package ch.njol.skript.structures.command.arguments;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.world.biome.Biome;

public class ArgumentBiome extends CustomArgumentResource<Biome> {

	public ArgumentBiome(String id) {
		super(id, "minecraft:worldgen/biome");
	}

	@Override
	protected Biome getValue(CommandSender sender, Key key) {
		return MinecraftServer.getBiomeRegistry().get(key);
	}

}
