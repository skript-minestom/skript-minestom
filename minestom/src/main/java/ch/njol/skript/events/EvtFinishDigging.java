package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.events.wrapper.*;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.instance.block.Block;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtFinishDigging extends SkriptEvent {

	static {
		Class<? extends Event>[] eventTypes = CollectionUtils.array(PlayerFinishDiggingWrapper.class, PacketPlayerFinishDiggingEvent.class);
		Skript.registerEvent("Player Finish Digging", EvtFinishDigging.class, eventTypes,
				"[:accurate] [player] finish digging [[of] %-blocks%]")
			.description("""
				Called when a player finishes digging on a block.
				Optionally specify one or more block types to only listen for those blocks.
				By default, this event will fire even if the player finishes digging on a fake block.""")
			.examples("on player finish digging");
	}

	@Nullable
	private Literal<Block> blocks;

	private boolean accurate;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
		blocks = (Literal<Block>) args[0];
		accurate = parseResult.hasTag("accurate");
		return true;
	}

	@Override
	public boolean check(Event event) {
		Block block;
		if (event instanceof PacketPlayerFinishDiggingEvent e) {
			if (accurate) return false;
			block = e.getBlock();
		} else {
			PlayerFinishDiggingWrapper e = (PlayerFinishDiggingWrapper) event;
			if (!accurate) return false;
			block = e.getEvent().getBlock();
		}
		if (blocks == null) return true;
		return blocks.check(event, block::equals);
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return (accurate ? "accurate " : "") + "player finish digging" + (blocks == null ? "" : " " + blocks.toString(event, debug));
	}

}
