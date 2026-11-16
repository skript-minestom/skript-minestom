package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.EntityAnimation;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.ServerPacket;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Play Animation")
@Description("Plays an animation for the given entities.")
@Examples("play swing main arm animation on player")
public class EffPlayAnimation extends Effect {

	static {
		Skript.registerEffect(EffPlayAnimation.class, "play %animations% animation[s] on %entities% [(to|for) %-players%]");
	}

	private Expression<EntityAnimation> animations;
	private Expression<Entity> entities;
	@Nullable
	private Expression<Player> players;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		animations = (Expression<EntityAnimation>) expressions[0];
		entities = (Expression<Entity>) expressions[1];
		players = (Expression<Player>) expressions[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Entity[] entities = this.entities.getArray(event);
		Player[] players = this.players == null ? null : this.players.getArray(event);
		for (EntityAnimation animation : animations.getArray(event)) {
			for (Entity entity : entities) {
				ServerPacket packet = animation.createPacket(entity);
				if (players == null) {
					if (entity.getInstance() != null) entity.getInstance().sendGroupedPacket(packet);
				} else {
					for (Player player : players) {
						player.sendPacket(packet);
					}
				}
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "play " + animations.toString(event, debug) + " animations on " + entities.toString(event, debug)
			+ (players == null ? "" : " " + players.toString(event, debug)) ;
	}

}
