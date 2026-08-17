package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.EntitySpawnWrapper;
import ch.njol.skript.lang.*;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.NonTickingEntity;
import ch.njol.skript.util.NonTickingLivingEntity;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Name("Spawn Entity")
@Description("Spawns one or more entities at a location. If an entity is non-ticking, some features like gravity may not work even if it's set to true.")
@Examples("""
	spawn living zombie at player's position:
	    before spawn:
	        set name of entity to "Custom Zombie"
	    after spawn:
	        broadcast "Zombie spawned!\"""")
public class EffSecSpawn extends EffectSection {

	private static final EntryValidator ENTRY_VALIDATOR;
	private static final Set<EntityType> NO_PHYSICS_TYPES = Set.of(EntityType.INTERACTION, EntityType.MARKER,
		EntityType.ITEM_DISPLAY, EntityType.TEXT_DISPLAY, EntityType.BLOCK_DISPLAY, EntityType.PAINTING, EntityType.ITEM_FRAME,
		EntityType.GLOW_ITEM_FRAME, EntityType.OMINOUS_ITEM_SPAWNER, EntityType.AREA_EFFECT_CLOUD, EntityType.EYE_OF_ENDER);

	static {
		ENTRY_VALIDATOR = EntryValidator.builder()
										.addSection("before spawn", true)
										.addSection("after spawn", true)
										.build();
		Skript.registerSection(EffSecSpawn.class,
			"(summon|spawn) [:non ticking] [:navigable|:living] %entitytypes% [%directions% %points%] [in [(world|instance)[s]] %instances%] [:sync]",
			"(summon|spawn) %integer% [of] [:non ticking] [:navigable|:living] %entitytypes% [%directions% %points%] [in [(world|instance)[s]] %instances%] [:sync]");
	}

	private Expression<Integer> amount;
	private Expression<EntityType> types;
	private Expression<Point> points;
	private Expression<Instance> instances;
	@Nullable
	private String type;
	@Nullable
	private Trigger beforeSpawnTrigger;
	@Nullable
	private Trigger afterSpawnTrigger;
	private boolean sync = false;
	private boolean nonTicking = false;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		if (matchedPattern == 1) amount = (Expression<Integer>) expressions[0];
		types = (Expression<EntityType>) expressions[matchedPattern];
		points = Direction.combine((Expression<? extends Direction>) expressions[1+matchedPattern], (Expression<? extends Point>) expressions[2+matchedPattern]);
		instances = (Expression<Instance>) expressions[3+matchedPattern];
		nonTicking = parseResult.hasTag("non ticking");
		sync = parseResult.hasTag("sync");
		List<String> tags = parseResult.tags;
		if (!tags.isEmpty()) {
			int typeIndex = nonTicking ? 1 : 0;
			if (tags.size() > 1) type = tags.get(typeIndex);
		}
		if (sectionNode != null) {
			EntryContainer container = ENTRY_VALIDATOR.validate(sectionNode);
			if (container == null) return false;
			SectionNode beforeSpawn = container.getOptional("before spawn", SectionNode.class, false);
			if (beforeSpawn != null) beforeSpawnTrigger = loadCode(beforeSpawn, "before spawn", EntitySpawnWrapper.class);
			SectionNode afterSpawn = container.getOptional("after spawn", SectionNode.class, false);
			if (afterSpawn != null) afterSpawnTrigger = loadCode(afterSpawn, "after spawn", EntitySpawnWrapper.class);
			if (beforeSpawn == null && afterSpawn == null) {
				Skript.error("You can't run abstract code within this section! Either put it under 'before spawn' or 'after spawn'.");
				return false;
			}
		}
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		Integer amount = null;
		if (this.amount != null) amount = this.amount.getSingle(event);
		if (amount == null) amount = 1;
		EntityType[] types = this.types.getArray(event);
		Point[] points = this.points.getArray(event);
		Instance[] instances = this.instances.getArray(event);
		Object variables = Variables.copyLocalVariables(event);
		Object mostRecentLocals = variables;
		for (EntityType type : types) {
			if (type == EntityType.PLAYER) continue;
			for (Instance instance : instances) {
				for (Point point : points) {
					for (int i = 0; i < amount; i++) {
						Entity entity = switch (this.type) {
							case "navigable" -> new EntityCreature(type);
							case "living" -> nonTicking ? new NonTickingLivingEntity(type) : new LivingEntity(type);
							case null, default -> nonTicking ? new NonTickingEntity(type) : new Entity(type);
						};
						if (NO_PHYSICS_TYPES.contains(entity.getEntityType())) {
							entity.setNoGravity(true);
							entity.setHasPhysics(false);
						}
						if (beforeSpawnTrigger != null) {
							Event e = new EntitySpawnWrapper(new EntitySpawnEvent(entity, instance));
							Variables.setLocalVariables(e, variables);
							TriggerItem.walk(beforeSpawnTrigger, e);
							mostRecentLocals = Variables.copyLocalVariables(e);
						}
						Object finalMostRecentLocals = mostRecentLocals;
						CompletableFuture<Void> future = entity.setInstance(instance, point)
															   .whenComplete((_, throwable) -> {
							if (throwable != null || afterSpawnTrigger == null) return;
							Event e = new EntitySpawnWrapper(new EntitySpawnEvent(entity, instance));
							Variables.setLocalVariables(e, finalMostRecentLocals);
							TriggerItem.walk(afterSpawnTrigger, e);
							Variables.removeLocals(e);
						});
						if (sync) future.join();
					}
				}
			}
		}
		Variables.setLocalVariables(event, mostRecentLocals);
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String type = this.type == null ? "normal" : this.type;
		String amount = this.amount == null ? "1" : this.amount.toString(event, debug);
		return "spawn " + amount + " of " + type + " " + types.toString(event, debug) + " "
			+ points.toString(event, debug) + " in instances " + instances.toString(event, debug);
	}

}
