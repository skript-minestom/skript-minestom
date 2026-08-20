package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.minestom.server.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Group")
@Description("The primary group or all groups of a player")
@Example("""
	on join:
		broadcast "%group of player%" # this is the player's primary group
		broadcast "%groups of player%" # this is all of the player's groups
	""")
@Since("2.2-dev35")
public class ExprGroup extends SimpleExpression<String> {

	static {
		PropertyExpression.register(ExprGroup.class, String.class, "group[plural:s]", "players");
	}

	private boolean primary;
	private Expression<Player> players;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		players = (Expression<Player>) exprs[0];
		primary = !parseResult.hasTag("plural");
		return true;
	}

	@SuppressWarnings("null")
	@Override
	protected String[] get(Event event) {
		Player[] players = this.players.getArray(event);
		List<String> groups = new ArrayList<>();
		for (Player player : players) {
			if (primary) groups.add(LuckPermsLookup.getPrimaryGroup(player));
			else groups.addAll(LuckPermsLookup.getAllGroups(player));
		}
		return groups.toArray(new String[0]);
	}

	@Override
	public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.REMOVE_ALL || mode == Changer.ChangeMode.DELETE) return null;
		if (primary && (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET)) return CollectionUtils.array(String.class);
		if (!primary) {
			if (mode == Changer.ChangeMode.SET) return null;
			return CollectionUtils.array(String[].class);
		}
		return null;
	}

	@Override
	public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
		String[] groups = delta == null ? null : (String[]) delta;
		if (groups == null && mode != Changer.ChangeMode.RESET) return;
		for (Player player : players.getArray(event)) {
			if (mode == Changer.ChangeMode.RESET) LuckPermsLookup.setPrimaryGroup(player, "default");
			else if (primary) LuckPermsLookup.setPrimaryGroup(player, groups[0]);
			else {
				if (mode == Changer.ChangeMode.ADD) LuckPermsLookup.addGroups(player, groups);
				else LuckPermsLookup.removeGroups(player, groups);
			}
		}
	}

	@SuppressWarnings("null")
	@Override
	public boolean isSingle() {
		return players.isSingle() && primary;
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@SuppressWarnings("null")
	@Override
	public String toString(Event event, boolean debug) {
		return "group" + (primary ? "" : "s") + " of " + players.toString(event, debug);
	}

}