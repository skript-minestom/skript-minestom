package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Item;
import ch.njol.util.Kleenean;
import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

@Name("Send Notification")
@Description("Sends an advancement-style notification to the given players with a frame type, title, and icon item.")
@Examples("send task notification with title \"Quest Complete!\" and diamond as the icon to player")
public class EffSendNotification extends Effect {

	static {
		Skript.registerEffect(EffSendNotification.class,
			"send %frametype% notification (with title|titled) %component% (and|using|with) %item% as [the] icon to %players%");
	}

	private Expression<FrameType> frameType;
	private Expression<ComponentWrapper> title;
	private Expression<Item> icon;
	private Expression<Player> players;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		frameType = (Expression<FrameType>) expressions[0];
		title = (Expression<ComponentWrapper>) expressions[1];
		icon = (Expression<Item>) expressions[2];
		players = (Expression<Player>) expressions[3];
		return true;
	}

	@Override
	protected void execute(Event event) {
		FrameType type = this.frameType.getSingle(event);
		Component title = ComponentWrapper.getOrElse(this.title, event, null);
		Item icon = this.icon.getSingle(event);
		if (type == null || title == null || icon == null) return;
		ItemStack item = icon.getItem();
		if (item.material() == Material.AIR) return;
		Notification notification = new Notification(title, type, item);
		for (Player player : players.getArray(event)) {
			player.sendNotification(notification);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "send " + frameType.toString(event, debug) + " notification with title " + title.toString(event, debug)
			+ " and " + icon.toString(event, debug) + " as icon to " + players.toString(event, debug);
	}

}
