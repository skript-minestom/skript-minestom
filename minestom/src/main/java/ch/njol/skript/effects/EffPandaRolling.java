package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.animal.PandaMeta;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Make Panda Roll")
@Description("""
	Make a panda start/stop rolling.
	Entities that are not pandas are ignored.""")
@Examples("""
	spawn a panda at player's position:
		after spawn:
			make entity start rolling

	make {_panda} stop rolling""")
@Keywords({"panda", "roll", "rolling"})
public class EffPandaRolling extends Effect {

	static {
		Skript.registerEffect(EffPandaRolling.class,
			"make %entities% (start:(start rolling|roll)|stop rolling)",
			"force %entities% to (:start|stop) rolling");
	}

	private Expression<Entity> entities;
	private boolean start;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		entities = (Expression<Entity>) expressions[0];
		start = parseResult.hasTag("start");
		return true;
	}

	@Override
	protected void execute(Event event) {
		for (Entity entity : entities.getArray(event)) {
			if (entity.getEntityMeta() instanceof PandaMeta pandaMeta) {
				pandaMeta.setRolling(start);
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
		builder.append("make", entities);
		if (start) {
			builder.append("start");
		} else {
			builder.append("stop");
		}
		builder.append("rolling");
		return builder.toString();
	}

}
