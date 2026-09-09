package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import static com.github.hapily04.skriptminestom.util.MessageUtils.BASIC_MINI_MESSAGE;

@Name("Formatted")
@Description("""
	Format a string into a component.
	You can also use <head64:texture> to input a custom head.""")
@Example("send formatted \"<red>hello\" to player")
public class ExprFormatted extends SimpleExpression<ComponentWrapper> {

	static {
		Skript.registerExpression(ExprFormatted.class, ComponentWrapper.class, ExpressionType.COMBINED,
			"formatted %string% [(using|with) [[tag] resolver[s]] %-tagresolvers%]");
	}

	private Expression<String> input;
	private Expression<TagResolver> resolvers;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		input = (Expression<String>) expressions[0];
		resolvers = (Expression<TagResolver>) expressions[1];
		return true;
	}

	@Override
	protected ComponentWrapper @Nullable [] get(Event event) {
		String input = this.input.getSingle(event);
		if (input == null) return new ComponentWrapper[0];
		TagResolver[] resolvers = this.resolvers == null ? new TagResolver[0] : this.resolvers.getArray(event);
		return new ComponentWrapper[]{ComponentWrapper.toWrapper(BASIC_MINI_MESSAGE.deserialize(input, resolvers))};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "formatted " + input.toString(event, debug) + (resolvers == null ? "" : " using tag resolvers " + resolvers.toString(event, debug));
	}

}
