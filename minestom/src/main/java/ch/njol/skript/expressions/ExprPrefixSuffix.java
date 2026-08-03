package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsPlayer;
import net.minestom.server.entity.Player;
import org.jspecify.annotations.Nullable;

// todo of groups and changeable

@Name("Prefix/Suffix")
@Description("A player's LuckPerms prefix or suffix.")
@Examples("""
	broadcast "%prefix of player%\"""")
public class ExprPrefixSuffix extends SimplePropertyExpression<Player, String> {

	static {
		register(ExprPrefixSuffix.class, String.class, "(:prefix|suffix)", "players");
	}

	private boolean prefix;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		prefix = parseResult.hasTag("prefix");
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public @Nullable String convert(Player from) {
		LuckPermsPlayer player = (LuckPermsPlayer) from;
		return prefix ? player.getPrefix() : player.getSuffix();
	}

	@Override
	protected String getPropertyName() {
		return prefix ? "prefix" : "suffix";
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

}
