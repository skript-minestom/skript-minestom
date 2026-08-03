package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biome.Biome;
import org.bukkit.event.Event;


@Name("Biome/Dimension From Key")
@Description("A biome or dimension type from a namespace key.")
@Examples("set {_biome} to biome from namespace key \"minecraft:plains\"")
public class ExprEnvironmentFromKey extends SimpleExpression<Object> {

	static {
		Skript.registerExpression(ExprEnvironmentFromKey.class, Object.class, ExpressionType.COMBINED,
			"(:biome|dimension [type]) (from|under) [name[ ]space[d]] [key] %string%");
	}

	private Expression<String> key;
	private boolean biome;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		key = (Expression<String>) expressions[0];
		if (key instanceof Literal<String> lit) {
			String id = lit.getSingle();
			if (!Key.parseable(id)) {
				Skript.error("Provided namespace isn't parseable. Format is 'prefix:value'.");
				return false;
			}
		}
		biome = parseResult.hasTag("biome");
		return true;
	}

	@Override
	protected Object[] get(Event event) {
		String key = this.key.getSingle(event);
		if (!Key.parseable(key)) return null;
		Key k = Key.key(key);
		if (biome) {
			Biome b = MinecraftServer.getBiomeRegistry().get(k);
			if (b != null) return new Biome[]{b};
			else return new Biome[0];
		} else {
			DimensionType dimension = MinecraftServer.getDimensionTypeRegistry().get(k);
			if (dimension != null) return new DimensionType[]{dimension};
			else return new DimensionType[0];
		}
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<?> getReturnType() {
		return biome ? Biome.class : DimensionType.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return getEnvName() + " under key " + key.toString(event, debug);
	}

	private String getEnvName() {
		return biome ? "biome" : "dimension";
	}

}
