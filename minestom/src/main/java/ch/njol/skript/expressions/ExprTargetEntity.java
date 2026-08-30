package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Name("Target Entity")
@Description("The target entity of an entity from their eye height. Default range is 50.")
@Example("""
	spawn zombie at position 2 in front of player in player's instance
	send target zombie of player with range 1.5 to player # doesn't print anything
	send target zombie of player with range 2 to player # prints zombie""")
public class ExprTargetEntity extends PropertyExpression<Entity, Entity> {

	private static final Predicate<? super Entity> PASS = _ -> true;

	static {
		Skript.registerExpression(ExprTargetEntity.class, Entity.class, ExpressionType.PROPERTY,
			"[the] target[[ed] %-entitytype%] [of %entities%] [with range %-number%] [blocks:ignoring blocks]",
			"%entities%'[s] target[[ed] %-entitytype%] [with range %-number%] [blocks:ignoring blocks]");
	}

	@Nullable
	private Expression<EntityType> type;
	@Nullable
	private Expression<Number> range;
	private boolean ignoreBlocks;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Entity>) expressions[1 - matchedPattern]);
		type = (Expression<EntityType>) expressions[matchedPattern];
		range = (Expression<Number>) expressions[2];
		ignoreBlocks = parseResult.hasTag("blocks");
		return true;
	}

	@Override
	protected Entity[] get(Event event, Entity[] source) {
		double range = this.range == null ? 50 : this.range.getOptionalSingle(event).map(Number::doubleValue).orElse(50d);
		Predicate<? super Entity> predicate = PASS;
		if (type != null) {
			EntityType type = this.type.getSingle(event);
			if (type == null) return new Entity[0];
			predicate = entity -> entity.getEntityType().equals(type);
		}
		List<Entity> entities = new ArrayList<>();
		for (Entity entity : source) {
			Entity target = entity.getLineOfSightEntity(range, predicate, ignoreBlocks);
			if (target != null) entities.add(target);
		}
		return entities.toArray(new Entity[0]);
	}

	@Override
	public boolean isSingle() {
		return getExpr().isSingle();
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder b = new SyntaxStringBuilder(event, debug);
		b.append("target" + (type == null ? "" : "ed"));
		if (type != null) b.append(type);
		b.append("of", getExpr());
		b.append("with range", range == null ? 50 : range);
		if (ignoreBlocks) b.append("ignoring blocks");
		return b.toString();
	}

}
