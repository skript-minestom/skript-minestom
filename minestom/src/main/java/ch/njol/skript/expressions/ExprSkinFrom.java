package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.Validate;
import net.minestom.server.entity.PlayerSkin;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Skin From")
@Description("Returns a skin from a player name, UUID, or signature.")
@Examples("""
	set skin of player to skin from "jeb_\"""")
public class ExprSkinFrom extends SimpleExpression<PlayerSkin> {

	static {
		Skript.registerExpression(ExprSkinFrom.class, PlayerSkin.class, ExpressionType.COMBINED,
			"skin from [(uuid|[user]name)[s]] %strings%",
			"skin from texture[s] %string% and signature %string%");
	}

	private Expression<String> uuidName;
	private Expression<String> texture;
	private Expression<String> signature;

	private int pattern;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		pattern = matchedPattern;
		if (matchedPattern == 0) uuidName = (Expression<String>) expressions[0];
		else {
			texture = (Expression<String>) expressions[0];
			signature = (Expression<String>) expressions[1];
		}
		return true;
	}

	@Override
	protected @Nullable PlayerSkin[] get(Event event) {
		if (pattern == 1) {
			String texture = this.texture.getSingle(event);
			String signature = this.signature.getSingle(event);
			if (texture == null || signature == null) return new PlayerSkin[0];
			return new PlayerSkin[]{new PlayerSkin(texture, signature)};
		}
		List<PlayerSkin> skins = new ArrayList<>();
		for (String uuidName : uuidName.getArray(event)) {
			PlayerSkin skin = null;
			if (!uuidName.contains("-")) skin = PlayerSkin.fromUsername(uuidName);
			else if (Validate.isUUID(uuidName)) skin = PlayerSkin.fromUuid(uuidName);
			if (skin != null) skins.add(skin);
		}
		return skins.toArray(new PlayerSkin[0]);
	}

	@Override
	public boolean isSingle() {
		return uuidName == null || uuidName.isSingle();
	}

	@Override
	public Class<? extends PlayerSkin> getReturnType() {
		return PlayerSkin.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		if (pattern == 0) return "skin from " + uuidName.toString(event, debug);
		else return "skin from textures " + texture.toString(event, debug) + " and signature " + signature.toString(event, debug);
	}

}
