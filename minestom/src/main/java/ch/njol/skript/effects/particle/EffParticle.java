package ch.njol.skript.effects.particle;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Name("Draw Particle")
@Description("""
	Draws particles at the given locations for specific players or everyone in specified instances. Either players or an instance must be provided.
	See ExprParticle for more particle customization""")
@Examples("""
	draw 10 of flame at player for player
	draw 5 of smoke at player's position in player's instance with speed 0.5""")
public class EffParticle extends Effect {

	static {
		Skript.registerEffect(EffParticle.class,
			"[:force] draw %integer% [of] %particle%"
				+ " [(with offset|offset by) %-vector%] [%directions% %points%] [in [(world|instance)] %-instances%]"
				+ " [(to|for) %-players%] [with (speed|extra) %-number%] [without (:distance) limit[s]]");
	}

	private boolean force = false;

	private Expression<Integer> amount;
	private Expression<Particle> particle;

	@Nullable
	private Expression<Vec> offset;
	private Expression<Point> points;
	@Nullable
	private Expression<Instance> instances;
	@Nullable
	private Expression<Player> players;
	@Nullable
	private Expression<Number> extra;
	private boolean longDistance = false;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		force = parseResult.hasTag("force");
		amount = (Expression<Integer>) expressions[0];
		particle = (Expression<Particle>) expressions[1];

		offset = (Expression<Vec>) expressions[2];
		points = Direction.combine((Expression<? extends Direction>) expressions[3], (Expression<? extends Point>) expressions[4]);
		instances = (Expression<Instance>) expressions[5];
		players = (Expression<Player>) expressions[6];

		if (instances == null && players == null) {
			Skript.error("Instances and players cannot be null, provide one or both.");
			return false;
		}

		extra = (Expression<Number>) expressions[7];
		longDistance = parseResult.hasTag("distance");
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void execute(Event event) {
		Particle particle = this.particle.getSingle(event);
		if (particle == null) return;
		Vec offset = this.offset == null ? Vec.ZERO : this.offset.getSingle(event);
		if (offset == null) offset = Vec.ZERO;
		float extra = 1f;
		if (this.extra != null) {
			Number num = this.extra.getSingle(event);
			if (num != null) extra = num.floatValue();
		}
		Integer amount = this.amount.getSingle(event);
		if (amount == null) amount = 0;
		List<Player> players = collectPlayers(event);
		for (Point point : points.getArray(event)) {
			ParticlePacket packet = new ParticlePacket(particle, force, longDistance, point, offset, extra, amount);
			for (Player player : players) {
				player.sendPacket(packet);
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder syntaxBuilder = new SyntaxStringBuilder(event, debug);
		if (force) syntaxBuilder.append("force");
		syntaxBuilder.append("draw", amount, "of", particle);
		if (offset != null) syntaxBuilder.append("with offset", offset);
		syntaxBuilder.append(points);
		if (instances != null) syntaxBuilder.append("in instances", instances);
		if (players != null) syntaxBuilder.append("for", players);
		if (extra != null) syntaxBuilder.append("with extra", extra);
		if (longDistance) syntaxBuilder.append("without distance limit");
		return syntaxBuilder.toString();
	}

	private List<Player> collectPlayers(Event event) {
		List<Player> players = new ArrayList<>();
		if (this.players != null) players.addAll(List.of(this.players.getArray(event)));
		if (instances != null) {
			Set<Instance> instances = new HashSet<>(List.of(this.instances.getArray(event)));
			if (this.players != null) {
				for (Player player : List.copyOf(players)) {
					if (!instances.contains(player.getInstance())) players.remove(player);
				}
			} else {
				for (Instance instance : instances) {
					players.addAll(instance.getPlayers());
				}
			}
		}
		return players;
	}


}
