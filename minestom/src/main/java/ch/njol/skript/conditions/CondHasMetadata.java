package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.Taggable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Has Metadata")
@Description("Checks whether taggable objects have the given metadata tags.")
@Examples("""
	player has the metadata tag "frozen\"""")
public class CondHasMetadata extends Condition {

	static {
		Skript.registerCondition(CondHasMetadata.class,
			"%taggables% (has|have) [the] metadata[s] [tag[s]] %strings%",
			"%taggables% (doesn't|does not|do not|don't) have [the] metadata[s] [tag[s]] %strings%");
	}

	@SuppressWarnings("null")
	private Expression<String> tags;
	@SuppressWarnings("null")
	private Expression<Taggable> taggables;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
		taggables = (Expression<Taggable>) exprs[0];
		tags = (Expression<String>) exprs[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(final Event e) {
		return taggables.check(e,
			s -> tags.check(e,
				tag -> s.hasTag(Tag.Transient("skript-minestom:metadata:" + tag))), isNegated());
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, e, debug, taggables,
			"the metadata tag" + (tags.isSingle() ? " " : "s ") + tags.toString());
	}

}
