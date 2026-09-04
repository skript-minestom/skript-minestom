package com.github.hapily04.skriptminestom;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.bstats.charts.SimplePie;
import ch.njol.skript.bstats.charts.SingleLineChart;
import ch.njol.skript.events.EffectCommandEvent;
import ch.njol.skript.events.UnknownCommandEvent;
import ch.njol.skript.events.minestom.CustomConnectEvent;
import ch.njol.skript.events.wrapper.EventWrapper;
import ch.njol.skript.events.wrapper.marker.MarkerRegistration;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.variables.Variables;
import com.github.hapily04.skriptminestom.bukkit.BukkitServer;
import com.github.hapily04.skriptminestom.command.SkriptCommand;
import com.github.hapily04.skriptminestom.command.StopCommand;
import com.github.hapily04.skriptminestom.luckperms.DummyContextProvider;
import com.github.hapily04.skriptminestom.luckperms.HoconConfigurationAdapter;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsPlayer;
import com.github.hapily04.skriptminestom.registration.MinestomClasses;
import com.github.hapily04.skriptminestom.registration.MinestomFunctions;
import com.github.hapily04.skriptminestom.terminal.MinestomTerminal;
import com.github.hapily04.skriptminestom.update.MinestomUpdateService;
import com.github.hapily04.skriptminestom.util.FileUtils;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import com.github.hapily04.skriptminestom.util.PropertyUtils;
import me.lucko.luckperms.common.config.generic.adapter.EnvironmentVariableConfigAdapter;
import me.lucko.luckperms.common.config.generic.adapter.MultiConfigurationAdapter;
import me.lucko.luckperms.minestom.CommandRegistry;
import me.lucko.luckperms.minestom.LuckPermsMinestom;
import me.lucko.spark.minestom.SparkMinestom;
import mx.kenzie.mirror.ConstructorAccessor;
import mx.kenzie.mirror.Mirror;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minestom.server.Auth;
import net.minestom.server.Git;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.thread.TickThread;
import net.minestom.server.timer.TaskSchedule;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

import static com.github.hapily04.skriptminestom.util.MessageUtils.BASIC_MINI_MESSAGE;
import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;
import static com.github.hapily04.skriptminestom.util.PropertyUtils.*;

public class SkriptMinestom {

	public static final String DEFAULT_BRAND_NAME = "Skript-Minestom";

	private static Properties properties;
	private static LuckPerms luckPerms;
	private static SparkMinestom spark;
	private static Metrics metrics;
	private static volatile Thread schedulerThread;
	private static boolean isServerJar = false;

	static void main() {
		isServerJar = true;
		Bukkit.setServer(new BukkitServer()); // serverDirectory is resolved from this, so it needs to be set immediately

		properties = PropertyUtils.loadServerProperties();
		initMinestomProperties();
		MinecraftServer server = MinecraftServer.init(PropertyUtils.getAuth(properties));

		luckPerms = initLuckPerms();
		spark = initSpark();

		MinecraftServer.setBrandName(DEFAULT_BRAND_NAME);
		MinecraftServer.getConnectionManager().setPlayerProvider(
			(playerConnection, gameProfile) -> {
				Player player = new LuckPermsPlayer(luckPerms, playerConnection, gameProfile);
				if (!fireConnectEvent(player)) return null;
				return player;
			});

		initCommands();
		initStopCommand();
		try {
			initSkript(MinecraftServer.getGlobalEventHandler());
			MinestomUpdateService.cleanupAfterRestart();
			MinestomUpdateService.runStartupCheck();
			MinestomUpdateService.schedulePeriodicChecks();
			initEffectCommands(MinecraftServer.getGlobalEventHandler());
			scheduleShutdownTasks();
			server.start(properties.getProperty(ADDRESS_KEY), Integer.parseInt(properties.getProperty(PropertyUtils.PORT_KEY)));
			MinestomTerminal.start();


			metrics = new Metrics(-1) // TODO change service id provided
				.addCustomChart(new SingleLineChart("players", () -> MinecraftServer.getConnectionManager().getOnlinePlayerCount()))
				.addCustomChart(new SimplePie("auth_type", () -> switch (properties.getProperty(AUTH_TYPE_KEY).toLowerCase(Locale.ENGLISH)) {
					case "mojang" -> "mojang";
					case "velocity" -> "velocity";
					case "bungee", "bungeecord" -> "bungee";
					default -> "offline";
				}))
				.addCustomChart(new SimplePie("dispatcher_threads", () -> {
					String dispatcherThreads = properties.getProperty(DISPATCHER_THREADS);
					if (NumberUtils.isInteger(dispatcherThreads)) return dispatcherThreads;
					return "unknown";
				}))
				.addCustomChart(new SimplePie("skript_version", () -> Version.VERSION))
				.addCustomChart(new SimplePie("minestom_build", Git::version))
				.addCustomChart(new SimplePie("minecraft_version", () -> MinecraftServer.VERSION_NAME))
				.addCustomChart(new SimplePie("addon_count", () -> String.valueOf(Skript.getAddons().size())))
				.addCustomChart(new SimplePie("effect_commands", () -> SkriptConfig.enableEffectCommands.value().toString()))
				.addCustomChart(new SimplePie("log_effect_commands", () -> SkriptConfig.logEffectCommands.value().toString()))
				.addCustomChart(new SimplePie("number_accuracy", () -> SkriptConfig.numberAccuracy.value().toString()));
		} catch (Throwable t) {
			SkriptLogger.LOGGER.error("An error occurred while initializing Skript-Minestom:");
			t.printStackTrace();
			System.exit(1);
		}
	}

	private static void initMinestomProperties() {
		System.setProperty("minestom.send-light-after-block-placement-delay", "-1");
		System.setProperty("minestom.chunk-view-distance", properties.getProperty(CHUNK_VIEW_DISTANCE));
		System.setProperty("minestom.entity-view-distance", properties.getProperty(ENTITY_VIEW_DISTANCE));
		System.setProperty("minestom.dispatcher-threads", properties.getProperty(DISPATCHER_THREADS));
		System.setProperty("minestom.registry.unsafe-ops", "true");
		System.setProperty("minestom.new-socket-write-lock", "true");
	}

	private static LuckPerms initLuckPerms() {
		Path directory = new File(FileUtils.getServerDirectory(), "luckperms").toPath();
		return LuckPermsMinestom.builder(directory)
			.commandRegistry(CommandRegistry.minestom())
			.contextProvider(new DummyContextProvider())
			.configurationAdapter(plugin -> new MultiConfigurationAdapter(plugin,
				new EnvironmentVariableConfigAdapter(plugin),
				new HoconConfigurationAdapter(plugin)
			))
			.enable();
	}

	private static SparkMinestom initSpark() {
		Path directory = new File(FileUtils.getServerDirectory(), "spark").toPath();
		return SparkMinestom.builder(directory)
			.commands(true) // enables registration of Spark commands
			.permissionHandler((sender, a) -> LuckPermsLookup.hasPermission(sender, "spark"))
			.enable();
	}

	/**
	 * Boots Skript, registering its event listeners on the provided {@link EventNode}.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void initSkript(EventNode<net.minestom.server.event.Event> node) throws URISyntaxException {
		Bukkit.setPrimaryThreadCheck(() -> {
			Thread current = Thread.currentThread();
			return current instanceof TickThread || current == schedulerThread;
		});
		Bukkit.setTicker(tick -> MinecraftServer.getSchedulerManager().scheduleTask(() -> {
			schedulerThread = Thread.currentThread();
			tick.run();
		}, TaskSchedule.tick(1), TaskSchedule.tick(1))); // tick on minestom's thread
		Bukkit.getScheduler(); // initialize scheduler

		PluginManager pluginManager = Bukkit.getPluginManager();
		File skriptFile = new File(Skript.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File minestomFile = new File(SkriptMinestom.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Skript skript = (Skript) pluginManager.loadPlugin(skriptFile);
		Skript.onRegistration(() -> {
			if (!minestomFile.equals(skriptFile)) {
				org.skriptlang.skript.util.ClassLoader.loadClasses(SkriptMinestom.class, minestomFile, "ch.njol.skript",
					"conditions", "effects", "events", "expressions", "literals", "sections", "structures");
			}
			MinestomClasses.register();
			MinestomFunctions.register();
			MarkerRegistration.register();
		});
		skript.setEnabled(true);
		MinestomUpdateService.registerFinishedLoadingHook();
		skript.onEnable(); // have to manually initialize, addons are initialized on their own
		//Skript.getAddonInstance(true).UNSAFE_setLanguageFileDirectory("minestomlang"); // todo figure out how to do this if lang file is used
		Skript.closeUnsafeSkript();

		// init events
		EventNode<net.minestom.server.event.Event> skriptEventNode = EventNode.all("skript-user-events").setPriority(50);
		node.addChild(skriptEventNode);
		MinestomUpdateService.registerJoinListener(node);
		Set<Class<? extends Event>> listeningFor = new HashSet<>();
		for (SkriptEventInfo<?> eventInfo : Skript.getEvents()) {
			for (Class<? extends Event> bukkitEventClazz : eventInfo.events) {
				if (!EventWrapper.class.isAssignableFrom(bukkitEventClazz)) continue;
				if (!listeningFor.add(bukkitEventClazz)) continue;
				Constructor<? extends Event> constructor = null;
				Class<? extends Event> eventType = null;
				for (Constructor<?> c :  bukkitEventClazz.getDeclaredConstructors()) {
					Class<?>[] parameters =  c.getParameterTypes();
					if (parameters.length != 1) continue;
					constructor = (Constructor<? extends Event>) c;
					eventType = (Class<? extends Event>) parameters[0];
				}
				if (constructor == null) continue;
				ConstructorAccessor<? extends Event> mirrorConstructor = Mirror.of(bukkitEventClazz).constructor(eventType);
				skriptEventNode.addListener(eventType.asSubclass(net.minestom.server.event.Event.class), event -> {
					EventWrapper eventWrapper = (EventWrapper) mirrorConstructor.newInstance(event);
					pluginManager.callEvent(eventWrapper);
				});
			}
		}
	}

	public static void initEffectCommands(EventNode<net.minestom.server.event.Event> node) {
		node.addListener(PlayerChatEvent.class, event -> {
			if (!SkriptConfig.enableEffectCommands.value()) return;
			Player player = event.getPlayer();
			if (!LuckPermsLookup.hasPermission(player, "skript.effectcommands")) return;
			String message = event.getRawMessage();
			if (!message.startsWith(SkriptConfig.effectCommandToken.value())) return;
			event.setCancelled(true);
			parseAndExecuteEffectCommand(player, message);
		});

		MinecraftServer.getCommandManager().setUnknownCommandCallback((sender, command) -> {
			if (sender instanceof ConsoleSender && SkriptConfig.enableEffectCommands.value()
				&& command.startsWith(SkriptConfig.effectCommandToken.value())) {
				parseAndExecuteEffectCommand(sender, command);
				return;
			}
			Bukkit.getPluginManager().callEvent(new UnknownCommandEvent(sender, command));
		});
	}

	private static void parseAndExecuteEffectCommand(CommandSender sender, String input) {
		input = input.substring(1);

		EffectCommandEvent effectCommandEvent = new EffectCommandEvent(sender, input);
		Bukkit.getPluginManager().callEvent(effectCommandEvent);
		if (effectCommandEvent.isCancelled()) return;
		try (RedirectingLogHandler log = new RedirectingLogHandler(sender, null)) {
			SkriptLogger.startLogHandler(log);
			ParserInstance parserInstance = ParserInstance.get();
			parserInstance.setCurrentEvent("effect command", EffectCommandEvent.class);
			Effect effect = Effect.parse(input, null);
			TagResolver effectCommand = Placeholder.unparsed("effect_command", input);
			if (effect != null) {
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <base_grey>Executing effect command: <yellow><effect_command>", effectCommand));
				if (SkriptConfig.logEffectCommands.value()) {
					String senderIdentifier = sender instanceof Player player ? player.getUsername() : "Console";
					MinecraftServer.getCommandManager().getConsoleSender().sendMessage(senderIdentifier + " issued effect command: " + input);
				}
				TriggerItem.walk(effect, effectCommandEvent);
				Variables.removeLocals(effectCommandEvent);
			} else sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Couldn't find an effect under '<yellow><effect_command><error_color>'.", effectCommand));
		}
	}

	private static void scheduleShutdownTasks() {
		MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
			String kickMsg = properties.getProperty(SERVER_STOPPING_MINIMESSAGE);
			Component kickComponent = BASIC_MINI_MESSAGE.deserialize(kickMsg);
			for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
				p.kick(kickComponent);
			}
			if (spark != null) spark.shutdown();
			shutdown();
			LuckPermsMinestom.disable();
			MinestomTerminal.stop();
			System.exit(0); // sometimes server hangs so manually stop
		});
	}

	/**
	 * Registers {@code /skript} and routes unknown commands to {@link UnknownCommandEvent}.
	 */
	public static void initCommands() {
		CommandManager commandManager = MinecraftServer.getCommandManager();
		commandManager.register(new SkriptCommand());
		// unknown command callback moved to initEffectCommands
	}

	public static void initStopCommand() {
		MinecraftServer.getCommandManager().register(new StopCommand());
	}

	/**
	 * Fires {@link CustomConnectEvent} for a connecting player, returning {@code false} if
	 * the event kicked them (in which case the caller should reject the connection).
	 */
	public static boolean fireConnectEvent(Player player) {
		CustomConnectEvent connectEvent = new CustomConnectEvent(player);
		EventDispatcher.call(connectEvent);
		return !connectEvent.isKicked();
	}

	/**
	 * Runs the shutdown sequence for strictly the Skript plugin, installed skript-minestom addons, and bStats only.
	 */
	public static void shutdown() {
		PluginManager pluginManager = Bukkit.getPluginManager();

		Collection<SkriptAddon> addons = Skript.getAddons();
		Plugin skript = pluginManager.getPlugin("Skript");
		if (skript != null) pluginManager.disablePlugin(skript);
		for (SkriptAddon addon : addons) {
			pluginManager.disablePlugin(addon.plugin);
		}

		if (metrics != null) metrics.shutdown();
	}

	public static @Nullable LuckPerms getLuckPerms() {
		if (luckPerms != null) return luckPerms;
		try {
			return LuckPermsProvider.get();
		} catch (IllegalStateException e) {
			return null;
		}
	}

	public static @Nullable SparkMinestom getSpark() {
		return spark;
	}

	public static boolean isIsServerJar() {
		return isServerJar;
	}

}
