package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.effects.EffChange;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.parser.ParsingStack;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Camera Target")
@Description("The camera target of a player")
@Examples("set camera target of player to {_entity}")
public class ExprCamera extends SimplePropertyExpression<Player, Entity> {

	static {
		register(ExprCamera.class, Entity.class, "camera [target]", "players");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		if (!inEffChange(this)) {
			Skript.error("You can't get the camera target of a player.");
			return false;
		}
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable Entity convert(Player from) {
		return null;
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(Entity.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Entity target = delta == null ? null : (Entity) delta[0];
		for (Player player : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.SET) {
				if (target == null) return;
				player.spectate(target);
			} else player.stopSpectating();
		}
	}

	@Override
	protected String getPropertyName() {
		return "camera target";
	}

	@Override
	public Class<? extends Entity> getReturnType() {
		return Entity.class;
	}

	// todo maybe change this to only check 1st ParsingStack element
	public static boolean inEffChange(SyntaxElement syntaxElement) {
		for (ParsingStack.Element element : syntaxElement.getParser().getParsingStack()) {
			if (!EffChange.class.isAssignableFrom(element.getSyntaxElementClass())) continue;
			return true;
		}
		return false;
	}

}
