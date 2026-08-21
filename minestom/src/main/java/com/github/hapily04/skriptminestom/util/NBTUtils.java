package com.github.hapily04.skriptminestom.util;

import ch.njol.skript.util.Item;
import ch.njol.skript.util.NBTCompound;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.Taggable;
import org.eclipse.jdt.annotation.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static com.github.hapily04.skriptminestom.util.ArrayUtils.*;

public class NBTUtils {

	public static CompoundBinaryTag deepMerge(CompoundBinaryTag base, CompoundBinaryTag incoming) {
		CompoundBinaryTag.Builder out = CompoundBinaryTag.builder();
		out.put(base);
		for (String key : incoming.keySet()) {
			BinaryTag in = incoming.get(key);
			if (in == null) continue;
			mergeValue(out, base, key, in);
		}
		return out.build();
	}

	public static CompoundBinaryTag deepMerge(CompoundBinaryTag base, CompoundBinaryTag incoming, boolean remove) {
		if (!remove) {
			return deepMerge(base, incoming); // your existing merge
		}

		CompoundBinaryTag.Builder out = CompoundBinaryTag.builder();
		out.put(base);

		for (String key : incoming.keySet()) {
			BinaryTag in = incoming.get(key);
			if (in == null) continue;

			BinaryTag b = base.get(key);
			if (b == null) continue;

			// compound → recurse
			if (b instanceof CompoundBinaryTag bc && in instanceof CompoundBinaryTag ic) {
				CompoundBinaryTag result = deepMerge(bc, ic, true);
				if (result.keySet().isEmpty()) {
					out.remove(key);
				} else {
					out.put(key, result);
				}
			}

			// list → remove matching elements
			else if (b instanceof ListBinaryTag bl && in instanceof ListBinaryTag il) {
				out.put(key, subtractLists(bl, il));
			}

			// primitive → remove key
			else {
				out.remove(key);
			}
		}

		return out.build();
	}

	public static String asString(CompoundBinaryTag compound) throws IOException {
		return TagStringIO.tagStringIO().asString(compound);
	}

	public static CompoundBinaryTag asCompound(String s) throws IOException {
		return TagStringIO.tagStringIO().asCompound(s);
	}

	public static <T> T getTagOrElse(Taggable taggable, Tag<T> tag, T other) {
		T object = taggable.getTag(tag);
		return object == null ? other : object;
	}

	public static BinaryTag getNestedTag(CompoundBinaryTag compound, String input) {
		String[] split = input.split(";");
		for (int i = 0; i < split.length-1; i++) {
			String s = split[i];
			if (!compound.contains(s, BinaryTagTypes.COMPOUND)) return null;
			compound = compound.getCompound(s);
		}
		String lastTag = split[split.length-1];
		if (!compound.contains(lastTag)) return null;
		return compound.get(lastTag);
	}

	// chatgpt
	public static CompoundBinaryTag setNestedTag(CompoundBinaryTag root, String path, @Nullable BinaryTag newValue) {
		String[] keys = path.split(";");
		if (keys.length == 0) return root;

		// Single key (fast path)
		if (keys.length == 1) {
			CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
			for (String key : root.keySet()) {
				BinaryTag tag = root.get(key);
				if (tag != null && !key.equals(keys[0])) builder.put(key, tag);
			}
			if (newValue != null) builder.put(keys[0], newValue);
			return builder.build();
		}

		Deque<CompoundBinaryTag> parents = new ArrayDeque<>();
		CompoundBinaryTag current = root;

		// Walk to parent of leaf
		for (int i = 0; i < keys.length - 1; i++) {
			parents.push(current);
			if (current.contains(keys[i], BinaryTagTypes.COMPOUND)) {
				current = current.getCompound(keys[i]);
			} else {
				current = CompoundBinaryTag.empty();
			}
		}

		// Update only the leaf key while preserving siblings.
		CompoundBinaryTag.Builder leafBuilder = CompoundBinaryTag.builder();
		for (String key : current.keySet()) {
			BinaryTag tag = current.get(key);
			if (tag != null && !key.equals(keys[keys.length - 1])) leafBuilder.put(key, tag);
		}
		if (newValue != null) leafBuilder.put(keys[keys.length - 1], newValue);
		CompoundBinaryTag modified = leafBuilder.build();

		// Rebuild upward
		for (int i = keys.length - 2; i >= 0; i--) {
			CompoundBinaryTag parent = parents.pop();
			CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

			for (String key : parent.keySet()) {
				BinaryTag tag = parent.get(key);
				if (tag != null && !key.equals(keys[i])) builder.put(key, tag);
			}

			if (!modified.isEmpty()) builder.put(keys[i], modified);
			modified = builder.build();
		}

		return modified;
	}

	public static boolean isItemComponentKey(String key) {
		if (key.contains("/")) return false; // all entity data components contain a slash
		String keyString = key.startsWith("minecraft:") ? key : ("minecraft:" + key);
		if (!Key.parseable(keyString)) return false;
		return DataComponent.fromKey(keyString) != null;
	}

	private static ListBinaryTag subtractLists(ListBinaryTag base,
											   ListBinaryTag incoming) {

		List<BinaryTag> result = new ArrayList<>();

		for (BinaryTag element : base) {
			boolean shouldRemove = false;

			for (BinaryTag remove : incoming) {
				if (element.equals(remove)) {
					shouldRemove = true;
					break;
				}
			}

			if (!shouldRemove) {
				result.add(element);
			}
		}

		return ListBinaryTag.listBinaryTag(base.elementType(), result);
	}

	private static void mergeValue(CompoundBinaryTag.Builder out, CompoundBinaryTag base, String key, BinaryTag in) {
		BinaryTag b = base.get(key);
		if (b instanceof CompoundBinaryTag bc && in instanceof CompoundBinaryTag ic) out.put(key, deepMerge(bc, ic));
		else if (b instanceof ListBinaryTag bl && in instanceof ListBinaryTag il) out.put(key, appendLists(bl, il));
		else out.put(key, in);
	}

	private static ListBinaryTag appendLists(ListBinaryTag base, ListBinaryTag incoming) {
		BinaryTagType<? extends BinaryTag> type = !base.isEmpty() ? base.elementType() : incoming.elementType();
		List<BinaryTag> all = new ArrayList<>(base.size() + incoming.size());
		for (BinaryTag e : base) {
			all.add(e);
		}
		for (BinaryTag e : incoming) {
			all.add(e);
		}
		return ListBinaryTag.listBinaryTag(type, all);
	}

	public static NBTCompound getNBTCompound(Item item, boolean custom) {
		ItemStack internalItem = item.getItem();
		CompoundBinaryTag compoundBinaryTag;
		if (custom) {
			CustomData customData = internalItem.get(DataComponents.CUSTOM_DATA);
			compoundBinaryTag = customData != null ? customData.nbt() : CompoundBinaryTag.empty();
		} else {
			CompoundBinaryTag itemNBT = internalItem.toItemNBT(MinecraftServer.getRegistries());
			compoundBinaryTag = itemNBT.contains("components") ? itemNBT.getCompound("components") : CompoundBinaryTag.empty();
		}
		return new NBTCompound(compoundBinaryTag, item, custom);
	}

	public enum TagType {

		BYTE(BinaryTagTypes.BYTE, Byte.class, o -> ByteBinaryTag.byteBinaryTag((Byte) o), t -> ((ByteBinaryTag) t).value()),
		BOOLEAN(BinaryTagTypes.BYTE, Boolean.class, o -> {
			Boolean b = (Boolean) o;
			if (b) return ByteBinaryTag.ONE;
			return ByteBinaryTag.ZERO;
		}, t -> {
			byte b = ((ByteBinaryTag) t).value();
			if (b == 0) return false;
			else if (b == 1) return true;
			return null;
		}),
		SHORT(BinaryTagTypes.SHORT, Short.class, o -> ByteBinaryTag.byteBinaryTag((Byte) o), t -> ((ShortBinaryTag) t).value()),
		INT(BinaryTagTypes.INT, Integer.class, o -> IntBinaryTag.intBinaryTag((Integer) o), t -> ((IntBinaryTag) t).value()),
		LONG(BinaryTagTypes.LONG, Long.class, o -> LongBinaryTag.longBinaryTag((Long) o), t -> ((LongBinaryTag) t).value()),
		FLOAT(BinaryTagTypes.FLOAT, Float.class, o -> FloatBinaryTag.floatBinaryTag((Float) o), t -> ((FloatBinaryTag) t).value()),
		DOUBLE(BinaryTagTypes.DOUBLE, Double.class, o -> DoubleBinaryTag.doubleBinaryTag((Double) o), t -> ((DoubleBinaryTag) t).value()),
		BYTE_ARRAY(BinaryTagTypes.BYTE_ARRAY, Byte[].class, o -> ByteArrayBinaryTag.byteArrayBinaryTag(toByteArray((Object[]) o)), t -> toByteArray(((ByteArrayBinaryTag) t).value())),
		INT_ARRAY(BinaryTagTypes.INT_ARRAY, Integer[].class, o -> IntArrayBinaryTag.intArrayBinaryTag(toIntArray((Object[]) o)), t -> toIntegerArray(((IntArrayBinaryTag) t).value())),
		LONG_ARRAY(BinaryTagTypes.LONG_ARRAY, Long[].class, o -> LongArrayBinaryTag.longArrayBinaryTag(toLongArray((Object[]) o)), t -> toLongArray(((LongArrayBinaryTag) t).value())),
		STRING(BinaryTagTypes.STRING, String.class, o -> StringBinaryTag.stringBinaryTag((String) o), t -> ((StringBinaryTag) t).value()),
		COMPOUND(BinaryTagTypes.COMPOUND, NBTCompound.class, o -> ((NBTCompound) o).getCompound(), t -> new NBTCompound((CompoundBinaryTag) t, false)),
		//UUID(BinaryTagTypes.INT_ARRAY, String.class, o -> IntArrayBinaryTag.intArrayBinaryTag(NumberUtils.toNBTIntArray(java.util.UUID.fromString((String) o))), t -> NumberUtils.fromNBTIntArray(((IntArrayBinaryTag) t).value()).toString()),
		LIST(BinaryTagTypes.LIST, Object[].class, o -> {
			ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.heterogeneousListBinaryTag();
			for (Object object : (Object[]) o) {
				builder.add(TagType.convertFromSkript(object));
			}
			return builder.build();
		}, t -> {
			ListBinaryTag listBinaryTag = (ListBinaryTag) t;
			List<Object> objects = new ArrayList<>(listBinaryTag.size());
			for (BinaryTag tag : listBinaryTag) {
				objects.add(TagType.convertToSkript(tag));
			}
			return objects.toArray();
		});

		private final BinaryTagType<?> expectedBinaryTag;
		private final Class<?> skriptCompatibleClass;
		private final Function<Object, BinaryTag> objectConverter;
		private final Function<BinaryTag, Object> binaryTagConverter;

		TagType(BinaryTagType<?> expectedBinaryTag, Class<?> skriptCompatibleClass, Function<Object, BinaryTag> objectConverter,
				Function<BinaryTag, Object> binaryTagConverter) {
			this.expectedBinaryTag = expectedBinaryTag;
			this.skriptCompatibleClass = skriptCompatibleClass;
			this.objectConverter = objectConverter;
			this.binaryTagConverter = binaryTagConverter;

		}

		public BinaryTagType<?> getExpectedBinaryTag() {
			return expectedBinaryTag;
		}

		public Class<?> getSkriptCompatibleClass() {
			return skriptCompatibleClass;
		}

		public @Nullable Object convertToSkriptFriendly(BinaryTag binaryTag) {
			return binaryTagConverter.apply(binaryTag);
		}

		public static Object convertToSkript(BinaryTag binaryTag) {
			for (TagType tagType : TagType.values()) {
				if (!tagType.expectedBinaryTag.equals(binaryTag.type())) continue;
				return tagType.convertToSkriptFriendly(binaryTag);
			}
			return null;
		}

		public static BinaryTag convertFromSkript(Object o) {
			return convertFromSkript(o, null);
		}

		public static BinaryTag convertFromSkript(Object o, @Nullable BinaryTagType<?> expectedBinaryTag) {
			Class<?> clazz = o.getClass();
			if (clazz.isArray()) {
				Object[] arr = (Object[]) o;
				if (arr.length == 1) {
					clazz = arr[0].getClass();
					o = arr[0];
				}
			}
			for (TagType tagType : TagType.values()) {
				Class<?> compatibleClass = tagType.skriptCompatibleClass;
				if (clazz.isArray() && compatibleClass.isArray()) {
					if (compatibleClass.componentType().isAssignableFrom(((Object[]) o)[0].getClass())) return tagType.objectConverter.apply(o);
				}
				if (!tagType.skriptCompatibleClass.isAssignableFrom(clazz)) continue;
				return tagType.objectConverter.apply(o);
			}
			return null;
		}
	}

}
