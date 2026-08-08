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

@Name("Tags Of")
@Description("""
	Every minecraft tag an item, block, entity or entity type belongs to.
	Without a kind, every registry is searched, so an oak log returns both its item and its block tags.""")
@Examples("""
	broadcast tags of dirt
	set {_tags::*} to block tags of event-block
	if player's tool's item tags contains item tag "planks":
		send "holding a plank" to player""")
@Keywords({"tag", "tags of", "minecraft tag"})
public class ExprTagsOf extends SimpleExpression<MinecraftTag> {

	static {
		Skript.registerExpression(ExprTagsOf.class, MinecraftTag.class, ExpressionType.PROPERTY,
			"[all [of] [the]] [" + TagType.getFullPattern() + "] tags of %items/blocks/entitytypes/entities%",
			"%items/blocks/entitytypes/entities%'[s] [" + TagType.getFullPattern() + "] tags");
	}

	private Expression<?> values;
	@Nullable
	private TagType<?> type;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		values = expressions[0];
		if (parseResult.mark != 0) type = TagType.getType(parseResult.mark - 1);
		return true;
	}

	@Override
	protected MinecraftTag[] get(Event event) {
		List<TagType<?>> types = type == null ? TagType.getTypes() : List.of(type);
		List<MinecraftTag<?>> tags = new ArrayList<>();
		for (Object value : values.getArray(event)) {
			RegistryKey<?> key = TagType.keyOf(value);
			if (key == null) continue;
			for (TagType<?> searchType : types) collect(searchType, key, tags);
		}
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
		return (type == null ? "" : type.getName() + " ") + "tags of " + values.toString(event, debug);
	}

	private static <T extends RegistryKey<T>> void collect(TagType<T> type, RegistryKey<?> key, List<MinecraftTag<?>> into) {
		for (RegistryTag<T> tag : type.getRegistry().tags()) {
			MinecraftTag<T> minecraftTag = new MinecraftTag<>(tag, type);
			if (minecraftTag.contains(key)) into.add(minecraftTag);
		}
	}

}
