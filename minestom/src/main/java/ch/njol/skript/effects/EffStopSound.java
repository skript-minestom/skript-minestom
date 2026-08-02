package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Locale;

@Name("Stop Sound")
@Description({
	"Stops specific sounds, or all sounds, from playing to players or to everybody in an instance.",
	"If no sound category is provided, the sound is stopped in every category."
})
@Examples({
	"stop sound \"block.chest.open\" for player",
	"stop sounds \"ambient.cave\" and \"ambient.underwater.loop\" for all players",
	"stop all sounds in music for player",
	"stop all sounds in player's instance"
})
public class EffStopSound extends Effect {

	static {
		Skript.registerEffect(EffStopSound.class,
			"stop (all:all sound[s]|sound[s] %-strings%) [(in|from) %-soundcategory%] (to|for|from playing to) %players%",
			"stop (all:all sound[s]|sound[s] %-strings%) [(in|from) %-soundcategory%] [in [(world|instance)[s]] %instances%]");
	}

	@Nullable
	private Expression<String> sounds;
	@Nullable
	private Expression<Sound.Source> category;
	private Expression<? extends Audience> targets;
	private boolean allSounds;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		allSounds = parseResult.hasTag("all");
		sounds = (Expression<String>) expressions[0];
		category = (Expression<Sound.Source>) expressions[1];
		targets = (Expression<? extends Audience>) expressions[2];
		return true;
	}

	@Override
	protected void execute(Event event) {
		Sound.Source category = this.category == null ? null : this.category.getSingle(event);
		Audience[] targets = this.targets.getArray(event);
		if (allSounds) {
			stop(targets, category == null ? SoundStop.all() : SoundStop.source(category));
			return;
		}
		if (sounds == null) return;
		for (String id : sounds.getArray(event)) {
			id = id.toLowerCase(Locale.ENGLISH);
			if (!id.contains(":")) id = "minecraft:" + id;
			if (!Key.parseable(id)) continue;
			Key key = Key.key(id);
			stop(targets, category == null ? SoundStop.named(key) : SoundStop.namedOnSource(key, category));
		}
	}

	private void stop(Audience[] targets, SoundStop soundStop) {
		for (Audience target : targets) {
			target.stopSound(soundStop);
		}
	}

	@Override
	public String toString(@Nullable Event event, boolean debug) {
		return "stop " + (allSounds ? "all sounds" : "sound " + sounds.toString(event, debug)) +
			(category != null ? " in " + category.toString(event, debug) : "") +
			" for " + targets.toString(event, debug);
	}

}
