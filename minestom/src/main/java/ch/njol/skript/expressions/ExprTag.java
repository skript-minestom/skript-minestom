package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.MinecraftTag;
import ch.njol.skript.util.TagType;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Name("Tag")
@Description("""
	A minecraft tag, used to group items, blocks or entity types together.
	Tags are written as "namespace:value"; if the namespace is left out, "minecraft" is used.
	Without a type, every registry is searched, so a name shared by an item and a block tag returns both.""")
@Examples("""
	set {_tag} to minecraft tag "logs"
	set {_tag} to block tag "mineable/pickaxe"
	set {_tags::*} to item tags "planks" and "slabs"
	broadcast tag contents of entity type tag "skeletons\"""")
@Keywords({"tag", "minecraft tag", "registry"})
public class ExprTag extends SimpleExpression<MinecraftTag> {

	static {
		Skript.registerExpression(ExprTag.class, MinecraftTag.class, ExpressionType.COMBINED,
			"[the] [minecraft] [" + TagType.getFullPattern() + "] tag[s] %strings%");
	}

	private Expression<String> names;
	@Nullable
	private TagType<?> type;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		names = (Expression<String>) expressions[0];
		if (parseResult.mark != 0) type = TagType.getType(parseResult.mark - 1);
		return true;
	}

	@Override
	protected MinecraftTag[] get(Event event) {
		List<TagType<?>> types = type == null ? TagType.getTypes() : List.of(type);
		List<MinecraftTag<?>> tags = new ArrayList<>();
		for (String name : names.getArray(event)) {
			String full = name.toLowerCase(Locale.ENGLISH);
			if (!full.contains(":")) full = "minecraft:" + full;
			if (!Key.parseable(full)) continue;
			Key key = Key.key(full);
			for (TagType<?> type : types) {
				MinecraftTag<?> tag = lookup(type, key);
				if (tag != null) tags.add(tag);
			}
		}
		return tags.toArray(new MinecraftTag[0]);
	}

	@Override
	public boolean isSingle() {
		return names.isSingle() && type != null;
	}

	@Override
	public Class<? extends MinecraftTag> getReturnType() {
		return MinecraftTag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (type == null ? "" : type.getName() + " ") + "tag " + names.toString(event, debug);
	}

	@Nullable
	private static <T extends RegistryKey<T>> MinecraftTag<T> lookup(TagType<T> type, Key key) {
		RegistryTag<T> tag = type.getRegistry().getTag(key);
		if (tag == null) return null;
		return new MinecraftTag<>(tag, type);
	}

}
