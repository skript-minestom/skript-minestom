package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.FileUtils;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Name("Image From File")
@Description("Loads a buffered image from a file path.")
@Examples("set {_img} to image from file \"server-icon.png\"")
public class ExprImageFromFile extends SimpleExpression<BufferedImage> {

	static {
		Skript.registerExpression(ExprImageFromFile.class, BufferedImage.class, ExpressionType.COMBINED,
			"[buffered] image[s] from file[s] %strings%");
	}

	private Expression<String> files;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		files = (Expression<String>) expressions[0];
		return true;
	}

	@Override
	protected @Nullable BufferedImage[] get(Event event) {
		List<BufferedImage> images = new ArrayList<>();
		for (String file : files.getArray(event)) {
			File f = new File(FileUtils.getServerDirectory(), file);
			if (!f.exists() || f.isDirectory()) continue;
			try {
				images.add(ImageIO.read(f));
			} catch (IOException e) {
				SkriptLogger.LOGGER.error("An error occurred whilst attempting to create an image from file '{}': {}", file, e.getMessage());
				e.printStackTrace();
			}
		}
		return images.toArray(new BufferedImage[0]);
	}

	@Override
	public boolean isSingle() {
		return files.isSingle();
	}

	@Override
	public Class<? extends BufferedImage> getReturnType() {
		return BufferedImage.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		return "buffered image from file " + files.toString(event, debug);
	}

}
