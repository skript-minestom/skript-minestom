package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.events.wrapper.EntitySpawnWrapper;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.variables.Variables;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;

import java.util.concurrent.CompletableFuture;

public abstract class EffSecEntitySpawn extends EffectSection {

	private static final EntryValidator ENTRY_VALIDATOR = EntryValidator.builder()
		.addSection("before spawn", true)
		.addSection("after spawn", true)
		.build();

	private @Nullable Trigger beforeSpawnTrigger;
	private @Nullable Trigger afterSpawnTrigger;

	protected boolean loadSpawnSections(@Nullable SectionNode sectionNode) {
		if (sectionNode == null) return true;
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
		return true;
	}

	protected Object spawn(Entity entity, Instance instance, Point point, Object locals, boolean sync) {
		Object spawnLocals = locals;
		if (beforeSpawnTrigger != null) {
			Event beforeSpawnEvent = new EntitySpawnWrapper(new EntitySpawnEvent(entity, instance));
			Variables.setLocalVariables(beforeSpawnEvent, locals);
			TriggerItem.walk(beforeSpawnTrigger, beforeSpawnEvent);
			spawnLocals = Variables.copyLocalVariables(beforeSpawnEvent);
		}
		Object afterSpawnLocals = spawnLocals;
		CompletableFuture<Void> future = entity.setInstance(instance, point)
											   .whenComplete((_, throwable) -> {
			if (throwable != null || afterSpawnTrigger == null) return;
			Event afterSpawnEvent = new EntitySpawnWrapper(new EntitySpawnEvent(entity, instance));
			Variables.setLocalVariables(afterSpawnEvent, afterSpawnLocals);
			TriggerItem.walk(afterSpawnTrigger, afterSpawnEvent);
			Variables.removeLocals(afterSpawnEvent);
		});
		if (sync) future.join();
		return spawnLocals;
	}

}
