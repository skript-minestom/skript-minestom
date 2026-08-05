package ch.njol.skript.util;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public final class TagType<T extends RegistryKey<T>> {

	public static final TagType<Material> ITEMS =
		new TagType<>("item", "item", Registries::material, material -> new Item(ItemStack.of(material)));
	public static final TagType<Block> BLOCKS =
		new TagType<>("block", "block", Registries::blocks, block -> block);
	public static final TagType<EntityType> ENTITIES =
		new TagType<>("entity [type]", "entity type", Registries::entityType, entityType -> entityType);

	private static final List<TagType<?>> TYPES = List.of(ITEMS, BLOCKS, ENTITIES);

	private final String pattern;
	private final String name;
	private final Function<Registries, Registry<T>> registry;
	private final Function<T, ?> toSkript;

	private TagType(String pattern, String name, Function<Registries, Registry<T>> registry, Function<T, ?> toSkript) {
		this.pattern = pattern;
		this.name = name;
		this.registry = registry;
		this.toSkript = toSkript;
	}

	public Registry<T> getRegistry() {
		return registry.apply(MinecraftServer.getRegistries());
	}

	public String getName() {
		return name;
	}

	public Object toSkript(T value) {
		return toSkript.apply(value);
	}

	@Override
	public String toString() {
		return name;
	}

	@Nullable
	public static RegistryKey<?> keyOf(Object value) {
		if (value instanceof Item item) return item.getItem().material();
		if (value instanceof Block block) return block;
		if (value instanceof EntityType entityType) return entityType;
		if (value instanceof Entity entity) return entity.getEntityType();
		return null;
	}

	public static List<TagType<?>> getTypes() {
		return TYPES;
	}

	public static TagType<?> getType(int index) {
		return TYPES.get(index);
	}

	public static String getFullPattern() {
		StringBuilder builder = new StringBuilder("(");
		for (int i = 0; i < TYPES.size(); i++) {
			if (i != 0) builder.append('|');
			builder.append(i + 1).append(':').append(TYPES.get(i).pattern);
		}
		return builder.append(')').toString();
	}

}
