package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.MinecraftTag;
import ch.njol.skript.util.TagType;
import ch.njol.util.Kleenean;
import net.minestom.server.registry.RegistryKey;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Is Tagged")
@Description("""
	Checks whether an item, block, entity or entity type is tagged with the given minecraft tag.
	Matching is done by namespaced key, so an oak log is tagged with both the item and the block version of "logs".""")
@Examples("""
	if player's tool is tagged with item tag "planks":
		send "that's a plank!"

	on block break:
		if event-block is tagged with tag "mineable/pickaxe":
			cancel event""")
@Keywords({"tag", "tagged", "minecraft tag"})
public class CondIsTagged extends Condition {

	static {
		Skript.registerCondition(CondIsTagged.class,
			"%items/blocks/entitytypes/entities% (is|are) tagged (as|with) %minecrafttags%",
			"%items/blocks/entitytypes/entities% (isn't|is not|aren't|are not) tagged (as|with) %minecrafttags%");
	}

	private Expression<?> values;
	private Expression<MinecraftTag> tags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		values = expressions[0];
		tags = (Expression<MinecraftTag>) expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		return values.check(event,
			value -> tags.check(event, tag -> matches(value, tag)), isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return PropertyCondition.toString(this, PropertyCondition.PropertyType.BE, event, debug, values,
			"tagged as " + tags.toString(event, debug));
	}

	private static boolean matches(Object value, MinecraftTag<?> tag) {
		RegistryKey<?> key = TagType.keyOf(value);
		return key != null && tag.contains(key);
	}

}
