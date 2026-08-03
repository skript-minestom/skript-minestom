package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Pattern;

@Name("Component Replace")
@Description("Replaces text in adventure components in place. Optionally replaces only the first occurrence or uses regex matching.")
@Examples("""
	component replace "foo" in {_component} with "bar\"""")
public class EffComponentReplace extends Effect {

	private static final TextReplacementConfig.Condition ONLY_ONE_REPLACEMENT = (_, _, replaced) -> {
		if (replaced < 1) return PatternReplacementResult.REPLACE;
		return PatternReplacementResult.STOP;
	};

	static {
		Skript.registerEffect(EffComponentReplace.class,
			"component replace [:first] [:regex] %strings% in %~components% with %component%");
	}

	private Expression<String> toReplace;
	private Expression<ComponentWrapper> haystack;
	private Expression<ComponentWrapper> replacement;

	private boolean first;
	private boolean regex;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		toReplace = (Expression<String>) expressions[0];
		haystack = (Expression<ComponentWrapper>) expressions[1];
		replacement = (Expression<ComponentWrapper>) expressions[2];
		first = parseResult.hasTag("first");
		regex = parseResult.hasTag("regex");
		return true;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected void execute(Event event) {
		Component replacement = ComponentWrapper.getOrElse(this.replacement, event, null);
		if (replacement == null) return;
		for (String toReplace : this.toReplace.getArray(event)) {
			Object capture = regex ? Pattern.compile(toReplace) : toReplace;
			if (capture == null) continue;
			haystack.changeInPlace(event, wrapper -> {
				if (wrapper == null) return null;
				wrapper.modify(component -> component.replaceText(builder -> {
					if (first) builder.condition(ONLY_ONE_REPLACEMENT);
					if (regex) builder.match((Pattern) capture);
					else builder.matchLiteral((String) capture);
					builder.replacement(replacement);
				}));
				return wrapper;
			});
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("component replace");
		if (first) sb.append("first");
		if (regex) sb.append("regex");
		sb.append(toReplace, "in", haystack, "with", replacement);
		return sb.toString();
	}

}
