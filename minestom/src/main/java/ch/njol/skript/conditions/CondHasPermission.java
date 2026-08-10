package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.conditions.base.PropertyCondition.PropertyType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.minestom.server.command.CommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * @author Peter Güttinger
 */
@Name("Has Permission")
@Description("Test whether a player has a certain permission.")
@Examples("""
	if player has the permission "admin":
		send "You're attacking an admin!" to player""")
@Since("1.0")
public class CondHasPermission extends Condition {

	static {
		Skript.registerCondition(CondHasPermission.class,
			"%senders% (has|have) [the] permission[s] %strings%",
			"%senders% (doesn't|does not|do not|don't) have [the] permission[s] %strings%");
	}

	@SuppressWarnings("null")
	private Expression<String> permissions;
	@SuppressWarnings("null")
	private Expression<CommandSender> senders;

	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parseResult) {
		senders = (Expression<CommandSender>) exprs[0];
		permissions = (Expression<String>) exprs[1];
		setNegated(matchedPattern == 1);
		return true;
	}

	@Override
	public boolean check(final Event e) {
		return senders.check(e,
			s -> permissions.check(e,
				perm -> LuckPermsLookup.hasPermission(s, perm)), isNegated());
	}

	@Override
	public String toString(final @Nullable Event e, final boolean debug) {
		return PropertyCondition.toString(this, PropertyType.HAVE, e, debug, senders,
			"the permission" + (permissions.isSingle() ? " " : "s ") + permissions.toString());
	}

}
