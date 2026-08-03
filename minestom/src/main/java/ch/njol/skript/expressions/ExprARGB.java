package ch.njol.skript.expressions;

import ch.njol.skript.doc.*;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import net.kyori.adventure.util.ARGBLike;
import net.kyori.adventure.util.RGBLike;

import java.util.Locale;
import java.util.function.Function;

@Name("Alpha/Red/Green/Blue RGBLike Value")
@Description("""
	The alpha, red, green, or blue value of colors. Ranges from 0 to 255.
	Alpha represents opacity.""")
@Example("broadcast red value of rgb(100, 0, 50) # sends '100'")
@Example("set {_red} to red's red value + 10")
@Keywords({"ARGB", "RGB", "color", "colour"})
@Since("2.10")
public class ExprARGB extends SimplePropertyExpression<RGBLike, Integer> {

	static {
		register(ExprARGB.class, Integer.class, "(:alpha|:red|:green|:blue) (value|component)", "rgblike");
	}

	private RGB color;

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		color = RGB.valueOf(parseResult.tags.getFirst().toUpperCase(Locale.ENGLISH));
		return super.init(expressions, matchedPattern, isDelayed, parseResult);
	}

	@Override
	public Integer convert(RGBLike from) {
		return color.getValue(from);
	}

	@Override
	public Class<? extends Integer> getReturnType() {
		return Integer.class;
	}

	@Override
	protected String getPropertyName() {
		return color.name().toLowerCase(Locale.ENGLISH);
	}

	/**
	 * helper enum for getting argb values of {@link RGBLike}s.
	 */
	private enum RGB {
		ALPHA(rgbLike -> {
			if (rgbLike instanceof ARGBLike argbLike) return argbLike.alpha();
			return 255;
		}),
		RED(RGBLike::red),
		GREEN(RGBLike::green),
		BLUE(RGBLike::blue);

		private final Function<RGBLike, Integer> get;

		RGB(Function<RGBLike, Integer> get) {
			this.get = get;
		}

		public int getValue(RGBLike from) {
			return get.apply(from);
		}

	}

}