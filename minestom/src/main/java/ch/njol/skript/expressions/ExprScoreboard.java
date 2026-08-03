package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.scoreboard.Sidebar;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("New Sidebar/Scoreboard")
@Description("Creates a new sidebar/scoreboard.")
@Examples("set {_s} to new sidebar named \"Stats\"")
public class ExprScoreboard extends SimpleExpression<Sidebar> {

	static {
		Skript.registerExpression(ExprScoreboard.class, Sidebar.class, ExpressionType.COMBINED, "[new] (score[ ]board|side[ ]bar) [(with [the] title|titled) %-component%]");
	}

	private Expression<ComponentWrapper> title;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		title = (Expression<ComponentWrapper>) expressions[0];
		return true;
	}

	@Override
	protected @Nullable Sidebar[] get(Event event) {
		Component title = ComponentWrapper.getOrElse(this.title, event, Component.empty());
		assert title != null;
		return new Sidebar[]{new Sidebar(title)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends Sidebar> getReturnType() {
		return Sidebar.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		String toString = "new scoreboard";
		if (title != null) toString += " titled " + title.toString(event, debug);
		return toString;
	}

}
