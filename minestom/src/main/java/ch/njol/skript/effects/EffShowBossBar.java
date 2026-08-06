package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Arrays;

@Name("Show/Hide Boss Bar")
@Description("""
	Shows or hides a boss bar for players.
	A player can see several boss bars at once, so showing one does not hide any of the others.""")
@Examples("""
	show {_bar} to all players
	hide {_bar} from player

	on quit:
		hide all boss bars from player""")
@Keywords({"boss bar", "bossbar"})
public class EffShowBossBar extends Effect {

	static {
		Skript.registerEffect(EffShowBossBar.class,
			"show %bossbars% to %players%",
			"hide %bossbars% from %players%",
			"hide all boss[ ]bars from %players%");
	}

	@Nullable
	private Expression<BossBar> bossBars;
	private Expression<Player> players;

	private boolean show;
	private boolean all;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		show = matchedPattern == 0;
		all = matchedPattern == 2;
		if (all) {
			players = (Expression<Player>) expressions[0];
		} else {
			bossBars = (Expression<BossBar>) expressions[0];
			players = (Expression<Player>) expressions[1];
		}
		return true;
	}

	@Override
	protected void execute(Event event) {
		Player[] players = this.players.getArray(event);
		if (players.length == 0) return;
		if (all) {
			for (Player player : players) {
				MinecraftServer.getBossBarManager().removeAllBossBars(player);
			}
			return;
		}
		assert bossBars != null;
		for (BossBar bossBar : bossBars.getArray(event)) {
			if (show) {
				MinecraftServer.getBossBarManager().addBossBar(Arrays.asList(players), bossBar);
			} else {
				MinecraftServer.getBossBarManager().removeBossBar(Arrays.asList(players), bossBar);
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		if (all) return "hide all boss bars from " + players.toString(event, debug);
		assert bossBars != null;
		return (show ? "show " : "hide ") + bossBars.toString(event, debug) +
			(show ? " to " : " from ") + players.toString(event, debug);
	}

}
