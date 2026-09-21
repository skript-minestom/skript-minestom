package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;

@Name("Potion Effect Type")
@Description("The effect type a potion applies, such as speed or poison.")
@Examples("""
	set {_type} to effect type of {_potion}
	if effect type of {_potion} is poison:""")
public class ExprPotionEffectType extends SimplePropertyExpression<Potion, PotionEffect> {

	static {
		register(ExprPotionEffectType.class, PotionEffect.class, "[potion] effect [type]", "potions");
	}

	@Override
	public PotionEffect convert(Potion from) {
		return from.effect();
	}

	@Override
	public Class<? extends PotionEffect> getReturnType() {
		return PotionEffect.class;
	}

	@Override
	protected String getPropertyName() {
		return "potion effect type";
	}

}
