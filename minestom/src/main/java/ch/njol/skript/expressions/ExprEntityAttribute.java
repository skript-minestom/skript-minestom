package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

@Name("Entity Attribute")
@Description("""
	The numerical value of an entity's particular attribute(s).
	Note that the movement speed attribute cannot be reliably used for players. For that purpose, use the speed expression instead.
	Resetting an entity's attribute is only available in Minecraft 1.11 and above.""")
@Example("""
	on join:
		set player's scale attribute to 0.5
		set player's (max health and scale) attributes to 10""")
@Since("2.5, 2.6.1 (final attribute value)")
public class ExprEntityAttribute extends PropertyExpression<LivingEntity, Number> {

	static {
		Skript.registerExpression(ExprEntityAttribute.class, Number.class, ExpressionType.COMBINED,
			"[the] %attributetypes% [(1:(total|final|modified))] attribute[s] [value[s]] of %livingentities%",
			"%livingentities%'[s] %attributetypes% [(1:(total|final|modified))] attribute[s] [value[s]]");
	}

	private Expression<Attribute> attributes;
	private boolean withModifiers;

	@SuppressWarnings({"null", "unchecked"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		attributes = (Expression<Attribute>) exprs[matchedPattern];
		setExpr((Expression<? extends LivingEntity>) exprs[matchedPattern ^ 1]);
		withModifiers = parseResult.mark == 1;
		return true;
	}

	@Override
	@SuppressWarnings("null")
	protected Number[] get(Event event, LivingEntity[] entities) {
		Attribute[] attributes = this.attributes.getArray(event);
		return Stream.of(entities)
			.flatMap(entity -> Stream.of(attributes).map(entity::getAttribute))
			.map(instance -> withModifiers ? instance.getValue() : instance.getBaseValue())
			.toArray(Number[]::new);
	}

	@Override
	@Nullable
	public Class<?>[] acceptChange(ChangeMode mode) {
		if (mode == ChangeMode.REMOVE_ALL || withModifiers)
			return null;
		return CollectionUtils.array(Number.class);
	}

	@Override
	@SuppressWarnings("null")
	public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
		Attribute[] attributes = this.attributes.getArray(event);
		double deltaValue = delta == null ? 0 : ((Number) delta[0]).doubleValue();
		for (LivingEntity entity : getExpr().getArray(event)) {
			for (Attribute attribute : attributes) {
				AttributeInstance instance = entity.getAttribute(attribute);
				switch (mode) {
					case ADD:
						instance.setBaseValue(instance.getBaseValue() + deltaValue);
						break;
					case SET:
						instance.setBaseValue(deltaValue);
						break;
					case DELETE:
						instance.setBaseValue(0);
						break;
					case RESET:
						// the attribute's own default is generic (e.g. 0.7 movement speed), so prefer the entity type's default
						instance.setBaseValue(entity.getEntityType().defaultAttributes().getOrDefault(attribute, attribute.defaultValue()));
						break;
					case REMOVE:
						instance.setBaseValue(instance.getBaseValue() - deltaValue);
						break;
					case REMOVE_ALL:
						assert false;
				}
			}
		}
	}

	@Override
	public boolean isSingle() {
		return super.isSingle() && attributes.isSingle();
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	@SuppressWarnings("null")
	public String toString(@Nullable Event event, boolean debug) {
		return getExpr().toString(event, debug) + "'s " + attributes.toString(event, debug) + (withModifiers ? " final" : "") + " attribute";
	}

}
