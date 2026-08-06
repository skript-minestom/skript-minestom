package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Boss Bars of Player")
@Description("Every boss bar currently being shown to a player.")
@Examples("""
	broadcast "%player% can see %size of boss bars of player% boss bars"

	hide boss bars of player from player""")
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
