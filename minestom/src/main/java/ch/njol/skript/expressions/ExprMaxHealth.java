package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Max Health")
@Description("The maximum health of a living entity.")
@Examples("set max health of player to 40")
public class ExprMaxHealth extends SimplePropertyExpression<LivingEntity, Number> {

	static {
		register(ExprMaxHealth.class, Number.class, "max health", "livingentities");
	}

	@Override
	public @Nullable Number convert(LivingEntity from) {
		return from.getAttributeValue(Attribute.MAX_HEALTH);
	}

	@Override
	public Class<?> @org.jspecify.annotations.Nullable [] acceptChange(Changer.ChangeMode mode) {
		return switch (mode) {
			case SET, REMOVE, ADD, RESET -> CollectionUtils.array(Number.class);
			default -> null;
		};
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void change(Event event, @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) throws UnsupportedOperationException {
		LivingEntity[] entities = getExpr().getArray(event);
		Float health = delta == null ? null : ((Number) delta[0]).floatValue();
		for (LivingEntity entity : entities) {
			AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
			if (mode == Changer.ChangeMode.RESET) {
				maxHealth.setBaseValue(entity.getEntityType().defaultAttributes().getOrDefault(Attribute.MAX_HEALTH, Attribute.MAX_HEALTH.defaultValue()));
				continue;
			}
			if (health == null) return;
			switch (mode) {
				case ADD -> maxHealth.setBaseValue(maxHealth.getBaseValue()+health);
				case REMOVE -> maxHealth.setBaseValue(maxHealth.getBaseValue()-health);
				case SET -> maxHealth.setBaseValue(health);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "max health";
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

}
