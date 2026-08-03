package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Name("Passengers")
@Description("The passengers of an entity.")
@Examples("""
	set {_passengers::*} to passengers of player
	add {_entity} to passengers of player
	remove player from passengers of vehicle""")
public class ExprPassengers extends PropertyExpression<Entity, Entity> {

	static {
		register(ExprPassengers.class, Entity.class, "passengers", "entities");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Entity>) expressions[0]);
		return true;
	}

	@Override
	public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case REMOVE, RESET, DELETE, ADD -> CollectionUtils.array(Entity[].class);
			default -> null;
		};
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event event, @org.jspecify.annotations.Nullable @Nullable Object[] delta, Changer.ChangeMode mode) {
		Entity[] entities = delta == null ? new Entity[0] : Arrays.copyOf(delta, delta.length, Entity[].class);
		for (Entity entity : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
				Iterator<Entity> iterator = entity.getPassengers().iterator();
				//noinspection WhileLoopReplaceableByForEach // not safe to replace
				while (iterator.hasNext()) {
					entity.removePassenger(iterator.next());
				}
				continue;
			}
			for (Entity e : entities) {
				switch (mode) {
					case REMOVE -> entity.removePassenger(e);
					case ADD -> {
						if (entity.equals(e)) continue;
						entity.addPassenger(e);
					}
				}
			}
		}
	}

	@Override
	protected Entity[] get(Event event, Entity[] source) {
		List<Entity> passengers = new ArrayList<>();
		for (Entity entity : source) {
			passengers.addAll(entity.getPassengers());
		}
		return passengers.toArray(new Entity[0]);
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "passengers of " + getExpr().toString(event, debug);
	}

}
