package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptminestom.SkriptMinestom;
import net.minestom.server.MinecraftServer;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;


@Name("Server Brand")
@Description("The server brand name sent to clients (shown in F3 menu).")
@Examples("""
	set server brand to "My custom server brand\"""")
public class ExprServerBrand extends SimpleExpression<String> {

	static {
		Skript.registerExpression(ExprServerBrand.class, String.class, ExpressionType.SIMPLE, "server brand [name]");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected @Nullable String[] get(Event event) {
		return new String[]{MinecraftServer.getBrandName()};
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.SET) return CollectionUtils.array(String.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.RESET) {
			MinecraftServer.setBrandName(SkriptMinestom.DEFAULT_BRAND_NAME);
			return;
		}
		String name = delta == null ? null : (String) delta[0];
		if (name == null) return;
		MinecraftServer.setBrandName(name);
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "server brand name";
	}

}
