package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.PlayerBlockPlaceWrapper;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtBlockPlace extends SkriptEvent {

	static {
		Skript.registerEvent("Player Block Place", EvtBlockPlace.class, PlayerBlockPlaceWrapper.class,
			"[block] plac(e|ing) [[of] %-blocks%]")
			.description("""
				Called when a player places a block.
				Optionally specify one or more block types to only listen for those blocks.""")
			.examples("""
				on block place:
				on block placing of oak planks:
				on placing of chest or trapped chest:""");
	}

	@Nullable
	private Literal<Block> blocks;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		blocks = (Literal<Block>) args[0];
		return true;
	}

	@Override
	public boolean check(Event event) {
		if (blocks == null) return true;
		Block toCheck = ((PlayerBlockPlaceWrapper) event).getEvent().getBlock();
		Block[] blocks = this.blocks.getAll();
		for (Block block : blocks) {
			if (block.equals(toCheck)) return true;
		}
		return false;
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "block place" + (blocks != null ? (" of " + blocks.toString(event, debug)) : "");
	}

}
