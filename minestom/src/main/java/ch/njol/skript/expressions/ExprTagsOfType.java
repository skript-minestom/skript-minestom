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
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("All Tags of a Type")
@Description("""
	Every minecraft tag the server knows about, optionally limited to one kind.
	Without a kind, the item, block and entity type registries are all included.""")
@Examples("""
	broadcast size of all block tags
	set {_entityTags::*} to all entity type tags""")
@Keywords({"tag", "tags", "minecraft tag", "registry"})
public class ExprTagsOfType extends SimpleExpression<MinecraftTag> {

	static {
		Skript.registerExpression(ExprTagsOfType.class, MinecraftTag.class, ExpressionType.SIMPLE,
			"[all [of] [the]] [minecraft] " + TagType.getFullPattern() + " tags",
			"all [of] [the] [minecraft] tags");
	}

	@Nullable
	private TagType<?> type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) type = TagType.getType(parseResult.mark - 1);
		return true;
	}

	@Override
	protected MinecraftTag[] get(Event event) {
		List<TagType<?>> types = type == null ? TagType.getTypes() : List.of(type);
		List<MinecraftTag<?>> tags = new ArrayList<>();
		for (TagType<?> searchType : types) collect(searchType, tags);
		return tags.toArray(new MinecraftTag[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends MinecraftTag> getReturnType() {
		return MinecraftTag.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "all " + (type == null ? "" : type.getName() + " ") + "tags";
	}

	private static <T extends RegistryKey<T>> void collect(TagType<T> type, List<MinecraftTag<?>> into) {
		for (RegistryTag<T> tag : type.getRegistry().tags()) into.add(new MinecraftTag<>(tag, type));
	}

}
