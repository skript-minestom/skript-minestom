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
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptminestom.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.scoreboard.Sidebar;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;

@Name("Scoreboard Line")
@Description("A specific line of a sidebar/scoreboard.")
@Examples("set line 1 of player's sidebar to \"Welcome!\"")
public class ExprScoreboardLine extends SimpleExpression<ComponentWrapper> {

	static {
		Skript.registerExpression(ExprScoreboardLine.class, ComponentWrapper.class, ExpressionType.PROPERTY,
			"line[s] %integers% of %scoreboards%", "%scoreboards%'[s] line[s] %integers%");
	}

	private Expression<Integer> lines;
	private Expression<Sidebar> scoreboards;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		int integerIndex = 0;
		int scoreboardIndex = 1;
		if (matchedPattern == 1) {
			integerIndex = 1;
			scoreboardIndex = 0;
		}
		lines = (Expression<Integer>) expressions[integerIndex];
		scoreboards = (Expression<Sidebar>) expressions[scoreboardIndex];
		return true;
	}

	@Override
	protected @Nullable ComponentWrapper[] get(Event event) {
		Integer[] lines = this.lines.getArray(event);
		Sidebar[] scoreboards = this.scoreboards.getArray(event);
		List<ComponentWrapper> components = new ArrayList<>();
		for (Sidebar sidebar : scoreboards) {
			for (Integer i : lines) {
				Sidebar.ScoreboardLine line = sidebar.getLine(i.toString());
				if (line == null) continue;
				components.add(toWrapper(line.getContent()));
			}
		}
		return components.toArray(new ComponentWrapper[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case /*RESET,*/ DELETE -> CollectionUtils.array(ComponentWrapper.class);
			case SET -> CollectionUtils.array(ComponentWrapper[].class);
			default -> null;
		};
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		Component content = null;
		Component fixedNumberFormat = null;
		if (delta != null && delta.length >= 1) content = ComponentUtils.getComponent(delta[0]);
		if (delta != null &&  delta.length >= 2) fixedNumberFormat = ComponentUtils.getComponent(delta[1]);
		Integer[] lines = this.lines.getArray(event);
		Sidebar[] scoreboards = this.scoreboards.getArray(event);
		for (Sidebar sidebar : scoreboards) {
			for (Integer i : lines) {
				String id = i.toString();
				Sidebar.ScoreboardLine line = sidebar.getLine(id);
				switch (mode) {
					/*case RESET -> {
						if (line == null) continue;
						sidebar.updateLineContent(id, Component.empty());
						sidebar.updateLineNumberFormat(id, Sidebar.NumberFormat.blank());
					}*/
					case DELETE -> {
						if (line == null) continue;
						sidebar.removeLine(id);
					}
					case SET -> {
						if (content == null) continue;
						Sidebar.NumberFormat format = (fixedNumberFormat == Component.empty() || fixedNumberFormat == null)
							? Sidebar.NumberFormat.blank() : Sidebar.NumberFormat.fixed(fixedNumberFormat);
						if (line == null) sidebar.createLine(new Sidebar.ScoreboardLine(id, content, i, format));
						else {
							if (!content.equals(line.getContent())) sidebar.updateLineContent(id, content);
							if (format != null) sidebar.updateLineNumberFormat(id, format);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isSingle() {
		return lines.isSingle() && scoreboards.isSingle();
	}

	@Override
	public Class<? extends ComponentWrapper> getReturnType() {
		return ComponentWrapper.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "line " + lines.toString(event, debug) + " of " + scoreboards.toString(event, debug);
	}

}
