package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Name("Boss Bars of Player")
@Description("""
	Every boss bar currently being shown to a player.
	Adding a boss bar shows it, removing one hides it, and clearing hides all of them.
	Setting this hides every boss bar the player can currently see, including any another script showed them, before showing the given ones.""")
@Examples("""
	add {_bar} to boss bars of all players
	remove {_bar} from player's boss bars

	broadcast "%player% can see %size of boss bars of player% boss bars"

	on quit:
		clear player's boss bars""")
@Keywords({"boss bar", "bossbar"})
public class ExprPlayerBossBars extends PropertyExpression<Player, BossBar> {

	static {
		register(ExprPlayerBossBars.class, BossBar.class, "boss[ ]bars", "players");
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Player>) expressions[0]);
		return true;
	}

	@Override
	protected BossBar[] get(Event event, Player[] source) {
		List<BossBar> bossBars = new ArrayList<>();
		for (Player player : source) {
			bossBars.addAll(MinecraftServer.getBossBarManager().getPlayerBossBars(player));
		}
		return bossBars.toArray(new BossBar[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case ADD, SET, REMOVE, DELETE, RESET -> CollectionUtils.array(BossBar[].class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
		Player[] players = getExpr().getArray(event);
		if (players.length == 0) return;
		if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
			for (Player player : players) {
				MinecraftServer.getBossBarManager().removeAllBossBars(player);
			}
			return;
		}
		if (delta == null) return;
		List<BossBar> bossBars = Arrays.stream(delta)
			.filter(BossBar.class::isInstance)
			.map(BossBar.class::cast)
			.toList();
		if (mode == Changer.ChangeMode.SET) {
			for (Player player : players) {
				MinecraftServer.getBossBarManager().removeAllBossBars(player);
			}
		}
		List<Player> playerList = Arrays.asList(players);
		for (BossBar bossBar : bossBars) {
			if (mode == Changer.ChangeMode.REMOVE) {
				MinecraftServer.getBossBarManager().removeBossBar(playerList, bossBar);
			} else {
				MinecraftServer.getBossBarManager().addBossBar(playerList, bossBar);
			}
		}
	}

	@Override
	public boolean isSingle() {
		return false;
	}

	@Override
	public Class<? extends BossBar> getReturnType() {
		return BossBar.class;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "boss bars of " + getExpr().toString(event, debug);
	}

}
