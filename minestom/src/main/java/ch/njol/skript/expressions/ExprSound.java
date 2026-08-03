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
import net.kyori.adventure.sound.Sound;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

@Name("Sound")
@Description("Creates a sound from an ID with optional seed, category, volume, and pitch.")
@Examples("""
	set {_sound} to sound "minecraft:entity.experience_orb.pickup" at volume 1 and pitch 1""")
public class ExprSound extends SimpleExpression<Sound> {

	static {
		Skript.registerExpression(ExprSound.class, Sound.class, ExpressionType.COMBINED,
			"sound[s] %strings% [with seed %-number%] [(in|from) %-soundcategory%] [(at|with) volume %number%] [(and|at|with) pitch %-number%]");
	}

	private Expression<String> ids;
	@Nullable
	private Expression<Number> seed;
	@Nullable
	private Expression<Sound.Source> category;
	@Nullable
	private Expression<Number> volume;
	@Nullable
	private Expression<Number> pitch;

	@SuppressWarnings("unchecked")
	@Override
	public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
		ids = (Expression<String>) expressions[0];
		seed = (Expression<Number>) expressions[1];
		category = (Expression<Sound.Source>) expressions[2];
		volume = (Expression<Number>) expressions[3];
		pitch = (Expression<Number>) expressions[4];
		return true;
	}

	@Override
	protected @Nullable Sound[] get(Event event) {
		String[] ids = this.ids.getArray(event);
		List<Sound> sounds = new ArrayList<>();
		for (String id : ids) {
			id = id.toLowerCase(Locale.ENGLISH);
			if (!id.contains(":")) id = "minecraft:" + id;
			if (!Key.parseable(id)) continue;
			Number seed = this.seed == null ? null : this.seed.getSingle(event);
			Sound.Source source = category == null ? null : category.getSingle(event);
			Number volume = this.volume == null ? null : this.volume.getSingle(event);
			Number pitch = this.pitch == null ? null : this.pitch.getSingle(event);
			sounds.add(Sound.sound()
				.type(Key.key(id))
				.seed(seed == null ? OptionalLong.empty() : OptionalLong.of(seed.longValue()))
				.source(source == null ? Sound.Source.MASTER : source)
				.volume(volume == null ? 1 : volume.floatValue())
				.pitch(pitch == null ? 1 : pitch.floatValue())
				.build());
		}
		return sounds.toArray(new Sound[0]);
	}

	@Override
	public boolean isSingle() {
		return ids.isSingle();
	}

	@Override
	public Class<? extends Sound> getReturnType() {
		return Sound.class;
	}

	@Override
	public String toString(@org.eclipse.jdt.annotation.Nullable Event event, boolean debug) {
		SyntaxStringBuilder sb = new SyntaxStringBuilder(event, debug);
		sb.append("sound", ids);
		if (seed != null) sb.append("with seed", seed);
		if (category != null) sb.append("in", category);
		if (volume != null) sb.append("at volume", volume);
		if (pitch != null) sb.append("with pitch", pitch);
		return sb.toString();
	}

}
