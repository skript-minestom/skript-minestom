package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.tag.Tag;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Sidebar/Scoreboard")
@Description("The sidebar/scoreboard of a player.")
@Examples("set sidebar of player to new sidebar named \"bob\"")
public class ExprPlayerScoreboard extends SimplePropertyExpression<Player, Sidebar> {

	private static final Tag<Sidebar> SCOREBOARD_TAG = Tag.Transient("skript-minestom:scoreboard");

	static {
		register(ExprPlayerScoreboard.class, Sidebar.class, "(score[ ]board|side[ ]bar)", "players");
	}

	@Override
	public @Nullable Sidebar convert(Player from) {
		return from.getTag(SCOREBOARD_TAG);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case DELETE, RESET, SET -> CollectionUtils.array(Sidebar.class);
			default -> null;
		};
	}

	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		for (Player player : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
				Sidebar sidebar = player.getTag(SCOREBOARD_TAG);
				if (sidebar == null) continue;
				player.setTag(SCOREBOARD_TAG, null);
				sidebar.removeViewer(player);
			} else {
				if (delta == null) continue;
				Sidebar sidebar = (Sidebar) delta[0];
				assert sidebar != null;
				sidebar.addViewer(player);
				player.setTag(SCOREBOARD_TAG, sidebar);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "scoreboard";
	}

	@Override
	public Class<? extends Sidebar> getReturnType() {
		return Sidebar.class;
	}

}
