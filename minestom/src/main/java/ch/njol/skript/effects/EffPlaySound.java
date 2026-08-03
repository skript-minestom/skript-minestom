package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Play Sound")
@Description("Plays a sound at a certain location, for certain players, or from an entity.")
@Examples("""
	play sound "entity.player.levelup" at player to all players
	play sound "ambient.cave" at vector(10, 64, 10) in player's instance
	play sound "entity.villager.ambient" from {_entity} to player # makes the sound follow the provided entity""")
public class EffPlaySound extends Effect {

	static {
		Skript.registerEffect(EffPlaySound.class,
			"play %sounds% [%-directions% %-points%] (to|for) %players%",
			"play %sounds% [%directions% %points%] [in [(world|instance)[s]] %instances%]",
			"play %sounds% (on|from) %entities% [(to|for) %-players%]");
	}

	private Expression<Sound> sounds;
	@Nullable
	private Expression<Point> points;
	@Nullable
	private Expression<Player> players;
	@Nullable
	private Expression<Instance> instances;
	@Nullable
	private Expression<Entity> entities;

	private int pattern;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		pattern = matchedPattern;
		sounds = (Expression<Sound>) expressions[0];
		if (matchedPattern != 2) {
			Expression<? extends Direction> expr1 = (Expression<? extends Direction>) expressions[1];
			Expression<? extends Point> expr2 = (Expression<? extends Point>) expressions[2];
			if (expr1 != null && expr2 != null) points = Direction.combine((Expression<? extends Direction>) expressions[1],
				(Expression<? extends Point>) expressions[2]);
			if (matchedPattern == 0) players = (Expression<Player>) expressions[3];
			else instances = (Expression<Instance>) expressions[3];
		} else {
			entities = (Expression<Entity>) expressions[1];
			players = (Expression<Player>) expressions[2];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		Player[] players = this.players == null ? null : this.players.getArray(event);
		Point[] points = this.points == null ? null : this.points.getArray(event);
		Entity[] entities = this.entities == null ? null : this.entities.getArray(event);
		Instance[] instances = this.instances == null ? null : this.instances.getArray(event);
		for (Sound sound : sounds.getArray(event)) {
			switch (pattern) {
				case 0 -> {
					assert players != null;
					if (points == null) {
						for (Player player : players) {
							player.playSound(sound);
						}
					} else {
						for (Player player : players) {
							for (Point point : points) {
								player.playSound(sound, point);
							}
						}
					}

				}
				case 1 -> {
					assert points != null;
					assert instances != null;
					for (Point point : points) {
						for (Instance instance : instances) {
							instance.playSound(sound, point);
						}
					}
				}
				case 2 -> {
					assert entities != null;
					for (Entity entity : entities) {
						if (players == null) entity.getInstance().playSound(sound, entity);
						else {
							for (Player player : players) {
								player.playSound(sound, entity);
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public String toString(@Nullable Event event, boolean debug) {
		String toString = "play sound " + sounds.toString(event, debug);
		return switch (pattern) {
			case 0 -> toString + (points != null ? points.toString(event, debug) : "") + " to " + players.toString(event, debug);
			case 1 -> toString + points.toString(event, debug) + " in instance " + instances.toString(event, debug);
			case 2 -> toString + " from " + entities.toString(event, debug) + (players == null ? "" : " " + players.toString(event, debug));
			default -> throw new IllegalStateException("Unexpected value: " + pattern);
		};
	}

}
