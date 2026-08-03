package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.EntityMeta;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

@Name("Edit Entity Meta")
@Description("Edits the metadata of one or more entities. Unnecessary unless you want entity meta to apply in one packet.")
@Examples("""
	edit meta of {_zombie}:
	    set name of entity to "New Name"
	    set custom name visibility of entity to true""")
public class SecEditEntityMeta extends Section {

	static {
		Skript.registerSection(SecEditEntityMeta.class, "(modify|edit) [entity] meta[data] of %entities%",
			"(modify|edit) %entities%'[s] [entity] meta[data]");
		EventValues.registerEventValue(EditEntityMetaEvent.class, Entity.class, EditEntityMetaEvent::getEntity);
	}

	private Expression<Entity> entities;

	private Trigger editTrigger;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult,
						@NonNull SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
		if (sectionNode.isEmpty()) {
			Skript.error("Edit Entity Meta section is missing code within the section!");
			return false;
		}
		entities = (Expression<Entity>) expressions[0];
		editTrigger = loadCode(sectionNode, "edit entity meta", EditEntityMetaEvent.class);
		return true;
	}

	@Override
	protected @Nullable TriggerItem walk(Event event) {
		for (Entity entity : entities.getArray(event)) {
			EntityMeta meta = entity.getEntityMeta();
			meta.setNotifyAboutChanges(false);
			EditEntityMetaEvent e = new EditEntityMetaEvent(entity);
			Variables.withLocalVariables(event, e, () -> TriggerItem.walk(editTrigger, e));
			meta.setNotifyAboutChanges(true);
		}
		return super.walk(event, false);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "edit entity meta of " + entities.toString(event, debug);
	}

	static class EditEntityMetaEvent extends Event {

		private final Entity entity;

		public EditEntityMetaEvent(Entity entity) {
			this.entity = entity;
		}

		public Entity getEntity() {
			return entity;
		}

		@Override
		public HandlerList getHandlers() {
			return null;
		}

	}

}
