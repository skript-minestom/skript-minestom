package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.MinecraftTag;

@Name("Tag Namespaced Key")
@Description("""
	The namespaced key of a minecraft tag, written as "namespace:value".
	Tags loaded from vanilla data always use the "minecraft" namespace.""")
@Examples("""
	broadcast namespaced key of block tag "logs"
	set {_keys::*} to namespaced keys of tags of player's tool""")
@Keywords({"tag", "key", "namespace", "minecraft tag"})
public class ExprTagKey extends SimplePropertyExpression<MinecraftTag, String> {

	static {
		register(ExprTagKey.class, String.class, "[namespace[d]] key[s]", "minecrafttags");
	}

	@Override
	public String convert(MinecraftTag from) {
		return from.key().asString();
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}

	@Override
	protected String getPropertyName() {
		return "namespaced key";
	}

}
