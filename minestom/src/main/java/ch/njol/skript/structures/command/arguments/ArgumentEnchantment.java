package ch.njol.skript.structures.command.arguments;

import ch.njol.skript.util.Enchantment;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;

public class ArgumentEnchantment extends CustomArgumentResource<Enchantment> {

	public ArgumentEnchantment(String id) {
		super(id, "minecraft:enchantment");
	}

	@Override
	protected Enchantment getValue(CommandSender sender, Key key) {
		return new Enchantment(MinecraftServer.getEnchantmentRegistry().getKey(key), -1);
	}

}
