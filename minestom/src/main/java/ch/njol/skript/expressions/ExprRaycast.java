package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Raycast;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Name("Raycast")
@Description("""
	Casts a ray and gives back what it hit. A block raycast only stops at blocks, an entity raycast only at \
	entities, and a plain raycast stops at whichever comes first. Casting from an entity starts at its eyes, \
	follows where it is looking, and ignores that entity. Blocks without a collision shape, such as grass, \
	are passed through unless the cast stops at any block. The range defaults to 50 blocks.""")
@Examples("""
	set {_ray} to raycast from player for 50 blocks
	set {_ray} to entity raycast from player ignoring all players
	set {_ray} to block raycast from {_position} in direction {_vector} in {_instance}
	set {_ray} to block raycast from {_position} towards {_target} in {_instance} stopping at any block""")
public class ExprRaycast extends SimpleExpression<Raycast> {

	static {
		String options = "[for %-number% block[s]] [ignoring %-entities%] [anyblock:stopping at any block]";
		Skript.registerExpression(ExprRaycast.class, Raycast.class, ExpressionType.COMBINED,
			"[a] [new] [(block:block|entity:entity)] raycast from %entities% " + options,
			"[a] [new] [(block:block|entity:entity)] raycast from %point% (in direction [of]|towards:towards) "
				+ "%vector/point% in [instance] %instance% " + options);
	}

	private @Nullable Expression<Entity> casters;
	private @Nullable Expression<Point> origin;
	private @Nullable Expression<?> direction;
	private @Nullable Expression<Instance> instance;
	private @Nullable Expression<Number> range;
	private @Nullable Expression<Entity> ignored;
	private boolean towards;
	private boolean stopAtAnyBlock;
	private Raycast.Kind kind;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) {
			casters = (Expression<Entity>) expressions[0];
			range = (Expression<Number>) expressions[1];
			ignored = (Expression<Entity>) expressions[2];
		} else {
			origin = (Expression<Point>) expressions[0];
			direction = expressions[1];
			instance = (Expression<Instance>) expressions[2];
			range = (Expression<Number>) expressions[3];
			ignored = (Expression<Entity>) expressions[4];
		}
		towards = parseResult.hasTag("towards");
		stopAtAnyBlock = parseResult.hasTag("anyblock");
		if (parseResult.hasTag("block")) {
			kind = Raycast.Kind.BLOCK;
		} else if (parseResult.hasTag("entity")) {
			kind = Raycast.Kind.ENTITY;
		} else {
			kind = Raycast.Kind.ANY;
		}
		return true;
	}

	@Override
	protected Raycast @Nullable [] get(Event event) {
		double range = rangeOf(event);
		if (Double.isNaN(range)) return new Raycast[0];
		List<Entity> ignored = this.ignored == null ? List.of() : Arrays.asList(this.ignored.getArray(event));

		if (casters != null) {
			List<Raycast> raycasts = new ArrayList<>();
			for (Entity caster : casters.getArray(event)) {
				Instance instance = caster.getInstance();
				if (instance == null) continue;
				Point origin = caster.getPosition().add(0, caster.getEyeHeight(), 0);
				List<Entity> skipped = new ArrayList<>(ignored);
				skipped.add(caster);
				raycasts.add(Raycast.cast(kind, instance, origin, caster.getPosition().direction(), range,
					skipped, stopAtAnyBlock));
			}
			return raycasts.toArray(new Raycast[0]);
		}

		Point origin = this.origin.getSingle(event);
		Instance instance = this.instance.getSingle(event);
		Object direction = this.direction.getSingle(event);
		if (origin == null || instance == null || !(direction instanceof Point point)) return new Raycast[0];

		Vec vector = towards ? Vec.fromPoint(point).sub(origin) : Vec.fromPoint(point);
		if (vector.isZero()) return new Raycast[0];
		return new Raycast[]{Raycast.cast(kind, instance, origin, vector, range, ignored, stopAtAnyBlock)};
	}

	private double rangeOf(Event event) {
		if (range == null) return Raycast.DEFAULT_RANGE;
		Number value = range.getSingle(event);
		if (value == null) return Double.NaN;
		double range = value.doubleValue();
		if (Double.isNaN(range)) return Double.NaN;
		return Math.max(0, range);
	}

	@Override
	public boolean isSingle() {
		return casters == null || casters.isSingle();
	}

	@Override
	public Class<? extends Raycast> getReturnType() {
		return Raycast.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append(switch (kind) {
			case BLOCK -> "block raycast";
			case ENTITY -> "entity raycast";
			case ANY -> "raycast";
		});
		if (casters != null) {
			sb.append("from", casters);
		} else {
			sb.append("from", origin, towards ? "towards" : "in direction", direction, "in", instance);
		}
		if (range != null) sb.append("for", range, "blocks");
		if (ignored != null) sb.append("ignoring", ignored);
		if (stopAtAnyBlock) sb.append("stopping at any block");
		return sb.toString();
	}

}
