package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import net.kyori.adventure.util.ARGBLike;
import net.kyori.adventure.util.RGBLike;
import org.jetbrains.annotations.Nullable;

@Name("Hex Code")
@Description("""
	Returns the hexadecimal value representing the given color(s).
	The hex value of a colour does not contain a leading #, just the RRGGBB value.
	For those looking for hex values of numbers, see the asBase and fromBase functions.
	NOTE: If the provided color is an alpha color, the hex format will be AARRGGBB.""")
@Example("send formatted \"<#%hex code of rgb(100, 10, 10)%>darker red\" to all players")
@Since("2.14")
public class ExprHexCode extends SimplePropertyExpression<RGBLike, String> {

	static {
		register(ExprHexCode.class, String.class, "hex[adecimal] code", "rgblikes");
	}

	@Override
	public @Nullable String convert(RGBLike color) {
		int alpha = 255;
		if (color instanceof ARGBLike argbLike) alpha = argbLike.alpha();
		int red = color.red();
		int green = color.green();
		int blue = color.blue();
		if (alpha != 255) return String.format("%02X%02X%02X%02X", alpha, red, green, blue);
		return String.format("%02X%02X%02X", red, green, blue);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	protected String getPropertyName() {
		return "hexadecimal code";
	}

	@Override
	public Expression<? extends String> simplify() {
		if (getExpr() instanceof Literal<?>) {
			return SimplifiedLiteral.fromExpression(this);
		}
		return this;
	}

}