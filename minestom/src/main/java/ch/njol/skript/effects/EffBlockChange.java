package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.blockchange.MultiBlockChange;
import ch.njol.util.Kleenean;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.utils.PacketSendingUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Set;

@Name("Block Change")
@Description("Make the provided players see the provided points as the provided block (client sided blocks).")
@Example("make player see all blocks in radius 3 of player as stone in player's instance")
public class EffBlockChange extends Effect {

	static {
		Skript.registerEffect(EffBlockChange.class, "make %players% see %points% as %block% [in %instance%]");
	}

	private Expression<Player> players;
	private Expression<Point> points;
	private Expression<Block> block;
	private Expression<Instance> instance;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		players = (Expression<Player>) expressions[0];
		points = (Expression<Point>) expressions[1];
		block = (Expression<Block>) expressions[2];
		instance = (Expression<Instance>) expressions[3];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Block block = this.block.getSingle(event);
		Instance instance = this.instance.getSingle(event);
		if (block == null || instance == null) return;
		Point[] points = this.points.getArray(event);
		if (points.length == 0) return;
		Set<Player> players = Set.of(this.players.getArray(event));

		if (points.length == 1) {
			BlockChangePacket packet = new BlockChangePacket(points[0], block);
			PacketSendingUtils.sendGroupedPacket(players, packet);
		} else {
			MultiBlockChange multiBlockChange = new MultiBlockChange(instance);
			for (Point point : points) {
				multiBlockChange.set(point, block);
			}
			multiBlockChange.broadcastTo(players);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "make " + players.toString(event, debug) + " see " + points.toString(event, debug) + " as " + block.toString(event, debug)
			+ " in " + instance.toString(event, debug);
	}

}
