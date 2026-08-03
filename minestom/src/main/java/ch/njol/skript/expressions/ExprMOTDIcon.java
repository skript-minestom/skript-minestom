package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.events.wrapper.ServerListPingWrapper;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


@Name("MOTD Icon")
@Description("The favicon image in the server list ping event.")
@Examples("set motd favicon to image from file \"server-icon.png\"")
public class ExprMOTDIcon extends SimpleExpression<BufferedImage> implements EventRestrictedSyntax {

	static {
		Skript.registerExpression(ExprMOTDIcon.class, BufferedImage.class, ExpressionType.EVENT,
			"motd (image|[fav]icon)");
	}

	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		return true;
	}

	@Override
	protected @Nullable BufferedImage[] get(Event event) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		byte[] favicon = e.getStatus().favicon();
		if (favicon != null) {
			try {
				return new BufferedImage[]{ImageIO.read(new ByteArrayInputStream(favicon))};
			} catch (IOException ex) {
				SkriptLogger.LOGGER.error("An error occurred whilst attempting to read the motd favicon: {}", ex.getMessage());
				ex.printStackTrace();
			}
		}
		return new BufferedImage[0];
	}

	@Override
	public @org.eclipse.jdt.annotation.Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
		if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.SET) return CollectionUtils.array(BufferedImage.class);
		return null;
	}

	@Override
	public void change(Event event, @Nullable @org.eclipse.jdt.annotation.Nullable Object[] delta, Changer.ChangeMode mode) {
		ServerListPingEvent e = ((ServerListPingWrapper) event).getEvent();
		Status currentStatus = e.getStatus();
		byte[] favicon;
		if (mode == Changer.ChangeMode.RESET) {
			favicon = null;
		} else {
			BufferedImage image = delta == null ? null : (BufferedImage) delta[0];
			if (image == null) return;
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				ImageIO.write(image, "png", out);
				favicon = out.toByteArray();
			} catch (IOException ex) {
				SkriptLogger.LOGGER.error("An error occurred whilst attempting to set the motd favicon: {}", ex.getMessage());
				ex.printStackTrace();
				return;
			}
		}
		Status newStatus = Status.builder(currentStatus)
			.favicon(favicon)
			.build();
		e.setStatus(newStatus);
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends BufferedImage> getReturnType() {
		return BufferedImage.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "motd favicon";
	}

	@Override
	public Class<? extends Event>[] supportedEvents() {
		return new Class[]{ServerListPingWrapper.class};
	}

}
