package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import static ch.njol.skript.util.ComponentWrapper.toWrapper;
import static com.github.hapily04.skriptminestom.util.NBTUtils.getTagOrElse;

@Name("Tablist Header/Footer")
@Description("The header or footer of a player's tablist, or their display name in the tablist.")
@Examples("""
	set tablist header of player to "Welcome to our server!\"""")
public class ExprTabHeaderFooter extends PropertyExpression<Player, Object> {

	private static final Tag<Component> HEADER_TAG = Tag.Transient("skript-minestom:tablist-header");
	private static final Tag<Component> FOOTER_TAG = Tag.Transient("skript-minestom:tablist-footer");

	static {
		register(ExprTabHeaderFooter.class, Object.class, "tab[[ ]list] (:header|footer|(name:([display] name))|:priority)", "players");
	}

	private boolean header;
	private boolean name;
	private boolean priority;
	private Class<?> returnType;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		setExpr((Expression<? extends Player>) expressions[0]);
		header = parseResult.hasTag("header");
		name = parseResult.hasTag("name");
		priority = parseResult.hasTag("priority");
		returnType = getReturnType();
		return true;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	protected Object[] get(Event event, Player[] source) {
		Object[] array = priority ? new Integer[source.length] : new ComponentWrapper[source.length];
		for (int i = 0; i < source.length; i++) {
			if (!priority) {
				if (!name) array[i] = toWrapper(source[i].getTag(header ? HEADER_TAG : FOOTER_TAG));
				else array[i] = toWrapper(source[i].getDisplayName());
			} else array[i] = source[i].getListOrder();
		}
		return array;
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(returnType);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		Object o = delta[0];
		Tag<Component> modifyComponent = header ? HEADER_TAG : FOOTER_TAG;
		for (Player p : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.RESET) {
				if (!priority) {
					if (!name) p.setTag(modifyComponent, Component.empty());
					else {
						p.setDisplayName(null);
						continue;
					}
				} else p.setListOrder(0);
			}
			else {
				if (o == null) return;
				if (!priority) {
					if (!name) p.setTag(modifyComponent, ((ComponentWrapper) o).getComponent());
					else {
						p.setDisplayName(((ComponentWrapper) o).getComponent());
						continue;
					}
				} else p.setListOrder((int) o);
			}
			p.sendPlayerListHeaderAndFooter(getTagOrElse(p, HEADER_TAG, Component.empty()), getTagOrElse(p, FOOTER_TAG, Component.empty()));
		}
	}

	@Override
	public Class<?> getReturnType() {
		return priority ? Integer.class : ComponentWrapper.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "tab list " + (!name ? (header ? "header" : "footer") : "name") + " of " + getExpr().toString(event, debug);
	}


}
