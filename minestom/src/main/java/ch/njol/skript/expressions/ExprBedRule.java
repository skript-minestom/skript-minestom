package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.world.attribute.BedRule;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;


@Name("Bed Rule")
@Description("Creates a bed rule with sleep and spawn rules, optionally destroying the bed when it is used or left, and an optional error message.")
@Examples({
	"set {_rule} to new bed rule with sleep rule when dark and with spawn rule always",
	"set {_rule} to new bed rule with sleep rule never, spawn rule never and destroyed on use"
})
public class ExprBedRule extends SimpleExpression<BedRule> {

	static {
		Skript.registerExpression(ExprBedRule.class, BedRule.class, ExpressionType.COMBINED,
			"[new] bed rule with sleep [rule] %bedrulerule% (,|[and] with) [set] spawn [rule] %bedrulerule% [[and] use:destroy[ed] on use] [[and] leave:destroy[ed] on leave] [[and] with error [message] %-component%]");
	}

	private Expression<BedRule.Rule> sleepRule;
	private Expression<BedRule.Rule> spawnRule;
	private boolean destroyOnUse;
	private boolean destroyOnLeave;
	@Nullable
	private Expression<ComponentWrapper> errorMessage;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		sleepRule = (Expression<BedRule.Rule>) expressions[0];
		spawnRule = (Expression<BedRule.Rule>) expressions[1];
		destroyOnUse = parseResult.hasTag("use");
		destroyOnLeave = parseResult.hasTag("leave");
		errorMessage = (Expression<ComponentWrapper>) expressions[2];
		return true;
	}

	@Override
	protected BedRule[] get(Event event) {
		BedRule.Rule sleep = sleepRule.getSingle(event);
		if (sleep == null) return new BedRule[0];
		BedRule.Rule spawn = spawnRule.getSingle(event);
		if (spawn == null) return new BedRule[0];
		Component errorMessage = ComponentWrapper.getOrElse(this.errorMessage, event, null);
		return new BedRule[]{new BedRule(sleep, spawn, destroyOnUse, destroyOnLeave, errorMessage)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends BedRule> getReturnType() {
		return BedRule.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("bed rule with sleep rule", sleepRule, ", set spawn rule", spawnRule);
		if (destroyOnUse) sb.append("destroyed on use");
		if (destroyOnLeave) sb.append("destroyed on leave");
		if (errorMessage != null) sb.append("with error message", errorMessage);
		return sb.toString();
	}

}

