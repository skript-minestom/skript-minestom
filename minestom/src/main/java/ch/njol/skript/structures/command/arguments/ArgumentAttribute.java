package ch.njol.skript.structures.command.arguments;

import net.kyori.adventure.key.Key;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.attribute.Attribute;

public class ArgumentAttribute extends CustomArgumentResource<Attribute> {

	public ArgumentAttribute(String id) {
		super(id, "minecraft:attribute");
	}

	@Override
	protected Attribute getValue(CommandSender sender, Key key) {
		return Attribute.fromKey(key);
	}

}
