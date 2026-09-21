package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Raycast;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Raycast Hit")
@Description("Whether a raycast hit a block, an entity, or anything at all.")
@Examples("""
	set {_ray} to raycast from player
	if {_ray} hit a block:
		send "you are looking at %hit block of {_ray}%" to player
	if {_ray} didn't hit anything:
		send "nothing in range" to player""")
public class CondRaycastHit extends Condition {

	static {
		Skript.registerCondition(CondRaycastHit.class,
			"%raycasts% hit [a[n]] (block:block|entity:entity|anything)",
			"%raycasts% (didn't|did not|doesn't|does not) hit [a[n]] (block:block|entity:entity|anything)");
	}

	private Expression<Raycast> raycasts;
	private boolean block;
	private boolean entity;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		raycasts = (Expression<Raycast>) expressions[0];
		block = parseResult.hasTag("block");
		entity = parseResult.hasTag("entity");
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(Event event) {
		return raycasts.check(event, raycast -> {
			if (block) return raycast.hitBlock() != null;
			if (entity) return raycast.hitEntity() != null;
			return raycast.hitAnything();
		}, isNegated());
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append(raycasts, isNegated() ? "didn't hit" : "hit");
		sb.append(block ? "a block" : entity ? "an entity" : "anything");
		return sb.toString();
	}

}
