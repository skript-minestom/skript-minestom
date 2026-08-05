package ch.njol.skript.util;

import net.kyori.adventure.key.Key;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record MinecraftTag<T extends RegistryKey<T>>(RegistryTag<T> tag, TagType<T> type) {

	public Key key() {
		return tag.key().key();
	}

	@SuppressWarnings("unchecked")
	public boolean contains(RegistryKey<?> value) {
		return tag.contains((RegistryKey<T>) value);
	}

	public Object[] contents() {
		Registry<T> registry = type.getRegistry();
		List<Object> values = new ArrayList<>(tag.size());
		for (RegistryKey<T> key : tag) {
			T value = registry.get(key);
			if (value != null) values.add(type.toSkript(value));
		}
		return values.toArray();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof MinecraftTag<?> other)) return false;
		return type == other.type && key().equals(other.key());
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, key());
	}

	@Override
	public String toString() {
		return key().asString();
	}

}
