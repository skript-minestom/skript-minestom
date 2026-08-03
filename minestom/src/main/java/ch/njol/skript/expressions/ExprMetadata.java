package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.Taggable;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operation;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;

import java.util.ArrayList;
import java.util.List;

@Name("Metadata")
@Description("The metadata of an entity or an object.")
@Examples("set metadata \"key\" of player to \"value\"")
public class ExprMetadata extends PropertyExpression<Taggable, Object> {

	static {
		register(ExprMetadata.class, Object.class, "meta[ ]data [tag] %string%", "taggables");
	}

	private Expression<String> tag;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		tag = (Expression<String>) expressions[matchedPattern];
		setExpr((Expression<Taggable>) (matchedPattern == 0 ? expressions[1] : expressions[0]));
		return true;
	}

	@Override
	protected Object[] get(Event event, Taggable[] source) {
		String tagName = tag.getSingle(event);
		if (tagName == null) return new Object[0];
		List<Object> objects = new ArrayList<>();
		Tag<Object> tag = Tag.Transient("skript-minestom:metadata:" + tagName);
		for (Taggable taggable : source) {
			if (!taggable.hasTag(tag)) continue;
			objects.add(taggable.getTag(tag));
		}
		return objects.toArray(new Object[0]);
	}

	@Override
	public Class<?> @org.jspecify.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, REMOVE, ADD, DELETE -> CollectionUtils.array(Object.class);
			default -> null;
		};
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		Object object = delta == null ? null : delta[0];
		String tagName = tag.getSingle(event);
		if (tagName == null) return;
		Tag<Object> tag = Tag.Transient("skript-minestom:metadata:" + tagName);
		for (Taggable taggable : getExpr().getArray(event)) {
			if (mode == Changer.ChangeMode.DELETE) {
				taggable.setTag(tag, null);
				continue;
			}
			if (object == null) return;
			switch (mode) {
				case ADD, REMOVE -> {
					Operator operator = mode == Changer.ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
					Object currentMetadata = taggable.getTag(tag);
					OperationInfo<?, ?, ?> info;
					if (currentMetadata != null) {
						info = Arithmetics.getOperationInfo(operator, currentMetadata.getClass(), object.getClass());
						if (info == null)
							continue;
					} else {
						info = Arithmetics.getOperationInfo(operator, object.getClass(), object.getClass());
						if (info == null)
							continue;
						currentMetadata = Arithmetics.getDefaultValue(info.getLeft());
						if (currentMetadata == null)
							continue;
					}
					//noinspection unchecked,rawtypes
					Object newValue = ((Operation) info.getOperation()).calculate(currentMetadata, object);
					taggable.setTag(tag, newValue);
				}
				case SET -> taggable.setTag(tag, object);
			}
		}
	}

	@Override
	public Class<?> getReturnType() {
		return Object.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "metadata tag " + tag.toString(event, debug) + " of " + getExpr().toString(event, debug);
	}

}
