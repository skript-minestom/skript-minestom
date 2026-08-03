package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.NBTCompound;
import ch.njol.util.Kleenean;
import com.github.hapily04.skriptminestom.util.FileUtils;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@Name("Save NBT")
@Description("Saves an NBT compound to a gzip-compressed file in the server directory.")
@Examples("""
	save {_nbt} to file "data/player.dat\"""")
public class EffSaveNBT extends Effect {

	static {
		Skript.registerEffect(EffSaveNBT.class, "save %nbtcompound% (in|to) file[s] %strings%");
	}

	private Expression<NBTCompound> compound;
	private Expression<String> files;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		compound = (Expression<NBTCompound>) expressions[0];
		files = (Expression<String>) expressions[1];
		return true;
	}

	@Override
	protected void execute(Event event) {
		NBTCompound compound = this.compound.getSingle(event);
		if (compound == null) return;
		CompoundBinaryTag compoundBinaryTag = compound.getCompound();
		for (String file : files.getArray(event)) {
			File f = FileUtils.defendFile(new File(FileUtils.getServerDirectory(), file));
			try (FileOutputStream output = new FileOutputStream(f)) {
				BinaryTagIO.writer().write(compoundBinaryTag, output, BinaryTagIO.Compression.GZIP);
			} catch (FileNotFoundException e) {
				SkriptLogger.LOGGER.error("Couldn't find file at '{}' while attempting to create an nbt compound.", file);
			} catch (IOException e) {
				SkriptLogger.LOGGER.error("An error occurred whilst trying to save nbt to file '{}': {}", file, e.getMessage());
			}
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "save " + compound.toString(event, debug) + " in file " + files.toString(event, debug);
	}

}
