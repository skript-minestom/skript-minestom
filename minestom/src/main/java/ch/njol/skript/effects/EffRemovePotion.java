package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Potions;
import ch.njol.util.Kleenean;
import net.minestom.server.entity.Entity;
import net.minestom.server.potion.PotionEffect;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Remove Potion Effect")
@Description("Removes potion effects from entities, either specific ones or all of them at once.")
@Examples("""
	remove speed from player
	remove {_potion} from all players
	clear potion effects of player""")
public class EffRemovePotion extends Effect {

	static {
		Skript.registerEffect(EffRemovePotion.class,
			"remove %potions/potioneffecttypes% from %entities%",
			"(clear|remove all) [[the] potion] effects (of|from) %entities%");
	}

	private @Nullable Expression<?> effects;
	private Expression<Entity> entities;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (matchedPattern == 0) {
			effects = expressions[0];
			entities = (Expression<Entity>) expressions[1];
		} else {
			entities = (Expression<Entity>) expressions[0];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		Entity[] entities = this.entities.getArray(event);
		if (entities.length == 0) return;

		if (effects == null) {
			for (Entity entity : entities)
				entity.clearEffects();
			return;
		}

		for (Object object : effects.getArray(event)) {
			PotionEffect effect = Potions.effectOf(object);
			if (effect == null) continue;
			for (Entity entity : entities)
				entity.removeEffect(effect);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		if (effects == null) {
			sb.append("clear potion effects of", entities);
		} else {
			sb.append("remove", effects, "from", entities);
		}
		return sb.toString();
	}

}
