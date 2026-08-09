package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.MinecraftTag;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Name("Tag Contents")
@Description("""
	Every value held by a minecraft tag.
	An item tag returns items, a block tag returns blocks, and an entity type tag returns entity types.""")
@Examples("""
	broadcast tag contents of block tag "logs"
	set {_planks::*} to tag values of item tag "planks\"""")
@Keywords({"tag", "contents", "minecraft tag"})
public class ExprTagContents extends SimpleExpression<Object> {

	static {
		PropertyExpression.register(ExprTagContents.class, Object.class, "tag (contents|values)", "minecrafttags");
	}

	private Expression<MinecraftTag> tags;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		tags = (Expression<MinecraftTag>) expressions[0];
		return true;
	}

	@Override
	protected Object[] get(Event event) {
		List<Object> values = new ArrayList<>();
		for (MinecraftTag<?> tag : tags.getArray(event)) {
			Collections.addAll(values, tag.contents());
		}
		return values.toArray();
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "tag contents of " + tags.toString(event, debug);
	}

}
