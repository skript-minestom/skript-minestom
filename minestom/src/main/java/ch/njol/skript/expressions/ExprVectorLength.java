package ch.njol.skript.expressions;


import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import net.minestom.server.coordinate.Vec;

@Name("Vectors - Length")
@Description("Gets or sets the length of a vector.")
@Examples("""
	send "%standard length of vector 1, 2, 3%\"""")
@Since("2.2-dev28")
public class ExprVectorLength extends SimplePropertyExpression<Vec, Number> {

	static {
		register(ExprVectorLength.class, Number.class, "(vector|standard|normal) length[s]", "vectors");
	}

	@Override
	@SuppressWarnings("unused")
	public Number convert(Vec vector) {
		return vector.length();
	}

	@Override
	public Class<? extends Number> getReturnType() {
		return Number.class;
	}

	@Override
	protected String getPropertyName() {
		return "vector length";
	}

}
