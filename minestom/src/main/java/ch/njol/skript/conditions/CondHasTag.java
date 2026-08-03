package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.NBTCompound;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.NBTUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Has NBT Tag")
@Description("Checks whether NBT compounds contain the given tag paths. Supports nested tags using dot notation.")
@Examples("{_nbt} has the nbt tag \"Health\"")
public class CondHasTag extends Condition {

	static {
		Skript.registerCondition(CondHasTag.class,
			"%nbtcompounds% (has|have) [the] [nbt] tag[s] %strings%",
			"%nbtcompounds% (doesn't|does not|do not|don't) have [the] [nbt] tag[s] %strings%");
	}

	private Expression<NBTCompound> compounds;
	private Expression<String> tags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		compounds = (Expression<NBTCompound>) expressions[0];
		tags = (Expression<String>) expressions[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		return compounds.check(event,
			compound -> tags.check(event,
				tag -> NBTUtils.getNestedTag(compound.getCompound(), tag) != null, isNegated()));
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, compounds,
			"the nbt tag" + (tags.isSingle() ? " " : "s ") + tags.toString());
	}

}
