package ch.njol.skript.structures.command.arguments;

import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Item;
import ch.njol.skript.util.NBTCompound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.*;
import net.minestom.server.command.builder.arguments.minecraft.*;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentEntityType;
import net.minestom.server.command.builder.arguments.minecraft.registry.ArgumentParticle;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.command.builder.arguments.number.ArgumentFloat;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.command.builder.arguments.number.ArgumentLong;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeBlockPosition;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec2;
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeVec3;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.entity.EntityFinder;
import net.minestom.server.utils.location.RelativeVec;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public enum ArgumentType {

	// todo ranges, resources, and more mc specific support

	// basic
	LITERAL("literal", ArgumentLiteral::new),
	BOOLEAN("boolean", ArgumentBoolean::new),
	INTEGER("integer", ArgumentInteger::new),
	LONG("long", ArgumentLong::new),
	DOUBLE("double", ArgumentDouble::new),
	FLOAT("float", ArgumentFloat::new),
	STRING("string", ArgumentString::new),
	WORD("word", ArgumentWord::new),
	STRING_ARRAY("stringarray", ArgumentStringArray::new),

	// enums
	GAME_MODE("gamemode", GameMode.class),
	SOUND_CATEGORY("soundcategory", Sound.Source.class),

	// minecraft specific
	//COMMAND("command", ArgumentCommand::new), // works but doesn't predict properly clientside so considered broken
	PARTICLE("particle", ArgumentParticle::new), // doesn't seem to predict properly clientside but may be useful still
	ENTITY_TYPE("entitytype", ArgumentEntityType::new),
	BLOCK("block", ArgumentBlockState::new),
	ENTITY("entity", s -> new ArgumentEntity(s).map((_, entityFinder) ->
		new CustomEntityFinder(entityFinder.setLimit(1), false, true))),
	ENTITIES("entities", s -> new ArgumentEntity(s).map((_, entityFinder) ->
		new CustomEntityFinder(entityFinder, false, false))),
	PLAYER("player", s -> new ArgumentEntity(s).map((_, entityFinder) ->
		new CustomEntityFinder(entityFinder.setLimit(1), true, true))),
	PLAYERS("players", s -> new ArgumentEntity(s).map((_, entityFinder) ->
		new CustomEntityFinder(entityFinder, true, false))),
	ITEM("item", ArgumentItemStack::new),
	COMPONENT("component", ArgumentComponent::new),
	UUID("uuid", ArgumentUUID::new),
	NBT_COMPOUND("nbtcompound", ArgumentNbtCompoundTag::new),
	RELATIVE_BLOCK_POSITION("blockposition", ArgumentRelativeBlockPosition::new),
	VECTOR_3("vector", ArgumentRelativeVec3::new),
	VECTOR_2("2dvector", ArgumentRelativeVec2::new),

	// resources
	ATTRIBUTE_TYPE("attributetype", ArgumentAttribute::new),
	BIOME("biome", ArgumentBiome::new),
	DAMAGE_TYPE("damagetype", s -> new ArgumentResource(s, "minecraft:damage_type")),
	ENCHANT("enchant", ArgumentEnchantment::new),
	SOUND("sound", s -> new ArgumentResource(s, "minecraft:sound_event")),;

	private final String expectedInitialInput;
	private final BiFunction<String, String, Argument<?>> provider;

	// typeInput is provided as 2nd argument so things line int ranges can use them later on when implemented
	ArgumentType(String expectedInitialInput, BiFunction<String, String, Argument<?>> provider) {
		this.expectedInitialInput = expectedInitialInput;
		this.provider = provider;
	}

	ArgumentType(String expectedInitialInput, Function<String, Argument<?>> provider) {
		this(expectedInitialInput, (s, _) -> provider.apply(s));
	}

	ArgumentType(String expectedInitialInput, Class<? extends Enum<?>> enumClass) {
		this(expectedInitialInput, s -> new ArgumentEnum<>(s, enumClass).setFormat(ArgumentEnum.Format.LOWER_CASED));
	}

	public static Object convertToSkriptObject(Object o, CommandSender sender, Argument<?> arg) {
		if (o instanceof Key key) return key.asString();
		if (o instanceof UUID uuid) return uuid.toString();
		if (o instanceof ItemStack itemStack) return new Item(itemStack);
		if (o instanceof Component component) return new ComponentWrapper(component);
		if (o instanceof CompoundBinaryTag compound) return new NBTCompound(compound, false);
		if (o instanceof CustomEntityFinder(EntityFinder entityFinder, boolean onlyPlayers, boolean single)) {
			Stream<Entity> entityStream = entityFinder.find(sender).stream();
			if (onlyPlayers) entityStream = entityStream.filter(entity -> entity instanceof Player);
			List<Entity> entities = entityStream.toList();
			if (single) {
				if (entities.isEmpty()) return null;
				return entities.getFirst();
			}
			return entities.toArray(new Entity[0]);
		}
		if (o instanceof RelativeVec relativeVec) {
			if (arg instanceof ArgumentRelativeVec2) {
				if (sender instanceof Player player) return relativeVec.fromView(player);
				return relativeVec.vec();
			}
			Point point = relativeVec.fromSender(sender);
			if (sender instanceof Player player) point = player.getPosition().withCoord(point);
			return point;
		}
		return o;
	}

	public boolean matchesInitialInput(String input) {
		return input.equalsIgnoreCase(expectedInitialInput);
	}

	public BiFunction<String, String, Argument<?>> getProvider() {
		return provider;
	}

	private record CustomEntityFinder(EntityFinder entityFinder, boolean onlyPlayers, boolean single) {}

}
