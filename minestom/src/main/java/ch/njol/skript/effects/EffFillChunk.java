package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.sections.EffSecCreateInstance;
import ch.njol.util.Kleenean;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.UnitModifier;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.biome.Biome;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Fill Chunk")
@Description("Fills a chunk or a portion of it with a specific block within a chunk generator.")
@Examples("""
	fill chunk with stone
	fill chunk between y levels 0 and 64 with water""")
public class EffFillChunk extends Effect implements EventRestrictedSyntax {

	static {
		Skript.registerEffect(EffFillChunk.class,
			"fill [generat(or|ion)] chunk [(biome|blocks)] [1:between (y|height[s]) [level[s]] %-integer% and %-integer%] with %block/biome%");
	}

	private Expression<Integer> minHeight;
	private Expression<Integer> maxHeight;
	private Expression<?> filler;

	private boolean betweenY;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		betweenY = parseResult.mark == 1;
		minHeight = (Expression<Integer>) expressions[0];
		maxHeight = (Expression<Integer>) expressions[1];
		filler = expressions[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Object filler = this.filler.getSingle(event);
		if (filler == null) return;
		GenerationUnit unit = ((EffSecCreateInstance.TerrainGenerateEvent) event).getUnit();
		UnitModifier modifier = unit.modifier();
		if (filler instanceof Block block) {
			if (betweenY) {
				Integer min = minHeight.getSingle(event);
				if (min == null) return;
				Integer max = maxHeight.getSingle(event);
				if (max == null) return;
				modifier.fillHeight(min, max, block);
				return;
			}
			modifier.fill(block);
		} else if (filler instanceof Biome biome) {
			RegistryKey<Biome> key = MinecraftServer.getBiomeRegistry().getKey(biome);
			if (key == null) return;
			if (betweenY) {
				Integer min = minHeight.getSingle(event);
				if (min == null) return;
				Integer max = maxHeight.getSingle(event);
				if (max == null) return;
				Point start = unit.absoluteStart();
				Point end = unit.absoluteEnd();
				int startX = start.blockX();
				int startZ = start.blockZ();
				int endX = end.blockX();
				int endZ = end.blockZ();
				for (int x = startX; x < endX; x++) {
					for (int y = min; y <= max; y++) {
						for (int z = startZ; z < endZ; z++) {
							modifier.setBiome(x, y, z, key);
						}
					}
				}
				return;
			}
			modifier.fillBiome(key);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "fill chunk" +
			(betweenY ? " between y levels " + minHeight.toString(event, debug) + " and " + maxHeight.toString(event, debug) : "") +
			" with " + filler.toString(event, debug);
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return new Class[]{EffSecCreateInstance.TerrainGenerateEvent.class};
	}

}
