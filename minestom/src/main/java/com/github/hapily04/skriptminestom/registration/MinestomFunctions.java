package com.github.hapily04.skriptminestom.registration;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.particle.*;
import ch.njol.skript.lang.function.*;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.skript.util.ComponentWrapper;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Validate;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.color.Color;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.Music;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.world.attribute.AmbientParticle;
import net.minestom.server.world.attribute.AmbientSounds;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.common.function.DefaultFunction;

import java.util.Locale;
import java.util.UUID;

import static ch.njol.skript.expressions.ExprAmbientSounds.getSoundEvent;
import static ch.njol.skript.util.ComponentWrapper.toWrapper;
import static com.github.hapily04.skriptminestom.util.MessageUtils.BASIC_MINI_MESSAGE;

public class MinestomFunctions {

	public static void register() {
		Parameter<Number> xParam = new Parameter<>("x", DefaultClasses.NUMBER, true, null);
		Parameter<Number> yParam = new Parameter<>("y", DefaultClasses.NUMBER, true, null);
		Parameter<Number> zParam = new Parameter<>("z", DefaultClasses.NUMBER, true, null);
		Functions.registerFunction(new SimpleJavaFunction<>("position", new Parameter[]{
			xParam,
			yParam,
			zParam,
			new Parameter<>("yaw", DefaultClasses.NUMBER, true, new SimpleLiteral<Number>(0, true)),
			new Parameter<>("pitch", DefaultClasses.NUMBER, true, new SimpleLiteral<Number>(0, true))
		}, Classes.getExactClassInfo(Pos.class), true) {
			@SuppressWarnings("NullableProblems")
			@Override
			public @Nullable Pos @NotNull [] executeSimple(Object[][] params) {
				if (parametersNull(params, 2)) return new Pos[0];
				Number x = (Number) params[0][0];
				Number y = (Number) params[1][0];
				Number z = (Number) params[2][0];
				Number yaw = (Number) params[3][0];
				Number pitch = (Number) params[4][0];
				return new Pos[]{new Pos(x.doubleValue(), y.doubleValue(), z.doubleValue(), yaw.floatValue(), pitch.floatValue())};
			}
		}).description("Creates a position with the given x, y, z, yaw and pitch.").examples("set {_pos} to position(0, 64, 0, 90, 0)");
		Functions.registerFunction(new SimpleJavaFunction<>("vector", new Parameter[]{
			xParam,
			yParam,
			zParam
		}, Classes.getExactClassInfo(Vec.class), true) {
			@Override
			public @Nullable Vec @NotNull [] executeSimple(Object[][] params) {
				if (parametersNull(params, 2)) return new Vec[0];
				Number x = (Number) params[0][0];
				Number y = (Number) params[1][0];
				Number z = (Number) params[2][0];
				return new Vec[]{new Vec(x.doubleValue(), y.doubleValue(), z.doubleValue())};
			}
		}).description("Creates a vector with the given x, y and z.").examples("set {_vec} to vector(1, 0, 0)");
		Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "blockVector", BlockVec.class)
			.description("Creates a block vector with integer x, y, and z coordinates.")
			.examples("set {_bv} to blockVector(0, 64, 0)")
			.parameter("x", Integer.class)
			.parameter("y", Integer.class)
			.parameter("z", Integer.class)
			.build(args -> {
				int x = args.get("x");
				int y = args.get("y");
				int z = args.get("z");
				return new BlockVec(x, y ,z);
			}));
		/*Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "mm", ComponentWrapper.class)
			.parameter("input", String.class)
			.parameter("resolvers", TagResolver[].class, org.skriptlang.skript.common.function.Parameter.Modifier.OPTIONAL)
			.build(args -> {
				String input = args.get("input");
				TagResolver[] resolvers = args.getOrDefault("resolvers", new TagResolver[0]);
				System.out.println("resolvers: " + Arrays.toString(resolvers));
				return toWrapper(BASIC_MINI_MESSAGE.deserialize(input, resolvers));
			}));*/
		Functions.registerFunction(new JavaFunction<>("mm", new Parameter[]{
			new Parameter<>("input", DefaultClasses.STRING, true, null),
			new Parameter<>("resolvers", Classes.getExactClassInfo(TagResolver.class), false, new SimpleLiteral<>(new TagResolver[0], TagResolver.class, true))
		}, Classes.getExactClassInfo(ComponentWrapper.class), true) {
			@Override
			public @Nullable ComponentWrapper[] execute(FunctionEvent<?> e, Object[][] params) {
				if (parametersNull(params, 0)) return new ComponentWrapper[0];
				String input = (String) params[0][0];
				TagResolver[] resolvers = (TagResolver[]) params[1];
				return new ComponentWrapper[]{toWrapper(BASIC_MINI_MESSAGE.deserialize(input, resolvers))};
			}
		})
			.description("Deserializes a MiniMessage string into a Component, with optional tag resolvers.\n You can also use <head64:texture> to input a custom head.")
			.examples("send mm(\"<red>Hello <name>!\", resolver(\"name\", player's name))");
		Functions.registerFunction(new JavaFunction<>("suggestionEntry", new Parameter[]{
			new Parameter<>("entry", DefaultClasses.STRING, true, null),
			new Parameter<>("tooltip", Classes.getExactClassInfo(ComponentWrapper.class), true, null) // todo provide default value of null
		}, Classes.getExactClassInfo(SuggestionEntry.class), true) {
			@Override
			public @Nullable SuggestionEntry[] execute(FunctionEvent<?> e, Object[][] params) {
				if (parametersNull(params, 0)) return new SuggestionEntry[0];
				String entry = (String) params[0][0];
				ComponentWrapper tooltip = (ComponentWrapper) params[1][0];
				return new SuggestionEntry[]{new SuggestionEntry(entry, tooltip.getComponent())};
			}
		}).description("Creates a command suggestion entry with an optional tooltip component.")
			.examples("""
				command /home:
					argument <home: string>:
						suggestions:
							loop indices of {homes::%player's uuid%::*}:
								add suggestionEntry(loop-value, mm("<green>%{homes::%player's uuid%::%loop-value%}%")) to suggestions
						trigger:
							if {homes::%player's uuid%::%{_home}%} isn't set:
								send "Home '%{_home}%' doesn't exist."
								stop
							send "Teleporting you to home '%{_home}%'..."
							teleport player to {homes::%player's uuid%::%{_home}%}
					trigger:
						send "Usage: /home <home-name>\"""");
		/*Functions.registerFunction(new SimpleJavaFunction<TagResolver>("tagresolver", new Parameter<>[] {

		}) {
		});*/
		Functions.registerFunction(new SimpleJavaFunction<>("rgb", new Parameter[]{
					 new Parameter<>("red", DefaultClasses.LONG, true, null),
					 new Parameter<>("green", DefaultClasses.LONG, true, null),
					 new Parameter<>("blue", DefaultClasses.LONG, true, null),
					 new Parameter<>("alpha", DefaultClasses.LONG, true, new SimpleLiteral<>(255L, true))
				 }, Classes.getExactClassInfo(Color.class), true) {
					 @Override
					 public Color[] executeSimple(Object[][] params) {
						 if (parametersNull(params, 2)) return new Color[0];
						 Long red = (Long) params[0][0];
						 Long green = (Long) params[1][0];
						 Long blue = (Long) params[2][0];
						 Long alpha = (Long) params[3][0];
						 return CollectionUtils.array(new AlphaColor(alpha.intValue(), red.intValue(), green.intValue(), blue.intValue()));
					 }
				 }).description("Returns a RGB color from the given red, green and blue parameters. Alpha values can be added optionally, " +
					 "but these only take affect in certain situations, like text display backgrounds.")
				 .examples("set background color of {_text-display} to rgb(10, 50, 100, 50)")
				 .since("2.5, 2.10 (alpha)");

		// Particle Data
		Functions.registerFunction(new SimpleJavaFunction<>("dustOption", new Parameter[]{
			new Parameter<>("color", Classes.getExactClassInfo(RGBLike.class), true, null),
			new Parameter<>("size", DefaultClasses.NUMBER, true, new SimpleLiteral<Number>(1f, true))
		}, Classes.getExactClassInfo(DustOption.class), true) {
			@Override
			public DustOption[] executeSimple(Object[][] params) {
				if (parametersNull(params, 0)) return new DustOption[0];
				RGBLike color = (RGBLike) params[0][0];
				Number size = (Number) params[1][0];
				return CollectionUtils.array(new DustOption(color, size.floatValue()));
			}
		}).description("Creates dust options with the given color and size.").examples("set {_data} to dustOption(red, 1)");
		Functions.registerFunction(new SimpleJavaFunction<>("dustTransition", new Parameter[]{
			new Parameter<>("color", Classes.getExactClassInfo(RGBLike.class), true, null),
			new Parameter<>("transition-color", Classes.getExactClassInfo(RGBLike.class), true, null),
			new Parameter<>("size", DefaultClasses.NUMBER, true, new SimpleLiteral<Number>(1f, true))
		}, Classes.getExactClassInfo(DustTransition.class), true) {
			@Override
			public DustTransition[] executeSimple(Object[][] params) {
				if (parametersNull(params, 1)) return new DustTransition[0];
				RGBLike color = (RGBLike) params[0][0];
				RGBLike transitionColor = (RGBLike) params[1][0];
				Number size = (Number) params[2][0];
				return CollectionUtils.array(new DustTransition(color, transitionColor, size.floatValue()));
			}
		}).description("Creates a dust transition with the given color, transition color and size.").examples("set {_data} to dustTransition(red, blue, 1)");
		Functions.registerFunction(new SimpleJavaFunction<>("entityVibrationData", new Parameter[]{
			new Parameter<>("entity", Classes.getExactClassInfo(Entity.class), true, null),
			new Parameter<>("travel-time", Classes.getExactClassInfo(Timespan.class), true, null)
		}, Classes.getExactClassInfo(VibrationData.class), true) {
			@Override
			public VibrationData[] executeSimple(Object[][] params) {
				if (parametersNull(params, 1)) return new VibrationData[0];
				Entity entity = (Entity) params[0][0];
				Timespan travelTime = (Timespan) params[1][0];
				return CollectionUtils.array(new VibrationData(Particle.Vibration.SourceType.ENTITY, null,
					entity.getEntityId(), (float) entity.getEyeHeight(), (int) NumberUtils.ticksFrom(travelTime)));
			}
		}).description("Creates vibration data targeting an entity.").examples("set {_data} to entityVibrationData(player, 5 seconds)");
		Functions.registerFunction(new SimpleJavaFunction<>("blockVibrationData", new Parameter[]{
			new Parameter<>("block", Classes.getExactClassInfo(Point.class), true, null),
			new Parameter<>("travel-time", Classes.getExactClassInfo(Timespan.class), true, null)
		}, Classes.getExactClassInfo(VibrationData.class), true) {
			@Override
			public VibrationData[] executeSimple(Object[][] params) {
				if (parametersNull(params, 1)) return new VibrationData[0];
				Point point = (Point) params[0][0];
				Timespan travelTime = (Timespan) params[1][0];
				return CollectionUtils.array(new VibrationData(Particle.Vibration.SourceType.BLOCK, point,
					-1, 0, (int) NumberUtils.ticksFrom(travelTime)));
			}
		}).description("Creates vibration data targeting a block.").examples("set {_data} to blockVibrationData(blockVector(0, 64, 0), 5 seconds)");
		Functions.registerFunction(new SimpleJavaFunction<>("trailData", new Parameter[]{
			new Parameter<>("target", Classes.getExactClassInfo(Point.class), true, null),
			new Parameter<>("color", Classes.getExactClassInfo(RGBLike.class), true, null),
			new Parameter<>("duration", Classes.getExactClassInfo(Timespan.class), true, null)
		}, Classes.getExactClassInfo(TrailData.class), true) {
			@Override
			public TrailData[] executeSimple(Object[][] params) {
				if (parametersNull(params, 2)) return new TrailData[0];
				Point target = (Point) params[0][0];
				RGBLike color = (RGBLike) params[1][0];
				Timespan duration = (Timespan) params[2][0];
				return CollectionUtils.array(new TrailData(target, color, (int) NumberUtils.ticksFrom(duration)));
			}
		}).description("Creates trail data for a trial particle.").examples("set {_data} to trailData(point(0, 64, 0), red, 5 seconds)");
		Functions.registerFunction(new SimpleJavaFunction<>("effectData", new Parameter[]{
			new Parameter<>("color", Classes.getExactClassInfo(RGBLike.class), true, null),
			new Parameter<>("power", Classes.getExactClassInfo(Number.class), true, null)
		}, Classes.getExactClassInfo(EffectData.class), true) {
			@Override
			public EffectData[] executeSimple(Object[][] params) {
				if (parametersNull(params, 1)) return new EffectData[0];
				RGBLike color = (RGBLike) params[0][0];
				Number power = (Number) params[1][0];
				return CollectionUtils.array(new EffectData(color, power.floatValue()));
			}
		}).description("Creates effect data for an effect particle.").examples("set {_data} to effectData(red, 1)");
		Functions.registerFunction(new SimpleJavaFunction<>("resolver", new Parameter[]{
			new Parameter<>("name", DefaultClasses.STRING, true, null),
			new Parameter<>("value", Classes.getExactClassInfo(Object.class), true, null),
			new Parameter<>("parsed", DefaultClasses.BOOLEAN, true, new SimpleLiteral<>(false, true))
		}, Classes.getExactClassInfo(TagResolver.class), true) {
			@Override
			public TagResolver[] executeSimple(Object[][] params) {
				if (parametersNull(params, 1)) return new TagResolver[0];
				String name = (String) params[0][0];
				Object value = params[1][0];
				if (value instanceof ComponentWrapper wrapper) value = wrapper.getComponent();
				boolean parsed = (boolean) params[2][0];
				if (value instanceof String s) return CollectionUtils.array(parsed ? Placeholder.parsed(name, s) : Placeholder.unparsed(name, s));
				if (value instanceof ComponentLike c) return CollectionUtils.array(Placeholder.component(name, c));
				return new TagResolver[0];
			}
		}).description("Creates a MiniMessage tag resolver.").examples("set {_resolver} to resolver(\"name\", player's name)");
		Functions.registerFunction(new SimpleJavaFunction<>("player", new Parameter[]{
			new Parameter<>("from", DefaultClasses.STRING, true, null),
			new Parameter<>("strict", DefaultClasses.BOOLEAN, true, new SimpleLiteral<>(false, true))
		}, Classes.getExactClassInfo(Player.class), true) {
			@Override
			public Player[] executeSimple(Object[][] params) {
				if (parametersNull(params, 0)) return new Player[0];
				String input = (String) params[0][0];
				boolean strict = (boolean) params[1][0];
				return new Player[]{findPlayer(input, strict)};
			}
		}).description("Find an online player from their username or UUID.").examples("send \"test\" to player(\"bob\")");
		Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "mood", AmbientSounds.Mood.class)
			.description("Creates ambient mood sound data for biome effects.")
			.examples("set {_mood} to mood(\"ambient.cave\", 1 second, 8, 2)")
			.parameter("sound-id", String.class)
			.parameter("delay", Timespan.class)
			.parameter("block-search-extent", Integer.class)
			.parameter("offset", Number.class)
			.build(args -> {
				String soundId = args.get("sound-id");
				SoundEvent sound = getSoundEvent(soundId);
				if (sound == null) return null;
				Timespan delay = args.get("delay");
				if (delay == null) return null;
				int tickDelay = Math.toIntExact(NumberUtils.ticksFrom(delay));
				Integer blockSearchExtent = args.get("block-search-extent");
				if (blockSearchExtent == null) return null;
				Number offset = args.get("offset");
				if (offset == null) return null;
				return new AmbientSounds.Mood(sound, tickDelay, blockSearchExtent, offset.doubleValue());
			}));
		Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "additions", AmbientSounds.Additions.class)
			.description("Creates ambient addition sound data for biome effects.")
			.examples("set {_additions} to additions(\"ambient.cave\", 0.01)")
			.parameter("sound-id", String.class)
			.parameter("tick-chance", Number.class)
			.build(args -> {
				String soundId = args.get("sound-id");
				SoundEvent sound = getSoundEvent(soundId);
				if (sound == null) return null;
				Number tickChance = args.get("tick-chance");
				if (tickChance == null) return null;
				return new AmbientSounds.Additions(sound, tickChance.doubleValue());
			}));
		Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "music", Music.class)
			.description("Creates background music data with min/max delay and whether it replaces current music.")
			.examples("set {_music} to music(\"music.overworld\", 1 minute, 2 minutes, true)")
			.parameter("sound-id", String.class)
			.parameter("min-delay", Timespan.class)
			.parameter("max-delay", Timespan.class)
			.parameter("replaces-current-music", Boolean.class)
			.build(args -> {
				String soundId = args.get("sound-id");
				SoundEvent sound = getSoundEvent(soundId);
				if (sound == null) return null;
				Timespan minDelay = args.get("min-delay");
				if (minDelay == null) return null;
				Timespan maxDelay = args.get("max-delay");
				if (maxDelay == null) return null;
				Boolean replacesCurrentMusic = args.get("replaces-current-music");
				if (replacesCurrentMusic == null) return null;
				return new Music(sound, Math.toIntExact(NumberUtils.ticksFrom(minDelay)),
					Math.toIntExact(NumberUtils.ticksFrom(maxDelay)), replacesCurrentMusic);
			}));
		Functions.register(DefaultFunction.builder(Skript.getAddonInstance(), "ambientParticle", AmbientParticle.class)
			.description("Creates ambient particle data with a spawn probability for biome effects.")
			.examples("set {_particle} to ambientParticle(white ash, 0.118093334)")
			.parameter("particle", Particle.class)
			.parameter("probability", Number.class)
			.build(args -> {
				Particle particle = args.get("particle");
				if (particle == null) return null;
				Number probability = args.get("probability");
				if (probability == null) return null;
				return new AmbientParticle(particle, probability.floatValue());
			}));
	}

	static Player findPlayer(String input, boolean strict) {
		ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
		Player player;
		if (input.contains("-") && Validate.isUUID(input)) player = connectionManager.getOnlinePlayerByUuid(UUID.fromString(input));
		else {
			player = connectionManager.getOnlinePlayerByUsername(input);
			if (!strict && player == null) {
				input = input.toLowerCase(Locale.ENGLISH);
				for (Player p : connectionManager.getOnlinePlayers()) {
					if (p.getUsername().toLowerCase(Locale.ENGLISH).contains(input)) {
						player = p;
						break;
					}
				}
			}
		}
		return player;
	}

	private static boolean parametersNull(Object[][] params, int toIndex) {
		for (int i = 0; i <= toIndex; i++) {
			if (params[i].length == 0) return true;
		}
		return false;
	}

}
