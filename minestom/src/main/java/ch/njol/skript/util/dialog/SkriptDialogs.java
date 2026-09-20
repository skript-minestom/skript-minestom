package ch.njol.skript.util.dialog;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkriptDialogs {

	public record Entry(DialogWrapper dialog, Script owner, Structure ownerStructure) { }

	private static final Map<Key, Entry> DIALOGS = new ConcurrentHashMap<>();

	public static void put(Key key, DialogWrapper dialog, Script owner, Structure ownerStructure) {
		DIALOGS.put(key, new Entry(dialog, owner, ownerStructure));
	}

	public static @Nullable DialogWrapper get(Key key) {
		Entry entry = DIALOGS.get(key);
		return entry == null ? null : entry.dialog();
	}

	public static @Nullable Script getOwner(Key key) {
		Entry entry = DIALOGS.get(key);
		return entry == null ? null : entry.owner();
	}

	public static @Nullable Structure getOwnerStructure(Key key) {
		Entry entry = DIALOGS.get(key);
		return entry == null ? null : entry.ownerStructure();
	}

	public static void remove(Key key) {
		DIALOGS.remove(key);
	}

	public static Set<Key> keys() {
		return DIALOGS.keySet();
	}

}
