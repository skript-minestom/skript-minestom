package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.bossbar.BossBar;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.bossbar.BossBarManager;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Destroy BossBar")
@Description("Effectively deletes the provided bossbar")
@Examples("destroy bossbar {-b}")
public class EffDestroyBossBar extends Effect {

	static {
		Skript.registerEffect(EffDestroyBossBar.class, "destroy boss[ ]bar[s] %bossbars%");
	}

	private Expression<BossBar> bossBars;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		bossBars = (Expression<BossBar>) expressions[0];
		return true;
	}

	@Override
	protected void execute(Event event) {
		BossBarManager bossBarManager = MinecraftServer.getBossBarManager();
		for (BossBar bossBar : bossBars.getArray(event)) {
			bossBarManager.destroyBossBar(bossBar);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "destroy bossbars " + bossBars.toString(event, debug);
	}

}
