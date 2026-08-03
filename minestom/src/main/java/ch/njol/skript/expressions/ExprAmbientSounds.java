package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import net.kyori.adventure.key.Key;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.world.attribute.AmbientSounds;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@Name("Ambient Sounds")
@Description("Creates ambient sounds with optional loop, mood, and additions.")
@Examples("""
	set {_sounds} to new ambient sounds with loop "minecraft:ambient.cave\"""")
public class ExprAmbientSounds extends SimpleExpression<AmbientSounds> {

	static {
		Skript.registerExpression(ExprAmbientSounds.class, AmbientSounds.class, ExpressionType.COMBINED,
			"[new] ambient sound[s] [with loop %-string%] [(,|[and] with) mood %-mood%] [(,|[and] with) addition[s] %-additions%]");
	}

	@Nullable
	private Expression<String> loop;
	@Nullable
	private Expression<AmbientSounds.Mood> mood;
	@Nullable
	private Expression<AmbientSounds.Additions> additions;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		loop = (Expression<String>) expressions[0];
		mood = (Expression<AmbientSounds.Mood>) expressions[1];
		additions = (Expression<AmbientSounds.Additions>) expressions[2];
		return true;
	}

	@Override
	protected AmbientSounds @Nullable [] get(Event event) {
		SoundEvent loop = null;
		if (this.loop != null) {
			String id = this.loop.getSingle(event);
			if (id != null) loop = getSoundEvent(id);
		}
		AmbientSounds.Mood mood = null;
		if (this.mood != null) {
			AmbientSounds.Mood m = this.mood.getSingle(event);
			if (m != null) mood = m;
		}
		List<AmbientSounds.Additions> additions = new ArrayList<>();
		if (this.additions != null) additions.addAll(Arrays.asList(this.additions.getArray(event)));

		return new AmbientSounds[]{new AmbientSounds(loop, mood, additions)};
	}

	@Override
	public boolean isSingle() {
		return true;
	}

	@Override
	public Class<? extends AmbientSounds> getReturnType() {
		return AmbientSounds.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("ambient sounds");
		if (loop != null) sb.append("with loop", loop);
		if (mood != null) sb.append("with mood", mood);
		if (additions != null) sb.append("with additions", additions);
		return sb.toString();
	}

	public static SoundEvent getSoundEvent(String input) {
		if (input == null) return null;
		input = input.toLowerCase(Locale.ENGLISH);
		if (!input.contains(":")) input = "minecraft:" + input;
		if (Key.parseable(input)) return SoundEvent.fromKey(Key.key(input));
		return null;
	}

}
