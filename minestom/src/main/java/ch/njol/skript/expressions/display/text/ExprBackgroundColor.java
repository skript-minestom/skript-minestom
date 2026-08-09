package ch.njol.skript.expressions.display.text;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.color.Color;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.bukkit.event.Event;

@Name("Background Color")
@Description("The background color of a text display entity.")
@Examples("set background color of {-a} to rgb(255, 255, 255, 128)")
public class ExprBackgroundColor extends SimplePropertyExpression<Entity, RGBLike> {

	static {
		register(ExprBackgroundColor.class, RGBLike.class, "background color", "entities");
	}

	@Override
	public Color convert(Entity from) {
		if (!(from.getEntityMeta() instanceof TextDisplayMeta meta)) return null;
		return new AlphaColor(meta.getBackgroundColor());
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.RESET) return CollectionUtils.array(RGBLike.class);
		return null;
	}

	@Override
	public void change(Event event, @org.jspecify.annotations.Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		RGBLike color = delta == null ? null : (RGBLike) delta[0];
		for (Entity entity : getExpr().getArray(event)) {
			if (!(entity.getEntityMeta() instanceof TextDisplayMeta meta)) continue;
			switch (mode) {
				case SET -> {
					if (color == null) return;
					if (!(color instanceof AlphaColor)) color = new AlphaColor(255, color);
					meta.setBackgroundColor(((AlphaColor) color).asARGB());
				}
				case RESET -> meta.setBackgroundColor(0x40000000);
			}
		}
	}

	@Override
	protected String getPropertyName() {
		return "background color";
	}

	@Override
	public Class<? extends RGBLike> getReturnType() {
		return RGBLike.class;
	}

}
