package com.github.hapily04.skriptminestom.registration;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.*;
import ch.njol.skript.effects.particle.*;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.*;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Fields;
import com.github.hapily04.skriptminestom.util.NBTUtils;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.color.Color;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntityAnimationPacket;
import net.minestom.server.network.player.ClientSettings;
import net.minestom.server.particle.Particle;
import net.minestom.server.ping.ServerListPingType;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.sound.Music;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Taggable;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.MoonPhase;
import net.minestom.server.world.attribute.AmbientParticle;
import net.minestom.server.world.attribute.AmbientSounds;
import net.minestom.server.world.attribute.BackgroundMusic;
import net.minestom.server.world.attribute.BedRule;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.*;
import java.util.stream.Collectors;

import static ch.njol.skript.expressions.ExprAmbientSounds.getSoundEvent;
import static ch.njol.skript.util.ComponentWrapper.toWrapper;
import static com.github.hapily04.skriptminestom.util.MessageUtils.BASIC_MINI_MESSAGE;
import static com.github.hapily04.skriptminestom.util.NumberUtils.timespanFrom;

@SuppressWarnings("unchecked")
public class MinestomClasses {

	public static final Changer<Item> ITEM_CHANGER = new Changer<>() {
		@Override
		public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
			return switch (mode) {
				case DELETE, SET -> CollectionUtils.array(Item.class);
				case REMOVE, ADD -> CollectionUtils.array(Enchantment[].class);
				default -> null;
			};
		}

		@Override
		public void change(Item[] what, @org.jetbrains.annotations.Nullable @Nullable Object[] delta, ChangeMode mode) {
			for (Item item : what) {
				switch (mode) {
					case DELETE -> item.modify(_ -> ItemStack.AIR, true);
					case SET -> {
						Item changeItem = (Item) delta[0];
						if (changeItem == null) continue;
						item.modify(_ -> changeItem.getItem(), true);
					}
					case ADD -> {
						Enchantment[] enchantments = Arrays.copyOf(delta, delta.length, Enchantment[].class);
						Enchantment.add(item, true, enchantments);
					}
					case REMOVE -> {
						Enchantment[] enchantments = Arrays.copyOf(delta, delta.length, Enchantment[].class);
						Enchantment.remove(item, enchantments);
					}
				}
			}
		}
	};

	public static void register() {
		/*
		 * Classes
		 */
		Classes.registerClass(new ClassInfo<>(CommandSender.class, "sender") // sender instead of commandsender for StructCommand
			.user("senders?")
			.name("Command Sender")
			.description("Something that can execute a command and receive messages (players/console).")
			.examples("""
				command /test:
					condition:
						return whether sender is a player # command can only be executed by players
					trigger:
						give player 5 diamond""")
			.defaultExpression(new EventValueExpression<>(CommandSender.class)));
		Classes.registerClass(new ClassInfo<>(ConsoleSender.class, "consolesender")
			.user("console ?senders?")
			.name("Console Sender")
			.description("The console.")
			.examples("send \"Server started\" to console")
			.defaultExpression(new EventValueExpression<>(ConsoleSender.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull ConsoleSender o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull ConsoleSender o) {
					return "console";
				}
			}));
		Classes.registerClass(new ClassInfo<>(Player.class, "player")
			.user("players?")
			.name("Player")
			.description("A entity of type Player with a connection to the server.")
			.examples("send \"hi\" to player(\"Steve\")")
			.defaultExpression(new EventValueExpression<>(Player.class))
			.parser(new Parser<>() {
				@Nullable
				public Player parse(@NotNull String s, @NotNull ParseContext context) {
					return MinestomFunctions.findPlayer(s, true);
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return context == ParseContext.COMMAND || context == ParseContext.PARSE;
				}

				@Override
				public @NotNull String toString(@NotNull Player o, int flags) {
					return o.getUsername();
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Player o) {
					return SkriptConfig.usePlayerUUIDsInVariableNames.value() ? o.getUuid().toString() : toString(o, 0);
				}
			})
			.changer(new Changer<>() {
				@SuppressWarnings("DataFlowIssue")
				@Override
				public @Nullable Class<?> @NotNull [] acceptChange(@NotNull ChangeMode mode) {
					return switch (mode) {
						case ADD, REMOVE -> CollectionUtils.array(AbstractInventory[].class, Item[].class);
						case REMOVE_ALL, DELETE -> CollectionUtils.array(Item[].class);
						default -> null;
					};
				}

				@Override
				public void change(Player @NotNull [] what, @Nullable Object @NotNull [] delta, @NotNull ChangeMode mode) {
					for (Player player : what) {
						AbstractInventory inventory = player.getInventory();
						inventoryChange(delta, mode, inventory);
					}
				}
			}));
		Classes.registerClass(new ClassInfo<>(Taggable.class, "taggable")
			.user("taggables?")
			.name("Taggable")
			.description("An object that can hold tags (entities, instances, etc.)")
			.examples("set metadata \"key\" of player to \"value\"")
			.defaultExpression(new EventValueExpression<>(Taggable.class)));
		Classes.registerClass(new ClassInfo<>(Entity.class, "entity")
			.user("entit(y|ies)")
			.name("Entity")
			.description("A mob/player/physical non-block object in an instance.")
			.examples("kill {_entity}")
			.defaultExpression(new EventValueExpression<>(Entity.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Entity o, int flags) {
					return Classes.toString(o.getEntityType());
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Entity o) {
					return o.getUuid().toString();
				}
			}));
		Classes.registerClass(new ClassInfo<>(LivingEntity.class, "livingentity")
			.user("living ?entit(y|ies)")
			.name("Living Entity")
			.description("An entity that has health, armor, and a main/offhand.")
			.examples("set health of {_entity} to 20")
			.defaultExpression(new EventValueExpression<>(LivingEntity.class)));
		Classes.registerClass(new ClassInfo<>(EntityCreature.class, "entitycreature")
			.user("entity ?creatures?")
			.name("Entity Creature")
			.description("An entity that has health, armor, main/offhand, and is able to pathfind.")
			.examples("set navigation target of {_entity} to player")
			.defaultExpression(new EventValueExpression<>(EntityCreature.class)));
		Classes.registerClass(new ClassInfo<>(EquipmentHandler.class, "equipmenthandler")
			.user("equipment ?handlers?")
			.name("Equipment Handler")
			.description("An entity that is capable of bearing armor and off/main hand tools.")
			.examples("set chestplate of player to iron chestplate")
			.defaultExpression(new EventValueExpression<>(EquipmentHandler.class)));
		Classes.registerClass(new ClassInfo<>(Pos.class, "position")
			.user("positions?")
			.name("Position")
			.description("A location with an x, y, z, yaw, and pitch. An instance is not attached to this type.")
			.examples("set {_pos} to position(0, 64, 0)")
			.defaultExpression(new EventValueExpression<>(Pos.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Pos o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Pos o) {
					return "position x: " + o.x() + " y: " + o.y() + " z: " + o.z() + " yaw: " + o.yaw() + " pitch: " + o.pitch();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Pos o) {
					Fields fields = new Fields();
					fields.putPrimitive("x", o.x());
					fields.putPrimitive("y", o.y());
					fields.putPrimitive("z", o.z());
					fields.putPrimitive("yaw", o.yaw());
					fields.putPrimitive("pitch", o.pitch());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Pos o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Pos deserialize(@NotNull Fields f) throws StreamCorruptedException {
					double x = f.getPrimitive("x", double.class);
					double y = f.getPrimitive("y", double.class);
					double z = f.getPrimitive("z", double.class);
					float yaw = f.getPrimitive("yaw", float.class);
					float pitch = f.getPrimitive("pitch", float.class);
					return new Pos(x, y, z, yaw, pitch);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(Vec.class, "vector")
			.user("vectors?")
			.name("Vector")
			.description("An object with 3 values: x, y, z. Can be used as a location, but position is used more often for that use-case.")
			.examples("set {_vec} to vector(1, 0, 0)")
			.defaultExpression(new EventValueExpression<>(Vec.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Vec o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Vec o) {
					return "vector x: " + o.x() + " y: " + o.y() + " z: " + o.z();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Vec o) {
					Fields fields = new Fields();
					fields.putPrimitive("x", o.x());
					fields.putPrimitive("y", o.y());
					fields.putPrimitive("z", o.z());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Vec o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Vec deserialize(@NotNull Fields f) throws StreamCorruptedException {
					double x = f.getPrimitive("x", double.class);
					double y = f.getPrimitive("y", double.class);
					double z = f.getPrimitive("z", double.class);
					return new Vec(x, y, z);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(Point.class, "point")
			.user("points?")
			.name("Point")
			.description("An object with 3 values: x, y, z. Is internally either a block vector, vector, or position.")
			.examples("set {_p} to player's position")
			.defaultExpression(new EventValueExpression<>(Point.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Point o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Point o) {
					return "point x: " + o.x() + " y: " + o.y() + " z: " + o.z();
				}
			})
			.serializeAs(Vec.class)); // don't think a serializer will be used as it should go to blockvec/vector/position
		Classes.registerClass(new ClassInfo<>(BlockVec.class, "blockvector")
			.user("block ?vectors?")
			.name("Block Vector")
			.description("A vector with the x, y, and z without decimals.")
			.examples("set {_bv} to blockVector(0, 64, 0)")
			.defaultExpression(new EventValueExpression<>(BlockVec.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull BlockVec o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull BlockVec o) {
					return "blockvector: x: " + o.x() + " y: " + o.y() + " z: " + o.z();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull BlockVec o) {
					Fields fields = new Fields();
					fields.putPrimitive("x", o.x());
					fields.putPrimitive("y", o.y());
					fields.putPrimitive("z", o.z());
					return fields;
				}

				@Override
				public void deserialize(@NotNull BlockVec o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull BlockVec deserialize(@NotNull Fields f) throws StreamCorruptedException {
					double x = f.getPrimitive("x", double.class);
					double y = f.getPrimitive("y", double.class);
					double z = f.getPrimitive("z", double.class);
					return new BlockVec(x, y, z);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(Instance.class, "instance")
			.user("instances?")
			.name("Instance")
			.description("A world consisting of blocks and entities.")
			.examples("teleport player to {_instance}")
			.defaultExpression(new EventValueExpression<>(Instance.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Instance o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Instance o) {
					return "instance with uuid: " + o.getUuid();
				}
			})
			.supplier(() -> MinecraftServer.getInstanceManager().getInstances().iterator()));
		Classes.registerClass(new ClassInfo<>(InstanceContainer.class, "instancecontainer")
			.user("instance ?containers?")
			.name("Instance Container")
			.description("A world consisting of blocks and entities.")
			.examples("set {_world} to instance of player")
			.defaultExpression(new EventValueExpression<>(InstanceContainer.class))
			.supplier(() -> MinecraftServer.getInstanceManager().getInstances().stream()
				.filter(instance -> instance instanceof InstanceContainer)
				.map(instance -> (InstanceContainer) instance)
				.iterator()));
		Classes.registerClass(new ClassInfo<>(SharedInstance.class, "sharedinstance")
			.user("shared ?instances?")
			.name("Shared Instance")
			.description("A world sharing the blocks from its underlying Instance Container. Entities are not shared.")
			.examples("create shared instance from {_container} and store it in {_shared}")
			.defaultExpression(new EventValueExpression<>(SharedInstance.class))
			.supplier(() -> MinecraftServer.getInstanceManager().getInstances().stream()
				.filter(instance -> instance instanceof SharedInstance)
				.map(instance -> (SharedInstance) instance)
				.iterator()));
		Classes.registerClass(new ClassInfo<>(Chunk.class, "chunk")
			.user("chunks?")
			.name("Chunk")
			.description("A 16×16 section of blocks in an instance.")
			.examples("loop all blocks in chunk at player:")
			.defaultExpression(new EventValueExpression<>(Chunk.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public String toString(Chunk o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public String toVariableNameString(Chunk o) {
					return "chunk x: " + o.getChunkX() + " z: " + o.getChunkZ();
				}
			}));
		Classes.registerClass(new ClassInfo<>(DimensionType.class, "dimensiontype")
			.user("dimension ?types?")
			.name("Dimension Type")
			.description("A dimension type with several values.")
			.examples("set {_dim} to dimension type from namespace key \"minecraft:overworld\"")
			.defaultExpression(new EventValueExpression<>(DimensionType.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public String toString(DimensionType o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public String toVariableNameString(DimensionType o) {
					Key key = null;
					RegistryKey<DimensionType> registryKey = MinecraftServer.getDimensionTypeRegistry().getKey(o);
					if (registryKey != null) key = registryKey.key();
					return "dimension type" + (key != null ? " under namespace: " + key.asString() : "");
				}
			}));
		Classes.registerClass(new ClassInfo<>(Biome.class, "biome")
			.user("biomes?")
			.name("Biome")
			.description("A biome with several values.")
			.examples("""
				create biome under "minecraft:lobby" stored in {_b}:
					sky color: rgb(111, 223, 249)
					fog color: rgb(253, 252, 198)
					foliage color: rgb(71, 255, 0)
					water color: rgb(111, 223, 249)""")
			.defaultExpression(new EventValueExpression<>(Biome.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public String toString(Biome o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public String toVariableNameString(Biome o) {
					Key key = null;
					RegistryKey<Biome> registryKey = MinecraftServer.getBiomeRegistry().getKey(o);
					if (registryKey != null) key = registryKey.key();
					return "biome" + (key != null ? " under namespace: " + key.asString() : "");
				}
			}));
		Classes.registerClass(new ClassInfo<>(Block.class, "block")
			.user("blocks?")
			.name("Block")
			.description("A block with a type, properties (blockdata), nbt, and handler.")
			.usage("<block_namespace>[<properties>]")
			.examples("stone button[powered=true]")
			.defaultExpression(new EventValueExpression<>(Block.class))
			.parser(new Parser<>() {
				@Nullable
				public Block parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH);
					int endChar;
					int initialBracketPos = -1;
					if (s.contains("[")) {
						if (!s.endsWith("]")) return null;
						initialBracketPos = s.indexOf('[');
						endChar = initialBracketPos-1;
					}
					else endChar = s.length()-1;
					String nameSpace = s.substring(0, endChar+1);
					nameSpace = nameSpace.replace(' ', '_');
					if (!nameSpace.contains(":")) nameSpace = "minecraft:" + nameSpace;
					else if (!nameSpace.startsWith("minecraft:")) return null; // only minecraft: is supported since you can't add mod blocks anyway atm
					if (!Key.parseable(nameSpace)) return null;
					Block block = Block.fromState(nameSpace);
					if (block == null) return null;
					if (initialBracketPos == -1) return block;
					String blockData = s.substring(initialBracketPos);
					blockData = blockData.replace("[", "").replace("]", "");
					if (blockData.isEmpty()) return block; // support stone[] (blank properties)
					int commaAmount = getCharacterAmount(blockData, ',');
					String[] properties = blockData.split(",");
					if (properties.length != commaAmount+1) return null;
					Map<String, String> propertyMap = new HashMap<>();
					Map<String, String> defaultPropertyMap = block.properties();
					for (String property : properties) {
						if (!parseProperty(property, propertyMap, defaultPropertyMap)) return null; // property did not parse
					}
					return block.withProperties(propertyMap);
				}

				private boolean parseProperty(String property, Map<String, String> into, Map<String, String> defaultProperties) {
					int equalSignAmount = getCharacterAmount(property, '=');
					if (equalSignAmount != 1) return false;
					String[] parts = property.split("=");
					String key = parts[0];
					if (!defaultProperties.containsKey(key)) return false; // invalid property for this block
					if (into.containsKey(key)) return false; // don't allow stone[bob=true,bob=false] (duplicate property keys)
					into.put(key, parts[1]);
					return true;
				}

				private int getCharacterAmount(String blockData, char character) {
					return Math.toIntExact(blockData.chars().filter(c -> c == character).count());
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull Block o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Block o) {
					return o.state(); // leave this because properties can be complex
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Block o) {
					Fields fields = new Fields();
					fields.putPrimitive("data-version", MinecraftServer.DATA_VERSION);
					fields.putObject("state", o.state());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Block o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Block deserialize(@NotNull Fields f) throws StreamCorruptedException {
					//int dataVersion = f.getPrimitive("data-version", int.class);
					String state = f.getObject("state", String.class);
					if (state == null) throw new StreamCorruptedException("State was not found.");
					Block block = Block.fromState(state);
					if (block == null)
						throw new StreamCorruptedException("Block with state id '" + state + "' was not found.");
					return block;
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			})
			.supplier(Block.values().toArray(new Block[0])));
		Classes.registerClass(new EnumClassInfo<>(GameMode.class, "gamemode")
			.user("game ?modes?")
			.name("Game Mode")
			.description("Represents a Minecraft game mode. Possible values: survival, creative, adventure, spectator.")
			.examples("set player's game mode to creative")
			.defaultExpression(new EventValueExpression<>(GameMode.class)));
		Classes.registerClass(new EnumClassInfo<>(PlayerHand.class, "playerhand")
			.user("player ?hands?")
			.name("Player Hand")
			.description("Represents a player's hand. Possible values: main, off.")
			.examples("""
				on item use:
					broadcast "%event-hand%\"""")
			.defaultExpression(new EventValueExpression<>(PlayerHand.class)));
		Classes.registerClass(new EnumClassInfo<>(ItemAnimation.class, "itemanimation")
			.user("item ?animations?")
			.name("Item Animation")
			.description("Represents the animation an item is playing while consuming.")
			.examples("""
				on item use:
					broadcast "%event-animation%\"""")
			.defaultExpression(new EventValueExpression<>(ItemAnimation.class)));
		Classes.registerClass(new EnumClassInfo<>(InventoryType.class, "inventorytype")
			.user("inventory ?types?")
			.name("Inventory Type")
			.description("The type of an inventory window (chest, anvil, etc.).")
			.examples("open chest 3 row to player")
			.defaultExpression(new EventValueExpression<>(InventoryType.class)));
		Classes.registerClass(new EnumClassInfo<>(ClickType.class, "clicktype")
			.user("click ?types?")
			.name("Click Type")
			.description("The type of click in an inventory click event.")
			.examples("on inventory click:\n\tif event-clicktype is left click:")
			.defaultExpression(new EventValueExpression<>(ClickType.class)));
		Classes.registerClass(new EnumClassInfo<>(FrameType.class, "frametype")
			.user("frame ?types?")
			.name("Frame Type")
			.description("The type of the frame for an advancement/notification.")
			.examples("send task notification with title \"Quest Complete!\" and diamond as the icon to player")
			.defaultExpression(new EventValueExpression<>(FrameType.class)));
		Classes.registerClass(new ClassInfo<>(ComponentWrapper.class, "component")
			.user("components?")
			.name("Component")
			.description("A piece of text with formatting (adventure component).")
			.examples("set player's tab list header to mm(\"<rainbow>Hello!\")")
			.defaultExpression(new EventValueExpression<>(ComponentWrapper.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull ComponentWrapper o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull ComponentWrapper o) {
					return BASIC_MINI_MESSAGE.serialize(o.getComponent());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public Fields serialize(ComponentWrapper o) throws NotSerializableException {
					Fields fields = new Fields();
					fields.putObject("gson", GsonComponentSerializer.gson().serialize(o.getComponent()));
					return fields;
				}

				@Override
				public void deserialize(ComponentWrapper o, Fields f) throws StreamCorruptedException, NotSerializableException {
					assert false;
				}

				@Override
				protected @NonNull ComponentWrapper deserialize(@NotNull Fields f) throws StreamCorruptedException {
					String componentString = f.getObject("gson", String.class);
					assert componentString != null;
					return new ComponentWrapper(GsonComponentSerializer.gson().deserialize(componentString));
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(TagResolver.class, "tagresolver")
			.user("tag ?resolvers?")
			.name("Tag Resolver")
			.description("Replace tags within a MiniMessage string.")
			.examples("set {_r} to resolver(\"name\", player's name)")
			.defaultExpression(new EventValueExpression<>(TagResolver.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull TagResolver o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull TagResolver o) {
					return o.toString();
				}
			}));
		Classes.registerClass(new ClassInfo<>(ComponentLike.class, "componentlike")
			.user("component ?likes?")
			.name("Component Like")
			.description("Represents something that can be viewed as a component, like a regular component or a hover event.")
			.examples("set {_c} to mm(\"Hello\")")
			.defaultExpression(new EventValueExpression<>(ComponentLike.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull ComponentLike o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull ComponentLike o) {
					return Classes.toString(toWrapper(o.asComponent()));
				}
			}));
		Classes.registerClass(new ClassInfo<>(SuggestionEntry.class, "suggestionentry")
			.user("suggestion ?entr(y|ies)?")
			.name("Suggestion Entry")
			.description("An entry for the suggestions section of a command containing the suggestion and a component tooltip.")
			.examples("add suggestionEntry(\"hello\", mm(\"<red>tooltip!\")) to suggestions")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull SuggestionEntry o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull SuggestionEntry o) {
					return "suggestion entry: " + o.getEntry() + " tooltip: " + Classes.toString(o.getTooltip());
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(ResourcePackStatus.class, "resourcepackstatus")
			.user("resource ?pack ?status(es)?")
			.name("Resource Pack Status")
			.description("The status of a resource pack that was sent.")
			.examples("if resource pack status is successfully loaded:")
			.defaultExpression(new EventValueExpression<>(ResourcePackStatus.class)));
		Classes.registerClass(new ClassInfo<>(PlayerInventory.class, "playerinventory")
			.user("player ?inventor(y|ies)")
			.name("Player Inventory")
			.description("Represents a player's inventory.")
			.examples("clear player's inventory")
			.defaultExpression(new EventValueExpression<>(PlayerInventory.class)));
		Classes.registerClass(new ClassInfo<>(Inventory.class, "nonsense")
			.name("Nonsense")
			.description("Internal inventory class alias used by Skript.")
			.defaultExpression(new EventValueExpression<>(Inventory.class)));
		Classes.registerClass(new ClassInfo<>(AbstractInventory.class, "inventory")
			.user("inventor(y|ies)")
			.name("Inventory")
			.description("Represents an inventory, such as a player's inventory or an anvil inventory.")
			.examples("open {_inventory} to player")
			.defaultExpression(new EventValueExpression<>(AbstractInventory.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AbstractInventory o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AbstractInventory o) {
					String name;
					if (o instanceof Inventory inventory) name = inventory.getInventoryType().name().toLowerCase(Locale.ENGLISH);
					else name = "player";
					return name + " inventory";
				}
			})
			.changer(new Changer<>() {
				@SuppressWarnings("DataFlowIssue")
				@Override
				public @Nullable Class<?> @NotNull [] acceptChange(@NotNull ChangeMode mode) {
					return switch (mode) {
						case ADD, REMOVE -> CollectionUtils.array(AbstractInventory[].class, Item[].class);
						case REMOVE_ALL, DELETE -> CollectionUtils.array(Item[].class);
						default -> null;
					};
				}

				@Override
				public void change(AbstractInventory @NotNull [] what, @Nullable Object @NotNull [] delta, @NotNull ChangeMode mode) {
					for (AbstractInventory inventory : what) {
						inventoryChange(delta, mode, inventory);
					}
				}
			}));
		Classes.registerClass(new ClassInfo<>(Item.class, "item")
			.user("items?")
			.name("Item")
			.description("An item with its amount, enchantments and other data.")
			.examples("give player 2 stone sword")
			.usage("[<number>] <item namespace>")
			.defaultExpression(new EventValueExpression<>(Item.class))
			.parser(new Parser<>() {
				@SuppressWarnings("PatternValidation")
				@Nullable
				public Item parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH);
					String[] parts = s.split(" ");;
					int amount = 1;
					int materialIndex = 0;
					if (parts.length >= 2) {
						String amountPart = parts[0];
						if (NumberUtils.isOnlyDigits(amountPart)) {
							if (!NumberUtils.isInteger(amountPart)) return null;
							try {
								amount = Integer.parseInt(amountPart);
							} catch (NumberFormatException e) {
								return null;
							}
							materialIndex = 1;
						}
					}
					String[] choppedParts = new String[parts.length-materialIndex];
					System.arraycopy(parts, materialIndex, choppedParts, 0, parts.length-materialIndex);
					String nameSpace = String.join("_", choppedParts);
					if (!nameSpace.contains(":")) nameSpace = "minecraft:" + nameSpace;
					else if (!nameSpace.startsWith("minecraft:")) return null; // only minecraft: is supported since you can't add mod items anyways atm
					if (!Key.parseable(nameSpace)) return null;
					Material material = Material.fromKey(nameSpace);
					if (material == null) return null;
					return new Item(ItemStack.of(material, amount));
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull Item o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Item o) {
					ItemStack item = o.getItem();
					return item.amount() + " " + keyToString(item.material().key());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Item o) throws NotSerializableException {
					Fields fields = new Fields();
					try {
						fields.putPrimitive("data-version", MinecraftServer.DATA_VERSION);
						fields.putObject("item-nbt", NBTUtils.asString(o.getItem().toItemNBT()));
					} catch (IOException e) {
						throw new NotSerializableException("Error whilst trying to to serialize an item: " + e.getMessage());
					}
					return fields;
				}

				@Override
				public void deserialize(@NotNull Item o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Item deserialize(@NotNull Fields f) throws StreamCorruptedException {
					try {
						Object nbt = f.getObject("item-nbt");
						if (nbt == null) throw new StreamCorruptedException("Error occurred whilst trying to deserialize an itemstack.");
						return new Item(ItemStack.fromItemNBT(NBTUtils.asCompound((String) nbt)));
					} catch (IOException e) {
						throw new StreamCorruptedException("Error occurred whilst trying to deserialize an itemstack.");
					}
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			})
			.changer(ITEM_CHANGER));
		Classes.registerClass(new ClassInfo<>(Slot.class, "slot")
			.user("slots?")
			.name("Slot")
			.description("Represents an item in a slot in an inventory.")
			.examples("set slot 0 of player's inventory to diamond")
			.defaultExpression(new EventValueExpression<>(Slot.class))
			.serializeAs(Item.class)
			.changer(ITEM_CHANGER));
		Classes.registerClass(new ClassInfo<>(EntityType.class, "entitytype")
			.user("entity ?types?")
			.name("Entity Type")
			.description("The type of an entity (zombie, player, skeleton, etc.)")
			.examples("spawn zombie at player")
			.usage(EntityType.values().stream().map(type -> type.key().value()).collect(Collectors.joining(", ")))
			.defaultExpression(new EventValueExpression<>(EntityType.class))
			.parser(new Parser<>() {
				@Nullable
				public EntityType parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH).replace(' ', '_');
					s = Utils.isPlural(s).updated();
					if (!s.contains("minecraft:")) s = "minecraft:" + s;
					if (!Key.parseable(s)) return null;
					return EntityType.fromKey(s);
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull EntityType o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull EntityType o) {
					return keyToString(o.key());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull EntityType o) throws NotSerializableException {
					Fields fields = new Fields();
					fields.putPrimitive("entity-type", o.key().asString());
					return fields;
				}

				@Override
				public void deserialize(@NotNull EntityType o, @NotNull Fields f) throws StreamCorruptedException {
					assert false;
				}

				@Override
				protected @NotNull EntityType deserialize(@NotNull Fields f) throws StreamCorruptedException {
					String key = f.getPrimitive("entity-type", String.class);
					EntityType type = EntityType.fromKey(key);
					if (type == null)
						throw new StreamCorruptedException("Can't deserialize entity type from key: " + key);
					return type;
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			})
			.supplier(EntityType.values().toArray(new EntityType[0])));
		Classes.registerClass(new EnumClassInfo<>(EquipmentSlot.class, "equipmentslot")
			.user("equipment ?slots?")
			.name("Equipment Slot")
			.description("An equipment slot for an entity. Possible values: main_hand, off_hand, boots, leggings, chestplate, helmet.")
			.examples("set helmet of player to diamond helmet")
			.defaultExpression(new EventValueExpression<>(EquipmentSlot.class)));
		Classes.registerClass(new ClassInfo<>(Sidebar.class, "scoreboard")
			.user("score ?boards?")
			.name("Scoreboard")
			.description("The scoreboard on the side of a player's screen")
			.examples("set {_s} to new sidebar named \"Stats\"")
			.defaultExpression(new EventValueExpression<>(Sidebar.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Sidebar o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Sidebar o) {
					return "scoreboard titled \"" + LegacyComponentSerializer.legacyAmpersand().serialize(o.getTitle()) + "\"";
				}
			}));
		Classes.registerClass(new ClassInfo<>(Team.class, "team")
			.user("teams?")
			.name("Team")
			.description("""
				A scoreboard team. Teams control the color of member name tags and their glow outline, \
				whether members push each other, name tag visibility, friendly fire and prefixes/suffixes.
				Teams live in memory only and are not saved across restarts, so scripts should recreate them on load.""")
			.examples("""
				set {_team} to a new team named "red"
				set team color of {_team} to dark red
				add player to members of {_team}""")
			.defaultExpression(new EventValueExpression<>(Team.class))
			.supplier(() -> MinecraftServer.getTeamManager().getTeams().iterator())
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Team o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Team o) {
					return o.getTeamName();
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(TeamsPacket.CollisionRule.class, "collisionrule")
			.user("collision ?rules?")
			.name("Collision Rule")
			.description("Whether members of a team push each other. Possible values: always, never, push other teams, push own team.")
			.examples("set collision rule of {_team} to never")
			.defaultExpression(new EventValueExpression<>(TeamsPacket.CollisionRule.class)));
		Classes.registerClass(new EnumClassInfo<>(TeamsPacket.NameTagVisibility.class, "nametagvisibility")
			.user("name ?tag ?visibilit(y|ies)")
			.name("Name Tag Visibility")
			.description("Who can see the name tags of a team. Possible values: always, never, hide for other teams, hide for own team.")
			.examples("set name tag visibility of {_team} to hide for other teams")
			.defaultExpression(new EventValueExpression<>(TeamsPacket.NameTagVisibility.class)));
		Classes.registerClass(new ClassInfo<>(BossBar.class, "bossbar")
			.user("boss ?bars?")
			.name("Boss Bar")
			.description("The bar shown at the top of a player's screen.")
			.examples("""
				set {_bar} to new boss bar titled "<red>Dragon" with progress 50
				add {_bar} to boss bars of all players""")
			.defaultExpression(new EventValueExpression<>(BossBar.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull BossBar o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull BossBar o) {
					return "boss bar titled \"" + LegacyComponentSerializer.legacyAmpersand().serialize(o.name()) + "\"";
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(BossBarColor.class, "bossbarcolor")
			.user("boss ?bar ?colou?rs?")
			.name("Boss Bar Color")
			.description("The color of a boss bar. Possible values: pink bar, blue bar, red bar, green bar, yellow bar, purple bar, white bar.")
			.examples("set color of {_bar} to red bar")
			.defaultExpression(new EventValueExpression<>(BossBarColor.class)));
		Classes.registerClass(new EnumClassInfo<>(BossBar.Overlay.class, "bossbaroverlay")
			.user("boss ?bar ?overlays?")
			.name("Boss Bar Overlay")
			.description("The notching of a boss bar. Possible values: progress, notched 6, notched 10, notched 12, notched 20.")
			.examples("set overlay of {_bar} to notched 12")
			.defaultExpression(new EventValueExpression<>(BossBar.Overlay.class)));
		Classes.registerClass(new ClassInfo<>(Enchantment.class, "enchantment")
			.user("enchantments?")
			.name("Enchantment")
			.description("An enchantment for an item, including its level.")
			.examples("add sharpness 5 to enchants of player's tool")
			.usage("<enchantment namespace> [<level>]")
			.defaultExpression(new EventValueExpression<>(Enchantment.class))
			.parser(new Parser<>() {
				@Nullable
				public Enchantment parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH);
					String[] parts = s.split(" ");;
					int level = -1;
					int hasLevel = 0;
					if (parts.length >= 2) {
						String levelPart = parts[parts.length-1];
						if (NumberUtils.isOnlyDigits(levelPart)) {
							if (!NumberUtils.isInteger(levelPart)) return null;
							try {
								level = Integer.parseInt(levelPart);
							} catch (NumberFormatException e) {
								return null;
							}
							hasLevel = 1;
						}
					}
					String[] choppedParts = new String[parts.length-(hasLevel)];
					System.arraycopy(parts, 0, choppedParts, 0, choppedParts.length);
					String nameSpace = String.join("_", choppedParts);
					if (!nameSpace.contains(":")) nameSpace = "minecraft:" + nameSpace;
					else if (!nameSpace.startsWith("minecraft:")) return null;
					if (!Key.parseable(nameSpace)) return null;
					RegistryKey<net.minestom.server.item.enchant.Enchantment> enchant = MinecraftServer.getEnchantmentRegistry().getKey(Key.key(nameSpace));
					if (enchant == null) return null;
					return new Enchantment(enchant, level);
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull Enchantment o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Enchantment o) {
					int level = o.level();
					return keyToString(o.enchantment().key()) + (level > 0 ? " " + level : "");
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Enchantment o) {
					Fields fields = new Fields();
					fields.putObject("id", o.enchantment().key().asString());
					fields.putPrimitive("level", o.level());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Enchantment o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Enchantment deserialize(@NotNull Fields f) throws StreamCorruptedException {
					String id = f.getObject("id", String.class);
					assert id != null;
					RegistryKey<net.minestom.server.item.enchant.Enchantment> enchantment = MinecraftServer.getEnchantmentRegistry().getKey(Key.key(id));
					if (enchantment == null) throw new StreamCorruptedException("Enchantment with id '" + id + "' was not found.");
					int level = f.getPrimitive("level", int.class);
					return new Enchantment(enchantment, level);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(Direction.class, "direction")
			.user("directions?")
			.name("Direction")
			.description("Represents a direction (north, south, east, west, up, down).")
			.examples("set {_dir} to north")
			.since("2.0")
			.defaultExpression(new SimpleLiteral<>(new Direction(new double[] {0, 0, 0}), true))
			.parser(new Parser<>() {
				@Override
				@Nullable
				public Direction parse(String s, final ParseContext context) {
					s = s.toUpperCase(Locale.ENGLISH);
					for (BlockFace blockFace : BlockFace.values()) {
						if (blockFace.name().equals(s)) return new Direction(blockFace.toDirection());
					}
					return null;
				}

				@Override
				public boolean canParse(final ParseContext context) {
					return true;
				}

				@Override
				public String toString(final Direction o, final int flags) {
					return o.toString();
				}

				@Override
				public String toVariableNameString(final Direction o) {
					return o.toString();
				}
			})
			.serializer(new YggdrasilSerializer<>()));
		Classes.registerClass(new ClassInfo<>(Sound.class, "sound")
			.user("sounds?")
			.name("Sound")
			.description("A sound with an id, seed, category, volume, and pitch.")
			.examples("play sound \"entity.player.levelup\" at player to all players")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Sound o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Sound o) {
					return "sound id: " + o.name().asString().toLowerCase(Locale.ENGLISH) + " category: " + Classes.toString(o.source()) +
						" seed: " + o.seed().orElse(0) + " volume: " + o.volume() + " pitch: " + o.pitch();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Sound o) {
					Fields fields = new Fields();
					fields.putObject("name", o.name().asString());
					fields.putObject("source", o.source().toString());
					fields.putPrimitive("seed", o.seed().orElse(0));
					fields.putPrimitive("volume", o.volume());
					fields.putPrimitive("pitch", o.pitch());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Sound o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Sound deserialize(@NotNull Fields f) throws StreamCorruptedException {
					String name = f.getObject("name", String.class);
					Sound.Source source = Sound.Source.valueOf(f.getObject("source", String.class));
					long seed = f.getPrimitive("seed", long.class);
					float volume = f.getPrimitive("volume", float.class);
					float pitch = f.getPrimitive("pitch", float.class);
					assert name != null;
					return Sound.sound()
						.type(Key.key(name))
						.source(source)
						.seed(seed)
						.volume(volume)
						.pitch(pitch)
						.build();
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(AmbientSounds.class, "ambientsound")
			.user("ambient ?sounds?")
			.name("Ambient Sounds")
			.description("An ambient sound with an id, mood, and additions.")
			.examples("set {_sounds} to new ambient sounds with loop \"minecraft:ambient.cave\"")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AmbientSounds o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AmbientSounds o) {
					StringBuilder sb = new StringBuilder("ambient sounds");
					SoundEvent loop = o.loop();
					if (loop != null) {
						sb.append(" loop: ");
						sb.append(loop.name().toLowerCase(Locale.ENGLISH));
					}
					AmbientSounds.Mood mood = o.mood();
					if (mood != null){
						sb.append(" mood: ");
						sb.append(Classes.toString(mood));
					}
					List<AmbientSounds.Additions> additions = o.additions();
					if (!additions.isEmpty()) {
						int endIndex = additions.size()-1;
						sb.append(" additions: ");
						for (AmbientSounds.Additions addition : additions) {
							sb.append(Classes.toString(addition));
							if (additions.indexOf(addition) != endIndex) sb.append(", ");
						}
					}
					return sb.toString();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull AmbientSounds o) {
					Fields fields = new Fields();
					SoundEvent loop = o.loop();
					if (loop != null) fields.putObject("loop", loop.key().asString());
					AmbientSounds.Mood mood = o.mood();
					if (mood != null) fields.putObject("mood", mood);
					List<AmbientSounds.Additions> additions = o.additions();
					if (!additions.isEmpty()) fields.putObject("additions", additions);
					return fields;
				}

				@Override
				public void deserialize(@NotNull AmbientSounds o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull AmbientSounds deserialize(@NotNull Fields f) throws StreamCorruptedException {
					SoundEvent loop = null;
					if (f.hasField("loop")) {
						String l = f.getObject("loop", String.class);
						loop = getSoundEvent(l);
					}
					AmbientSounds.Mood mood = null;
					if (f.hasField("mood")) mood = f.getObject("mood", AmbientSounds.Mood.class);
					List<AmbientSounds.Additions> additions = new ArrayList<>();
					if (f.hasField("additions")) additions = f.getObject("additions", List.class);
					assert additions != null;
					return new AmbientSounds(loop, mood, additions);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(AmbientSounds.Mood.class, "mood")
			.user("moods?")
			.name("Mood")
			.description("The mood for ambient sounds.")
			.examples("set {_mood} to mood(\"ambient.cave\", 1 second, 8, 2)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AmbientSounds.Mood o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AmbientSounds.Mood o) {
					return "mood sound: " + o.sound().name() + " delay: " + Classes.toString(timespanFrom(o.tickDelay()))
						+ " block search extent: " + o.blockSearchExtent() + " offset: " + o.offset();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull AmbientSounds.Mood o) {
					Fields fields = new Fields();
					fields.putObject("sound", o.sound().key().asString());
					fields.putPrimitive("delay", o.tickDelay());
					fields.putPrimitive("block-search-extent", o.blockSearchExtent());
					fields.putPrimitive("offset", o.offset());
					return fields;
				}

				@Override
				public void deserialize(@NotNull AmbientSounds.Mood o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull AmbientSounds.Mood deserialize(@NotNull Fields f) throws StreamCorruptedException {
					SoundEvent sound = getSoundEvent(f.getObject("sound", String.class));
					int delay = f.getPrimitive("delay", int.class);
					int blockSearchExtent = f.getPrimitive("block-search-extent", int.class);
					double offset = f.getPrimitive("offset", double.class);
					assert sound != null;
					return new AmbientSounds.Mood(sound, delay, blockSearchExtent, offset);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(AmbientSounds.Additions.class, "addition")
			.user("additions?")
			.name("Additions")
			.description("The additions for ambient sounds.")
			.examples("set {_additions} to additions(\"ambient.cave\", 0.01)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AmbientSounds.Additions o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AmbientSounds.Additions o) {
					return "additions sound: " + o.sound().name() + " tick chance: " + o.tickChance();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull AmbientSounds.Additions o) {
					Fields fields = new Fields();
					fields.putObject("sound", o.sound().key().asString());
					fields.putPrimitive("tick-chance", o.tickChance());
					return fields;
				}

				@Override
				public void deserialize(@NotNull AmbientSounds.Additions o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull AmbientSounds.Additions deserialize(@NotNull Fields f) throws StreamCorruptedException {
					SoundEvent sound = getSoundEvent(f.getObject("sound", String.class));
					double tickChance = f.getPrimitive("tick-chance", double.class);
					assert sound != null;
					return new AmbientSounds.Additions(sound, tickChance);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(BackgroundMusic.class, "backgroundmusic")
			.user("background ?musics?")
			.name("Background Music")
			.description("The background music.")
			.examples("set {_music} to new background music with music {_track}")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull BackgroundMusic o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull BackgroundMusic o) {
					return "background music music: " + Classes.toString(o.music()) + " creative: " + Classes.toString(o.creativeMusic())
						+ " underwater: " + Classes.toString(o.underwaterMusic());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull BackgroundMusic o) {
					Fields fields = new Fields();
					fields.putObject("music", o.music());
					fields.putObject("creative", o.creativeMusic());
					fields.putObject("underwater", o.underwaterMusic());
					return fields;
				}

				@Override
				public void deserialize(@NotNull BackgroundMusic o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull BackgroundMusic deserialize(@NotNull Fields f) throws StreamCorruptedException {
					Music music = null;
					if (f.hasField("music")) music = f.getObject("music", Music.class);
					Music creative = null;
					if (f.hasField("creative")) creative = f.getObject("creative", Music.class);
					Music underwater = null;
					if (f.hasField("underwater")) underwater = f.getObject("underwater", Music.class);
					return new BackgroundMusic(music, creative, underwater);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new ClassInfo<>(Music.class, "music")
			.user("musics?")
			.name("Music")
			.description("The regular/creative/underwater background music.")
			.examples("set {_music} to music(\"music.overworld\", 1 minute, 2 minutes, true)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Music o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Music o) {
					return "music id: " + o.sound().key().asString() + " min delay: " + Classes.toString(timespanFrom(o.minDelay()))
						+ " max delay: " + Classes.toString(timespanFrom(o.maxDelay())) + " replaces current music: " + o.replaceCurrentMusic();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull Music o) {
					Fields fields = new Fields();
					fields.putObject("sound", o.sound().key().asString());
					fields.putPrimitive("min-delay", o.minDelay());
					fields.putPrimitive("max-delay", o.maxDelay());
					fields.putPrimitive("replaces", o.replaceCurrentMusic());
					return fields;
				}

				@Override
				public void deserialize(@NotNull Music o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull Music deserialize(@NotNull Fields f) throws StreamCorruptedException {
					SoundEvent sound = getSoundEvent(f.getObject("sound", String.class));
					int minDelay = f.getPrimitive("min-delay", int.class);
					int maxDelay = f.getPrimitive("max-delay", int.class);
					boolean replaces = f.getPrimitive("replaces", boolean.class);
					assert sound != null;
					return new Music(sound, minDelay, maxDelay, replaces);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(EntityActivity.class, "entityactivity")
			.user("entity activit(y|ies)")
			.name("Entity Activity")
			.description("The activity state of an entity (in water, on ground, in air, etc.).")
			.defaultExpression(new EventValueExpression<>(EntityActivity.class)));
		Classes.registerClass(new EnumClassInfo<>(MoonPhase.class, "moonphase")
			.user("moon ?phases?")
			.name("Moon Phase")
			.description("The phase of the moon in a dimension.")
			.examples("set {_phase} to full moon"));
		Classes.registerClass(new ClassInfo<>(BedRule.class, "bedrule")
			.user("bed ?rules?")
			.name("Bed Rule")
			.description("The bed rule environment attribute.")
			.examples("set {_rule} to new bed rule with sleep rule monsters and with spawn rule spawn")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull BedRule o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull BedRule o) {
					return "bed rule can sleep: " + Classes.toString(o.canSleep()) + " can set spawn: " +
						Classes.toString(o.canSetSpawn()) + " explodes: " + o.explodes() + " error message: " + Classes.toString(o.errorMessage());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public @NotNull Fields serialize(@NotNull BedRule o) {
					Fields fields = new Fields();
					fields.putObject("sleep", o.canSleep());
					fields.putObject("spawn", o.canSetSpawn());
					fields.putPrimitive("explodes", o.explodes());
					fields.putObject("error-message", o.errorMessage());
					return fields;
				}

				@Override
				public void deserialize(@NotNull BedRule o, @NotNull Fields f) {
					assert false;
				}

				@Override
				protected @NotNull BedRule deserialize(@NotNull Fields f) throws StreamCorruptedException {
					BedRule.Rule sleep = f.getObject("sleep", BedRule.Rule.class);
					BedRule.Rule spawn = f.getObject("spawn", BedRule.Rule.class);
					boolean explodes = f.getPrimitive("explodes", boolean.class);
					Component errorMessage = f.getObject("error-message", Component.class);
					assert sleep != null;
					assert spawn != null;
					return new BedRule(sleep, spawn, explodes, errorMessage);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(BedRule.Rule.class, "bedrulerule")
			.user("bed ?rule ?rules?")
			.name("Bed Rule Rule")
			.description("A sleep or spawn rule used when creating a bed rule.")
			.examples("set {_rule} to new bed rule with sleep rule monsters and with spawn rule spawn")
			.defaultExpression(new EventValueExpression<>(BedRule.Rule.class)));
		Classes.registerClass(new EnumClassInfo<>(Sound.Source.class, "soundcategory")
			.user("sound ?categor(y|ies)")
			.name("Sound Category")
			.description("A sound category e.g. master")
			.examples("play sound \"entity.player.levelup\" at player to all players")
			.defaultExpression(new EventValueExpression<>(Sound.Source.class)));
		Classes.registerClass(new EnumClassInfo<>(AbstractDisplayMeta.BillboardConstraints.class, "billboardconstraint")
			.user("bill ?board ?constraints?")
			.name("Billboard Constraints")
			.description("Billboard constraint e.g. FIXED")
			.examples("set billboard render constraints of {_entity} to fixed")
			.defaultExpression(new EventValueExpression<>(AbstractDisplayMeta.BillboardConstraints.class)));
		Classes.registerClass(new ClassInfo<>(Attribute.class, "attributetype")
			.user("attribute ?types?")
			.name("Attribute Type")
			.description("Represents the type of an attribute.")
			.examples("set attack speed attribute of player to 2")
			.usage(Attribute.values().stream().map(attribute -> attribute.key().value()).collect(Collectors.joining(", ")))
			.parser(new Parser<>() {
				public Attribute parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH).replace(' ', '_');
					s = Utils.isPlural(s).updated();
					if (!s.contains("minecraft:")) s = "minecraft:" + s;
					if (!Key.parseable(s)) return null;
					return Attribute.fromKey(s);
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull Attribute o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Attribute o) {
					return keyToString(o.key());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public Fields serialize(Attribute o) throws NotSerializableException {
					Fields f = new Fields();
					f.putObject("type", o.key().asString());
					return f;
				}

				@Override
				public void deserialize(Attribute o, Fields f) throws StreamCorruptedException, NotSerializableException {
					assert false;
				}

				@SuppressWarnings("DataFlowIssue")
				@Override
				protected @NonNull Attribute deserialize(@NotNull Fields f) throws StreamCorruptedException {
					String type = f.getObject("type", String.class);
					return Attribute.fromKey(type);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			})
			.supplier(Attribute.values().toArray(new Attribute[0])));
		Classes.registerClass(new ClassInfo<>(NamedTextColor.class, "namedtextcolor")
			.user("named ?text ?colors?")
			.name("Named Text Color")
			.description("Team colors (dark red, dark aqua, etc.)")
			.examples("set {_color} to dark red")
			.usage(String.join(", ", NamedTextColor.NAMES.keys()))
			.parser(new Parser<>() {
				public NamedTextColor parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH).replace(' ', '_');
					for (NamedTextColor color : NamedTextColor.NAMES.values()) {
						if (color.toString().equals(s)) return color;
					}
					return null;
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull NamedTextColor o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull NamedTextColor o) {
					return typeFormatted(o.toString());
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public Fields serialize(NamedTextColor o) throws NotSerializableException {
					Fields f = new Fields();
					f.putPrimitive("color", o.value());
					return f;
				}

				@Override
				public void deserialize(NamedTextColor o, Fields f) throws StreamCorruptedException, NotSerializableException {
					assert false;
				}

				@SuppressWarnings("DataFlowIssue")
				@Override
				protected @NonNull NamedTextColor deserialize(@NotNull Fields f) throws StreamCorruptedException {
					int color = f.getPrimitive("color", int.class);;
					return NamedTextColor.namedColor(color);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			})
			.supplier(NamedTextColor.NAMES.values().toArray(new NamedTextColor[0])));
		Classes.registerClass(new ClassInfo<>(Color.class, "color")
			.user("colors?")
			.name("Color")
			.description("Color (outside of the team color range)")
			.examples("set {_color} to rgb(255, 0, 0)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull Color o, int flags) {
					// doesn't seem to work how I intended
					//return LegacyComponentSerializer.legacyAmpersand().serialize(Component.empty().color(TextColor.color(o.asRGB())).asComponent());
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Color o) {
					return "color r: " + o.red() + " g: " + o.green() + " b: " + o.blue();
				}
			}));
		Classes.registerClass(new ClassInfo<>(AlphaColor.class, "alphacolor")
			.user("alpha ?colors?")
			.name("Alpha Color")
			.description("Alpha Color (color with an alpha (transparency) value)")
			.examples("set background color of {-a} to rgb(255, 255, 255, 128)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AlphaColor o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AlphaColor o) {
					return "alpha color r: " + o.red() + " g: " + o.green() + " b: " + o.blue() + " alpha: " + o.alpha();
				}
			}));
		Classes.registerClass(new EnumClassInfo<>(ItemDisplayMeta.DisplayContext.class, "displaycontext")
			.user("display ?contexts?")
			.name("Item Display Context")
			.description("The context in which an item display is rendered (e.g. GUI)")
			.examples("set display context of {_entity} to head")
			.defaultExpression(new EventValueExpression<>(ItemDisplayMeta.DisplayContext.class)));
		Classes.registerClass(new EnumClassInfo<>(TextDisplayMeta.Alignment.class, "textalignment")
			.user("textalignments?")
			.name("Text Alignment")
			.description("The text alignment of a text display (center, left, or right)")
			.examples("set text alignment of {_entity} to center")
			.defaultExpression(new EventValueExpression<>(TextDisplayMeta.Alignment.class)));
		Classes.registerClass(new EnumClassInfo<>(EntityAnimationPacket.Animation.class, "animation")
			.user("animations?")
			.name("Entity Animation")
			.description("An animation that an entity can play (main hand swing, leave bed, etc.)")
			.examples("play swing main arm animation on player")
			.defaultExpression(new EventValueExpression<>(EntityAnimationPacket.Animation.class)));
		Classes.registerClass(new EnumClassInfo<>(NBTUtils.TagType.class, "tagtype")
			.user("tag ?types?")
			.name("NBT Tag Type")
			.description("The tag type of an nbt tag (e.g. int array)")
			.examples("set {_tag} to string nbt tag \"CustomName\" of {_nbt}")
			.defaultExpression(new EventValueExpression<>(NBTUtils.TagType.class)));
		Classes.registerClass(new EnumClassInfo<>(ServerListPingType.class, "pingtype")
			.user("ping ?types?")
			.name("Server List Ping Type")
			.description("The ping type of a ServerListPing event")
			.examples("on server list ping:\n\tif ping type is modern full:")
			.defaultExpression(new EventValueExpression<>(ServerListPingType.class)));
		Classes.registerClass(new EnumClassInfo<>(InputKey.class, "inputkey")
			.user("input ?keys?")
			.name("Input Key")
			.description("Represents a movement input key that is pressed by a player.")
			.examples("broadcast \"%current input keys of player%\"")
			.defaultExpression(new EventValueExpression<>(InputKey.class)));
		Classes.registerClass(new EnumClassInfo<>(ClientSettings.ParticleSetting.class, "particlesetting")
			.user("particle ?settings?")
			.name("Particle Setting")
			.description("The setting the player has set for their particles.")
			.examples("set particle setting of player to all")
			.defaultExpression(new EventValueExpression<>(ClientSettings.ParticleSetting.class)));
		Classes.registerClass(new EnumClassInfo<>(ParrotType.class, "parrottype")
			.user("parrot ?types?")
			.name("Parrot Type")
			.description("The type of a parrot that can sit on a player's shoulder.")
			.examples("set left shoulder parrot type of player to red parrot")
			.defaultExpression(new EventValueExpression<>(ParrotType.class)));
		Classes.registerClass(new EnumClassInfo<>(DimensionType.Skybox.class, "skybox")
			.user("sky ?box(es)?")
			.name("Dimension Skybox")
			.description("The visual skybox of a dimension type.")
			.examples("""
				create dimension under "test:lobby" stored in {_d}:
					skybox: end""")
			.defaultExpression(new EventValueExpression<>(DimensionType.Skybox.class)));
		Classes.registerClass(new EnumClassInfo<>(DimensionType.CardinalLight.class, "cardinallight")
			.user("cardinal ?lights?")
			.name("Dimension Cardinal Light")
			.description("The cardinal light of a dimension type.")
			.examples("""
				create dimension under "test:lobby" stored in {_d}:
					cardinal light: nether""")
			.defaultExpression(new EventValueExpression<>(DimensionType.CardinalLight.class)));
		Classes.registerClass(new EnumClassInfo<>(Biome.TemperatureModifier.class, "temperaturemodifier")
			.user("temperature ?modifiers?")
			.name("Biome Temperature Modifier")
			.description("The temperature modifier of a biome.")
			.examples("""
				create biome under "test:lobby" stored in {_b}:
					temperature modifier: frozen""")
			.defaultExpression(new EventValueExpression<>(Biome.TemperatureModifier.class)));
		Classes.registerClass(new EnumClassInfo<>(BiomeEffects.GrassColorModifier.class, "grasscolormodifier")
			.user("grass ?color ?modifiers?")
			.name("Biome Grass Color Modifier")
			.description("The grass color modifier of a biome.")
			.examples("""
				create biome under "test:lobby" stored in {_b}:
					grass color modifier: dark forest""")
			.defaultExpression(new EventValueExpression<>(BiomeEffects.GrassColorModifier.class)));
		Classes.registerClass(new ClassInfo<>(BufferedImage.class, "bufferedimage")
			.user("buffered ?images?")
			.name("Buffered Image")
			.description("A raster image loaded from a file.")
			.examples("""
				on server list ping:
					set motd favicon to image from file "favicon.png\"""")
			.defaultExpression(new EventValueExpression<>(BufferedImage.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull BufferedImage o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull BufferedImage o) {
					return "buffered image";
				}
			}));
		Classes.registerClass(new ClassInfo<>(Particle.class, "particle")
			.user("particles?")
			.name("Particle")
			.description("Particle (e.g. dust)")
			.examples("draw 10 of flame at player for player")
			.usage(Particle.values().stream().map(particle -> particle.key().value()).collect(Collectors.joining(", ")))
			.parser(new Parser<>() {
				public Particle parse(@NotNull String s, @NotNull ParseContext context) {
					s = s.toLowerCase(Locale.ENGLISH).replace(' ', '_');
					if (!s.contains("minecraft:")) s = "minecraft:" + s;
					if (!Key.parseable(s)) return null;
					return Particle.fromKey(s);
				}

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return true;
				}

				@Override
				public @NotNull String toString(@NotNull Particle o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull Particle o) {
					return keyToString(o.key());
				}
			})
			.supplier(Particle.values().toArray(new Particle[0])));
		Classes.registerClass(new ClassInfo<>(AmbientParticle.class, "ambientparticle")
			.user("ambient ?particles?")
			.name("Ambient Particle")
			.description("Particle (e.g. dust) with a probability of spawning")
			.examples("set {_particle} to ambientParticle(white ash, 0.118093334)")
			.parser(new Parser<>() {

				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull AmbientParticle o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull AmbientParticle o) {
					return "ambient particle particle: " + Classes.toString(o.particle()) + " probability: " + o.probability();
				}
			}));
		Classes.registerClass(new ClassInfo<>(DustOption.class, "dustoption")
			.user("dust ?options?")
			.name("Dust Option")
			.description("Dust options for the dust particle")
			.examples("set {_data} to dustOption(red, 1)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull DustOption o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull DustOption o) {
					return "dust options color: " + Classes.toString(o.color()) + " scale: " + o.scale();
				}
			}));
		Classes.registerClass(new ClassInfo<>(DustTransition.class, "dusttransition")
			.user("dust ?transitions?")
			.name("Dust Transition")
			.description("Dust options for the dust color transition particle")
			.examples("set {_data} to dustTransition(red, blue, 1)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull DustTransition o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull DustTransition o) {
					return "dust transition original color: " + Classes.toString(o.color()) + " to color: " + Classes.toString(o.transitionColor())
						+ " scale: " + o.scale();
				}
			}));
		Classes.registerClass(new ClassInfo<>(EffectData.class, "effectdata")
			.user("effect ?datas?")
			.name("Effect Data")
			.description("Effect data options for the effect and instance effect particles.")
			.examples("set {_data} to effectData(red, 1)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull EffectData o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull EffectData o) {
					return "effect data options color: " + Classes.toString(o.color()) + " power: " + o.power();
				}
			}));
		Classes.registerClass(new ClassInfo<>(TrailData.class, "traildata")
			.user("trail ?datas?")
			.name("Trail Data")
			.description("Trail data options for the trail particle")
			.examples("set {_data} to trailData(point(0, 64, 0), red, 5 seconds)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull TrailData o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull TrailData o) {
					return "trail data options target: " + Classes.toString(o.target()) + " color: " + Classes.toString(o.color())
						+ " duration: " + Classes.toString(timespanFrom(o.duration()));
				}
			}));
		Classes.registerClass(new ClassInfo<>(VibrationData.class, "vibrationdata")
			.user("vibration ?datas?")
			.name("Vibration Data")
			.description("Vibration data options for the vibration particle")
			.examples("set {_data} to entityVibrationData(player, 5 seconds)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull VibrationData o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull VibrationData o) {
					return "vibration data options type: " + o.sourceType().name().toLowerCase(Locale.ENGLISH) + " block: " + Classes.toString(o.sourceBlock())
						+ " entity id: " + o.sourceEntityId() + " entity eye height: " + o.sourceEntityEyeHeight()
						+ " travel time: " + Classes.toString(timespanFrom(o.travelTicks()));
				}
			}));
		Classes.registerClass(new ClassInfo<>(RGBLike.class, "rgblike")
			.user("rgb ?likes?")
			.name("RGB Like (Color)")
			.description("Essentially a color")
			.examples("set {_data} to dustOption(red, 1)")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull RGBLike o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull RGBLike o) {
					return "r: " + o.red() + " g: " + o.green() + " b: " + o.blue();
				}
			}));
		Classes.registerClass(new ClassInfo<>(PlayerSkin.class, "skin")
			.user("skins?")
			.name("Skin")
			.description("A skin with textures and a signature")
			.examples("set skin of player to skin from \"jeb_\"")
			.defaultExpression(new EventValueExpression<>(PlayerSkin.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull PlayerSkin o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull PlayerSkin o) {
					return "skin textures: " + o.textures() + " signature: " + o.signature();
				}
			})
			.serializer(new Serializer<>() {
				@Override
				public Fields serialize(PlayerSkin o) throws NotSerializableException {
					Fields f = new Fields();
					f.putObject("textures", o.textures());
					f.putObject("signature", o.signature());
					return f;
				}

				@Override
				public void deserialize(PlayerSkin o, Fields f) throws StreamCorruptedException, NotSerializableException {
					assert false;
				}

				@Override
				protected PlayerSkin deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
					String textures = fields.getObject("textures", String.class);
					String signature = fields.getObject("signature", String.class);
					return new PlayerSkin(textures, signature);
				}

				@Override
				public boolean mustSyncDeserialization() {
					return false;
				}

				@Override
				protected boolean canBeInstantiated() {
					return false;
				}
			}));
		// no serializer for nbtcompound because they should be saving it to a file
		Classes.registerClass(new ClassInfo<>(NBTCompound.class, "nbtcompound")
			.user("nbt ?compounds?")
			.name("NBT Compound")
			.description("A compound (e.g. {test:1b,hello:\"hi\"}")
			.examples("set {_nbt} to nbt compound of player's tool")
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull NBTCompound o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull NBTCompound o) {
					try {
						return NBTUtils.asString(o.getCompound());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			})
			.changer(new Changer<>() {
				@Override
				public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
					return switch (mode) {
						case ADD, REMOVE -> CollectionUtils.array(NBTCompound[].class);
						case SET, RESET, DELETE -> CollectionUtils.array(NBTCompound.class);
						default -> null;
					};
				}

				@Override
				public void change(NBTCompound[] what, @Nullable Object[] delta, ChangeMode mode) {
					if (mode != ChangeMode.ADD && mode != ChangeMode.REMOVE) {
						for (NBTCompound compound : what) {
							switch (mode) {
								case DELETE, RESET -> compound.update((_) -> CompoundBinaryTag.empty());
								case SET -> {
									if (delta.length == 0 || delta[0] == null) continue;
									compound.update((_) -> ((NBTCompound) delta[0]).getCompound());
								}
							}
						}
						return;
					}
					for (Object o : delta) {
						if (!(o instanceof NBTCompound oCompound)) continue;
						CompoundBinaryTag oCompoundTag = oCompound.getCompound();
						for (NBTCompound compound : what) {
							if (mode == ChangeMode.ADD) compound.update((c) -> NBTUtils.deepMerge(c, oCompoundTag));
							else compound.update((c) -> NBTUtils.deepMerge(c, oCompoundTag, true));
						}
					}
				}
			}));

		Classes.registerClass(new ClassInfo<>(MinecraftTag.class, "minecrafttag")
			.user("minecraft ?tags?")
			.name("Minecraft Tag")
			.description("""
				A tag used to group items, blocks or entity types, such as 'minecraft:logs'.
				Tags have a namespace and a value, written as "namespace:value". Obtain them with the 'tag' expression.""")
			.usage("<namespace>:<value>")
			.examples("minecraft tag \"logs\"")
			.defaultExpression(new EventValueExpression<>(MinecraftTag.class))
			.parser(new Parser<>() {
				@Override
				public boolean canParse(@NotNull ParseContext context) {
					return false;
				}

				@Override
				public @NotNull String toString(@NotNull MinecraftTag o, int flags) {
					return toVariableNameString(o);
				}

				@Override
				public @NotNull String toVariableNameString(@NotNull MinecraftTag o) {
					return o.type().getName() + " tag " + o.key().asString();
				}
			}));

		/*
		 * Converters
		 */
		Converters.registerConverter(String.class, ComponentWrapper.class, from -> new ComponentWrapper(Component.text(from)));
		Converters.registerConverter(ComponentWrapper.class, String.class, from -> BASIC_MINI_MESSAGE.serialize(from.getComponent()));
		Converters.registerConverter(CommandSender.class, Player.class, from -> {
			if (from instanceof Player player) return player;
			return null;
		});
		Converters.registerConverter(CommandSender.class, ConsoleSender.class, from -> {
			if (from instanceof ConsoleSender sender) return sender;
			return null;
		});
		//Converters.registerConverter(Player.class, EntityType.class, _ -> EntityType.PLAYER);
		Converters.registerConverter(Entity.class, LivingEntity.class, from -> {
			if (from instanceof LivingEntity livingEntity) return livingEntity;
			return null;
		});
		Converters.registerConverter(EquipmentHandler.class, LivingEntity.class, from -> {
			if (from instanceof LivingEntity livingEntity) return livingEntity;
			return null;
		});
		Converters.registerConverter(Entity.class, EntityCreature.class, from -> {
			if (from instanceof EntityCreature entityCreature) return entityCreature;
			return null;
		});
		Converters.registerConverter(Entity.class, Player.class, from -> {
			if (from instanceof Player player) return player;
			return null;
		});
		Converters.registerConverter(Entity.class, Pos.class, Entity::getPosition);
		Converters.registerConverter(Point.class, Pos.class, from -> {
			if (from instanceof Pos pos) return pos;
			return null;
		});
		Converters.registerConverter(Point.class, Vec.class, from -> {
			if (from instanceof Vec vec) return vec;
			return null;
		});
		Converters.registerConverter(Point.class, BlockVec.class, from -> {
			if (from instanceof BlockVec blockVec) return blockVec;
			return null;
		});
		Converters.registerConverter(Instance.class, InstanceContainer.class, from -> {
			if (from instanceof InstanceContainer container) return container;
			return null;
		});
		Converters.registerConverter(Instance.class, SharedInstance.class, from -> {
			if (from instanceof SharedInstance shared) return shared;
			return null;
		});
		Converters.registerConverter(Player.class, AbstractInventory.class, Player::getInventory);
		Converters.registerConverter(Vec.class, Direction.class, Direction::new);
		Converters.registerConverter(Direction.class, Vec.class, Direction::getDirection);
		Converters.registerConverter(Player.class, PlayerSkin.class, Player::getSkin);
		Converters.registerConverter(RGBLike.class, Color.class, from -> {
			if (from instanceof Color color) return color;
			return new Color(from.red(), from.green(), from.blue());
		});
		Converters.registerConverter(RGBLike.class, NamedTextColor.class, from -> {
			if (from instanceof NamedTextColor color) return color;
			return null;
		});
		Converters.registerConverter(ComponentLike.class, ComponentWrapper.class, from -> {
			if (from instanceof Component c) return new ComponentWrapper(c);
			return null;
		});
		Converters.registerConverter(Color.class, AlphaColor.class, from -> from.withAlpha(255));
		Converters.registerConverter(Item.class, Block.class, from -> from.getItem().material().block());
		Converters.registerConverter(Block.class, Item.class, from -> {
			Material material = from.material();
			if (material == null) return null;
			return new Item(ItemStack.of(material));
		});

		// unsure if these are necessary
		Converters.registerConverter(ItemDisplayMeta.DisplayContext.class, ItemAnimation.class, from -> {
			if (from == ItemDisplayMeta.DisplayContext.NONE) return ItemAnimation.NONE;
			return null;
		});
		Converters.registerConverter(ItemAnimation.class, DimensionType.Skybox.class, from -> {
			if (from == ItemAnimation.NONE) return DimensionType.Skybox.NONE;
			return null;
		});
		Converters.registerConverter(DimensionType.Skybox.class, Biome.TemperatureModifier.class, from -> {
			if (from == DimensionType.Skybox.NONE) return Biome.TemperatureModifier.NONE;
			return null;
		});
		Converters.registerConverter(Biome.TemperatureModifier.class, BiomeEffects.GrassColorModifier.class, from -> {
			if (from == Biome.TemperatureModifier.NONE) return BiomeEffects.GrassColorModifier.NONE;
			return null;
		});

		/*
		 *	Comparators
		 */
		Comparators.registerComparator(ComponentWrapper.class, ComponentWrapper.class, (o1, o2) -> {
			String s1 = BASIC_MINI_MESSAGE.serialize(o1.getComponent());
			String s2 = BASIC_MINI_MESSAGE.serialize(o2.getComponent());
			return Comparators.compare(s1, s2);
		});
		Comparators.registerComparator(CommandSender.class, EntityType.class, (o1, o2) -> {
			if (!(o1 instanceof Player)) return Relation.get(false);
			return Relation.get(o2.equals(EntityType.PLAYER));
		});
		Comparators.registerComparator(EntityType.class, Player.class, (o1, o2) -> {
			if (o1 == EntityType.PLAYER) return Relation.EQUAL;
			return Relation.NOT_EQUAL;
		});
		Comparators.registerComparator(Entity.class, EntityType.class, (o1, o2) -> Relation.get(o1.getEntityType().equals(o2)));
		//Comparators.registerComparator(Item.class, Slot.class, (o1, o2) -> Relation.get(o1.getItem().isSimilar(o2.getItem())));
		Comparators.registerComparator(Item.class, Item.class, (o1, o2) -> Relation.get(o1.getItem().equals(o2.getItem())));
		Comparators.registerComparator(Block.class, Block.class, (o1, o2) -> Relation.get(o1.compare(o2)));
		Comparators.registerComparator(MinecraftTag.class, MinecraftTag.class, (o1, o2) -> Relation.get(o1.equals(o2)));
		Comparators.registerComparator(Item.class, Block.class, (o1, o2) -> {
			ItemStack item = o1.getItem();
			Material material = item.material();
			ItemStack basicItemVersion = material == Material.AIR ? ItemStack.AIR : ItemStack.of(material);
			Block materialBlock = material == Material.AIR ? Block.AIR : material.block();
			if (item.equals(basicItemVersion) && o2.equals(materialBlock)) return Relation.EQUAL;
			return Relation.NOT_EQUAL;
		});

		/*
		 *	Arithmetic
		 */
		Arithmetics.registerOperation(Operator.ADDITION, Vec.class, Vec::add);
		Arithmetics.registerOperation(Operator.SUBTRACTION, Vec.class, Vec::sub);
		Arithmetics.registerOperation(Operator.MULTIPLICATION, Vec.class, Vec::mul);
		Arithmetics.registerOperation(Operator.DIVISION, Vec.class, Vec::div);
		Arithmetics.registerOperation(Operator.ADDITION, Vec.class, Number.class, (vec, num) -> vec.add(num.doubleValue()));
		Arithmetics.registerOperation(Operator.SUBTRACTION, Vec.class, Number.class, (vec, num) -> vec.sub(num.doubleValue()));
		Arithmetics.registerOperation(Operator.MULTIPLICATION, Vec.class, Number.class, (vec, num) -> vec.mul(num.doubleValue()));
		Arithmetics.registerOperation(Operator.DIVISION, Vec.class, Number.class, (vec, num) -> vec.div(num.doubleValue()));

		/*
		 *	Variable Intermediaries
		 */
		Variables.registerVariableSetIntermediary(Slot.class, Item::copy);

		/*
		 *	Variable Converters
		 */
		Variables.registerVariableConverter(Player.class, player -> {
			if (SkriptConfig.enablePlayerVariableFix.value() && player.isRemoved() && player.isOnline())
				return MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player.getUuid());
			return player;
		});
	}

	private static void inventoryChange(Object[] delta, Changer.ChangeMode mode, AbstractInventory inventory) {
		if (mode == Changer.ChangeMode.DELETE) {
			inventory.clear();
			return;
		}
		for (Object o : delta) {
			assert o != null;
			Item[] items;
			if (o instanceof Item item) items = new Item[]{item};
			else if (o instanceof AbstractInventory inv) items = Item.from(inv.getItemStacks());
			else continue; // only accepting inventories and itemstacks
			for (Item item : items) {
				if (item == null) continue;
				ItemStack internalStack = item.getItem();
				switch (mode) {
					case REMOVE_ALL -> {
						ItemStack[] stacks = inventory.getItemStacks();
						for (int i = 0; i < stacks.length; i++) {
							ItemStack itemStack = stacks[i];
							if (itemStack.isSimilar(internalStack)) stacks[i] = ItemStack.AIR;
						}
						inventory.copyContents(stacks);
					}
					case REMOVE -> {
						ItemStack[] stacks = inventory.getItemStacks();
						for (int i = 0; i < stacks.length; i++) {
							ItemStack itemStack = stacks[i];
							if (itemStack.isSimilar(internalStack)) {
								stacks[i] = itemStack.withAmount(itemStack.amount() - internalStack.amount());
								break;
							}
						}
						inventory.copyContents(stacks);
					}
					case ADD -> inventory.addItemStack(internalStack);
				}
			}
		}
	}

	private static String keyToString(Key key) {
		return typeFormatted(key.asString());
	}

	private static String typeFormatted(String string) {
		return string.toLowerCase(Locale.ENGLISH).replace("minecraft:", "").replace('_', ' ');
	}


}
