package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Player Skin")
@Description("The skin of a player.")
@Examples("""
	set skin of player to skin from "jeb_\"""")
public class ExprPlayerSkin extends SimplePropertyExpression<Player, PlayerSkin> {

	static {
		register(ExprPlayerSkin.class, PlayerSkin.class, "skin", "players");
	}

	@Override
	public @Nullable PlayerSkin convert(Player from) {
		return from.getSkin();
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(PlayerSkin.class);
		return null;
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		PlayerSkin skin = delta == null ? null : (PlayerSkin) delta[0];
		for (Player p : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.RESET) {
				skin = PlayerSkin.fromUuid(p.getUuid().toString());
			}
			if (skin == null) continue;
			p.setSkin(skin);
		}
	}

	@Override
	protected String getPropertyName() {
		return "skin";
	}

	@Override
	public Class<? extends PlayerSkin> getReturnType() {
		return PlayerSkin.class;
	}

}
