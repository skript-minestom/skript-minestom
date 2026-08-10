package ch.njol.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.data.*;
import ch.njol.skript.expressions.ExprSets;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.PluralizingArgsMessage;
import ch.njol.skript.log.*;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.ReleaseStatus;
import ch.njol.skript.util.*;
import ch.njol.skript.util.Date;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Closeable;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.iterator.CheckedIterator;
import ch.njol.util.coll.iterator.EnumerationIterable;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.LoggerUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.experiment.ExperimentRegistry;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.lang.structure.StructureInfo;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// TODO meaningful error if someone uses an %expression with percent signs% outside of text or a variable

/**
 * <b>Skript</b> - A Bukkit plugin to modify how Minecraft behaves without having to write a single line of code (You'll likely be writing some code though if you're reading this
 * =P)
 * <p>
 * Use this class to extend this plugin's functionality by adding more {@link Condition conditions}, {@link Effect effects}, {@link SimpleExpression expressions}, etc.
 * <p>
 * If your plugin.yml contains <tt>'depend: [Skript]'</tt> then your plugin will not start at all if Skript is not present. Add <tt>'softdepend: [Skript]'</tt> to your plugin.yml
 * if you want your plugin to work even if Skript isn't present, but want to make sure that Skript gets loaded before your plugin.
 * <p>
 * If you use 'softdepend' you can test whether Skript is loaded with <tt>'Bukkit.getPluginManager().getPlugin(&quot;Skript&quot;) != null'</tt>
 * <p>
 * Once you made sure that Skript is loaded you can use <code>Skript.getInstance()</code> whenever you need a reference to the plugin, but you likely won't need it since all API
 * methods are static.
 *
 * @author Peter Güttinger
 * @see #registerAddon(JavaPlugin)
 * @see #registerCondition(Class, String...)
 * @see #registerEffect(Class, String...)
 * @see #registerExpression(Class, Class, ExpressionType, String...)
 * @see #registerEvent(String, Class, Class, String...)
 * @see EventValues#registerEventValue(Class, Class, Converter, int)
 * @see Classes#registerClass(ClassInfo)
 * @see Comparators#registerComparator(Class, Class, Comparator)
 * @see Converters#registerConverter(Class, Class, Converter)
 */
public final class Skript extends JavaPlugin implements Listener {

	// ================ PLUGIN ================

	@Nullable
	private static Skript instance = null;

	static org.skriptlang.skript.@UnknownNullability Skript skript = null;
	private static org.skriptlang.skript.@UnknownNullability Skript unmodifiableSkript = null;

	private static boolean disabled = false;
	private static boolean partDisabled = false;
	private static boolean starting = true;
	private static boolean unsafeClosed = false;

	public static Skript getInstance() {
		if (instance == null)
			throw new IllegalStateException();
		return instance;
	}

	@ApiStatus.Internal
	public static org.skriptlang.skript.Skript UNSAFE_instance() {
		if (skript == null) {
			throw new SkriptAPIException("Skript is still initializing");
		}
		if (unsafeClosed) {
			throw new SkriptAPIException("You cannot access unsafe Skript");
		}
		return skript;
	}

	@ApiStatus.Internal
	public static void closeUnsafeSkript() {
		unsafeClosed = true;
	}

	@ApiStatus.Experimental
	public static org.skriptlang.skript.Skript instance() {
		if (unmodifiableSkript == null) {
			throw new SkriptAPIException("Skript is still initializing");
		}
		return unmodifiableSkript;
	}

	/**
	 * Current updater instance used by Skript.
	 */
	@Nullable
	private SkriptUpdater updater;

	public Skript() throws IllegalStateException {
		if (instance != null)
			throw new IllegalStateException("Cannot create multiple instances of Skript!");
		instance = this;
	}

	private static Runnable registration;

	private static Version minecraftVersion = new Version(666), UNKNOWN_VERSION = new Version(666);
	private static ServerPlatform serverPlatform = getServerPlatform(); // Start with unknown... onLoad changes this

	@Nullable
	private static Version version = null;
	@Deprecated(forRemoval = true) // TODO this field will be replaced by a proper registry later
	private static @UnknownNullability ExperimentRegistry experimentRegistry;

	/**
	 * Check minecraft version and assign it to minecraftVersion field
	 * This method is created to update MC version before onEnable method
	 */
	public static void updateMinecraftVersion() {
		minecraftVersion = new Version(Bukkit.getVersion());
	}

	public static Version getVersion() {
		final Version v = version;
		if (v == null)
			throw new IllegalStateException();
		return v;
	}

	public static final Message
		m_invalid_reload = new Message("skript.invalid reload"),
		m_finished_loading = new Message("skript.finished loading"),
		m_no_errors = new Message("skript.no errors"),
		m_no_scripts = new Message("skript.no scripts");
	private static final PluralizingArgsMessage m_scripts_loaded = new PluralizingArgsMessage("skript.scripts loaded");

	public static ServerPlatform getServerPlatform() {
		if (classExists("net.minestom.server.MinecraftServer")) {
			return ServerPlatform.MINESTOM;
		} else if (classExists("net.glowstone.GlowServer")) {
			return ServerPlatform.BUKKIT_GLOWSTONE; // Glowstone has timings too, so must check for it first
		} else if (classExists("co.aikar.timings.Timings")) {
			return ServerPlatform.BUKKIT_PAPER; // Could be Sponge, but it doesn't work at all at the moment
		} else if (classExists("org.spigotmc.SpigotConfig")) {
			return ServerPlatform.BUKKIT_SPIGOT;
		} else if (classExists("org.bukkit.craftbukkit.CraftServer") || classExists("org.bukkit.craftbukkit.Main")) {
			// At some point, CraftServer got removed or moved
			return ServerPlatform.BUKKIT_CRAFTBUKKIT;
		} else { // Probably some ancient Bukkit implementation
			return ServerPlatform.BUKKIT_UNKNOWN;
		}
	}

	/**
	 * Returns true if the underlying installed Java/JVM is 32-bit, false otherwise.
	 * Note that this depends on a internal system property and these can always be overridden by user using -D JVM options,
	 * more specifically, this method will return false on non OracleJDK/OpenJDK based JVMs, that don't include bit information in java.vm.name system property.
	 * @return Whether the installed Java/JVM is 32-bit or not.
	 */
	private static boolean using32BitJava() {
		// Property returned should either be "Java HotSpot(TM) 32-Bit Server VM" or "OpenJDK 32-Bit Server VM" if 32-bit and using OracleJDK/OpenJDK
		return System.getProperty("java.vm.name").contains("32");
	}

	/**
	 * The folder containing all Scripts.
	 * Never reference this field directly. Use {@link #getScriptsFolder()}.
	 */
	private File scriptsFolder;

	/**
	 * The folder containing all addons.
	 */
	private File addonsFolder;

	/**
	 * @return The manager for experimental, optional features.
	 */
	public static ExperimentRegistry experiments() {
		return experimentRegistry;
	}

	/**
	 * @return The folder containing all Scripts.
	 */
	public File getScriptsFolder() {
		if (!scriptsFolder.isDirectory())
			//noinspection ResultOfMethodCallIgnored
			scriptsFolder.mkdirs();
		return scriptsFolder;
	}

	/**
	 * @return The folder containing all Scripts.
	 */
	public @NotNull File getAddonsFolder() {
		if (!addonsFolder.isDirectory())
			//noinspection ResultOfMethodCallIgnored
			addonsFolder.mkdirs();
		return addonsFolder;
	}

	private Collection<String> getResourceEntries() throws IOException {
		URL url = getClass().getResource("/config.sk");
		if (url == null)
			return Collections.emptyList();
		Path root;
		try {
			root = Paths.get(url.toURI()).getParent();
		} catch (URISyntaxException | IllegalArgumentException e) {
			return Collections.emptyList();
		}
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(Files::isRegularFile)
				.map(path -> root.relativize(path).toString().replace(File.separatorChar, '/'))
				.collect(Collectors.toList());
		}
	}

	@Override
	public void onEnable() {
		updateMinecraftVersion();
		Bukkit.getPluginManager().registerEvents(this, this);
		if (disabled) {
			Skript.error(m_invalid_reload.toString());
			setEnabled(false);
			return;
		}

		handleJvmArguments(); // JVM arguments

		version = new Version("" + getDescription().getVersion()); // Skript version

		// Start the updater
		// Note: if config prohibits update checks, it will NOT do network connections
		try {
			this.updater = new SkriptUpdater();
		} catch (Exception e) {
			Skript.exception(e, "Update checker could not be initialized.");
		}

		if (!getDataFolder().isDirectory())
			getDataFolder().mkdirs();

		scriptsFolder = new File(getDataFolder(), SCRIPTSFOLDER);
		addonsFolder = new File(getDataFolder(), ADDONSFOLDER);
		File config = new File(getDataFolder(), "config.sk");
		File features = new File(getDataFolder(), "features.sk");
		File lang = new File(getDataFolder(), "lang");
		if (!scriptsFolder.isDirectory() || !config.exists() || !features.exists() || !lang.exists()) {
			ZipFile f = null;
			try {
				boolean populateExamples = false;
				if (!scriptsFolder.isDirectory()) {
					if (!scriptsFolder.mkdirs())
						throw new IOException("Could not create the directory " + scriptsFolder);
					populateExamples = true;
				}

				boolean populateLanguageFiles = false;
				if (!lang.isDirectory()) {
					if (!lang.mkdirs())
						throw new IOException("Could not create the directory " + lang);
					populateLanguageFiles = true;
				}

				File source = getFile();
				Collection<String> entries;
				if (source != null && source.isFile()) {
					f = new ZipFile(source);
					entries = new ArrayList<>();
					for (ZipEntry e : new EnumerationIterable<ZipEntry>(f.entries())) {
						if (!e.isDirectory())
							entries.add(e.getName());
					}
				} else {
					entries = getResourceEntries();
				}
				for (String entry : entries) {
					File saveTo = null;
					if (populateExamples && entry.startsWith(SCRIPTSFOLDER + "/")) {
						String fileName = entry.substring(entry.indexOf("/") + 1);
						// All example scripts must be disabled for jar security.
						if (!fileName.startsWith(ScriptLoader.DISABLED_SCRIPT_PREFIX))
							fileName = ScriptLoader.DISABLED_SCRIPT_PREFIX + fileName;
						saveTo = new File(scriptsFolder, fileName);
					} else if (populateLanguageFiles
							&& entry.startsWith("lang/")
							&& !entry.endsWith("default.lang")) {
						String fileName = entry.substring(entry.lastIndexOf("/") + 1);
						saveTo = new File(lang, fileName);
					} else if (entry.equals("config.sk")) {
						if (!config.exists())
							saveTo = config;
//					} else if (entry.startsWith("aliases-") && entry.endsWith(".sk") && !entry.contains("/")) {
//						File af = new File(getDataFolder(), entry);
//						if (!af.exists())
//							saveTo = af;
					} else if (entry.startsWith("features.sk")) {
						if (!features.exists())
							saveTo = features;
					}
					if (saveTo != null) {
						InputStream in = f != null ? f.getInputStream(f.getEntry(entry)) : getResource(entry);
						try {
							assert in != null;
							FileUtils.save(in, saveTo);
						} finally {
							in.close();
						}
					}
				}
				info("Successfully generated the config and the example scripts.");
			} catch (ZipException ignored) {} catch (IOException e) {
				error("Error generating the default files: " + ExceptionUtils.toString(e));
			} finally {
				if (f != null) {
					try {
						f.close();
					} catch (IOException ignored) {}
				}
			}
		}

		// initialize the modern Skript instance
		skript = org.skriptlang.skript.Skript.of(getClass(), getName());
		unmodifiableSkript = new ModernSkriptBridge.SpecialUnmodifiableSkript(skript);
		skript.localizer().setSourceDirectories("lang",
			getDataFolder().getAbsolutePath() + "lang");
		// initialize the old Skript SkriptAddon instance
		getAddonInstance();

		experimentRegistry = new ExperimentRegistry(this);
		Feature.registerAll(getAddonInstance(), experimentRegistry);

		// Load classes which are always safe to use
		new JavaClasses(); // These may be needed in configuration

		EventValueRegistry eventValueRegistry = EventValueRegistry.empty(this);
		skript.storeRegistry(EventValueRegistry.class, eventValueRegistry);
		//noinspection removal
		EventValues.setEventValueRegistry(eventValueRegistry);

		// And then not-so-safe classes
		Throwable classLoadError = null;
		try {
			new SkriptClasses();
		} catch (Throwable e) {
			classLoadError = e;
		}

		// Config must be loaded after Java and Skript classes are parseable
		// ... but also before platform check, because there is a config option to ignore some errors
		SkriptConfig.load();


		// If loading can continue (platform ok), check for potentially thrown error
		if (classLoadError != null) {
			exception(classLoadError);
			setEnabled(false);
			return;
		}

		new DefaultComparators();
		new DefaultConverters();
		new DefaultFunctions();
		new DefaultOperations();

		try {
			//Class.forName("ch.njol.skript.expressions.ExprEntities");
			new ExprSets(); // higher priority than other similarly syntaxed expressions
			getAddonInstance().loadClasses("ch.njol.skript", "elements", "conditions",
				"effects", "events", "expressions", "entity", "literals", "sections", "structures");

			registration.run();
		} catch (final Exception e) {
			exception(e, "Could not load required .class files: " + e.getLocalizedMessage());
			setEnabled(false);
			return;
		}

		if (logNormal())
			info("Skript " + Language.get("skript.copyright"));

		PluginManager pluginManager = Bukkit.getPluginManager();

		for (File addonFile : getAddonsFolder().listFiles()) {
			if (!addonFile.isFile() || !addonFile.getName().endsWith(".jar") || addonFile.equals(getFile()))
				continue;

			JavaPlugin plugin = pluginManager.loadPlugin(addonFile);
			plugin.setEnabled(true);
			plugin.onEnable();
		}

		stopAcceptingRegistrations();
		//Documentation.generate(); // TODO move to test classes?

		// Variable loading
		if (logNormal())
			info("Loading variables...");
		long vls = System.currentTimeMillis();

		LogHandler h = SkriptLogger.startLogHandler(new ErrorDescLogHandler() {
			@Override
			public LogResult log(final LogEntry entry) {
				super.log(entry);
				if (entry.level.intValue() >= Level.SEVERE.intValue()) {
					logEx(entry.message); // no [Skript] prefix
					return LogResult.DO_NOT_LOG;
				} else {
					return LogResult.LOG;
				}
			}

			@Override
			protected void beforeErrors() {
				logEx();
				logEx("===!!!=== Skript variable load error ===!!!===");
				logEx("Unable to load (all) variables:");
			}

			@Override
			protected void afterErrors() {
				logEx();
				logEx("Skript will work properly, but old variables might not be available at all and new ones may or may not be saved until Skript is able to create a backup of the old file and/or is able to connect to the database (which requires a restart of Skript)!");
				logEx();
			}
		});

		try (CountingLogHandler c = new CountingLogHandler(SkriptLogger.SEVERE).start()) {
			if (!Variables.load())
				if (c.getCount() == 0)
					error("(no information available)");
		} finally {
			h.stop();
		}

		long vld = System.currentTimeMillis() - vls;
		if (logNormal())
			info("Loaded " + Variables.numVariables() + " variables in " + ((vld / 100) / 10.) + " seconds");

		// Skript initialization done
		debug("Early init done");

		/*
		 * Start loading scripts
		 */
		Date start = new Date();
		CountingLogHandler logHandler = new CountingLogHandler(Level.SEVERE);

		File scriptsFolder = getScriptsFolder();
		ScriptLoader.updateDisabledScripts(scriptsFolder.toPath());
		ScriptLoader.loadScripts(scriptsFolder, logHandler)
			.thenAccept(scriptInfo -> {
				try {
					if (logHandler.getCount() == 0)
						Skript.info(m_no_errors.toString());
					if (scriptInfo.files == 0)
						Skript.warning(m_no_scripts.toString());
					if (Skript.logNormal() && scriptInfo.files > 0)
						Skript.info(m_scripts_loaded.toString(
							scriptInfo.files,
							scriptInfo.structures,
							start.difference(new Date())
						));

					Skript.info(m_finished_loading.toString());
				} catch (Exception e) {
					// Something went wrong, we need to make sure the exception is printed
					throw Skript.exception(e);
				}
			});
		starting = false;
	}

	public static boolean isStarting() {
		return starting;
	}

	public static void onRegistration(Runnable runnable) {
		registration = runnable;
	}

	/**
	 * Handles -Dskript.stuff command line arguments.
	 */
	private void handleJvmArguments() {
		Path folder = getDataFolder().toPath();

		/*
		 * Burger is a Python application that extracts data from Minecraft.
		 * Datasets for most common versions are available for download.
		 * Skript uses them to provide minecraft:material to Bukkit
		 * Material mappings on Minecraft 1.12 and older.
		 */
		String burgerEnabled = System.getProperty("skript.burger.enable");
		if (burgerEnabled != null) {
			tainted = true;
			String version = System.getProperty("skript.burger.version");
			String burgerInput;
			if (version == null) { // User should have provided JSON file path
				String inputFile = System.getProperty("skript.burger.file");
				if (inputFile == null) {
					Skript.exception("burger enabled but skript.burger.file not provided");
					return;
				}
				try {
					burgerInput = new String(Files.readAllBytes(Paths.get(inputFile)), StandardCharsets.UTF_8);
				} catch (IOException e) {
					Skript.exception(e);
					return;
				}
			} else { // Try to download Burger dataset for this version
				try {
					Path data = folder.resolve("burger-" + version + ".json");
					if (!Files.exists(data)) {
						URL url = new URL("https://pokechu22.github.io/Burger/" + version + ".json");
						try (InputStream is = url.openStream()) {
							Files.copy(is, data);
						}
					}
					burgerInput = new String(Files.readAllBytes(data), StandardCharsets.UTF_8);
				} catch (IOException e) {
					Skript.exception(e);
					return;
				}
			}

		}
	}

	public static Version getMinecraftVersion() {
		return minecraftVersion;
	}

	/**
	 * @return Whether this server is running CraftBukkit
	 */
	public static boolean isRunningCraftBukkit() {
		return serverPlatform == ServerPlatform.BUKKIT_CRAFTBUKKIT;
	}

	/**
	 * Used to test whether certain Bukkit features are supported.
	 *
	 * @param className
	 * @return Whether the given class exists.
	 * @deprecated use {@link #classExists(String)}
	 */
	@Deprecated
	public static boolean supports(final String className) {
		return classExists(className);
	}

	/**
	 * Tests whether a given class exists in the classpath.
	 *
	 * @param className The {@link Class#getCanonicalName() canonical name} of the class
	 * @return Whether the given class exists.
	 */
	public static boolean classExists(final String className) {
		try {
			Class.forName(className);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Tests whether a method exists in the given class.
	 *
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @return Whether the given method exists.
	 */
	public static boolean methodExists(final Class<?> c, final String methodName, final Class<?>... parameterTypes) {
		try {
			c.getDeclaredMethod(methodName, parameterTypes);
			return true;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	/**
	 * Tests whether a method exists in the given class, and whether the return type matches the expected one.
	 * <p>
	 * Note that this method doesn't work properly if multiple methods with the same name and parameters exist but have different return types.
	 *
	 * @param c The class
	 * @param methodName The name of the method
	 * @param parameterTypes The parameter types of the method
	 * @param returnType The expected return type
	 * @return Whether the given method exists.
	 */
	public static boolean methodExists(final Class<?> c, final String methodName, final Class<?>[] parameterTypes, final Class<?> returnType) {
		try {
			final Method m = c.getDeclaredMethod(methodName, parameterTypes);
			return m.getReturnType() == returnType;
		} catch (final NoSuchMethodException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	/**
	 * Tests whether a field exists in the given class.
	 *
	 * @param c The class
	 * @param fieldName The name of the field
	 * @return Whether the given field exists.
	 */
	public static boolean fieldExists(final Class<?> c, final String fieldName) {
		try {
			c.getDeclaredField(fieldName);
			return true;
		} catch (final NoSuchFieldException e) {
			return false;
		} catch (final SecurityException e) {
			return false;
		}
	}

	@SuppressWarnings("null")
	private final static Collection<Closeable> closeOnDisable = Collections.synchronizedCollection(new ArrayList<Closeable>());

	/**
	 * Registers a Closeable that should be closed when this plugin is disabled.
	 * <p>
	 * All registered Closeables will be closed after all scripts have been stopped.
	 *
	 * @param closeable
	 */
	public static void closeOnDisable(final Closeable closeable) {
		closeOnDisable.add(closeable);
	}

	@Nullable
	private static Method IS_RUNNING;
	@Nullable
	private static Object MC_SERVER;

	@SuppressWarnings("ConstantConditions")
	private boolean isServerRunning() {
		return false;
	}

	private void beforeDisable() {
		partDisabled = true;

		ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
	}

	@Override
	public void onDisable() {
		if (disabled)
			return;
		disabled = true;

		if (!partDisabled) {
			beforeDisable();
		}
		this.experimentRegistry = null;

		for (Closeable c : closeOnDisable) {
			try {
				c.close();
			} catch (final Exception e) {
				Skript.exception(e, "An error occurred while shutting down.", "This might or might not cause any issues.");
			}
		}
	}

	// ================ CONSTANTS, OPTIONS & OTHER ================

	public final static String SCRIPTSFOLDER = "scripts";
	public final static String ADDONSFOLDER = "addons";

	/**
	 * A small value, useful for comparing doubles or floats.
	 * <p>
	 * E.g. to test whether two floating-point numbers are equal:
	 *
	 * <pre>
	 * Math.abs(a - b) &lt; Skript.EPSILON
	 * </pre>
	 *
	 * or whether a location is within a specific radius of another location:
	 *
	 * <pre>
	 * location.distanceSquared(center) - radius * radius &lt; Skript.EPSILON
	 * </pre>
	 *
	 * @see #EPSILON_MULT
	 */
	public final static double EPSILON = 1e-10;
	/**
	 * A value a bit larger than 1
	 *
	 * @see #EPSILON
	 */
	public final static double EPSILON_MULT = 1.00001;

	/**
	 * The maximum ID a block can have in Minecraft.
	 */
	public final static int MAXBLOCKID = 255;
	/**
	 * The maximum data value of Minecraft, i.e. Short.MAX_VALUE - Short.MIN_VALUE.
	 */
	public final static int MAXDATAVALUE = Short.MAX_VALUE - Short.MIN_VALUE;

	// TODO localise Infinity, -Infinity, NaN (and decimal point?)
	public static String toString(final double n) {
		return StringUtils.toString(n, SkriptConfig.numberAccuracy.value());
	}

	public final static UncaughtExceptionHandler UEH = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(final @Nullable Thread t, final @Nullable Throwable e) {
			Skript.exception(e, "Exception in thread " + (t == null ? null : t.getName()));
		}
	};

	/**
	 * Creates a new Thread and sets its UncaughtExceptionHandler. The Thread is not started automatically.
	 */
	public static Thread newThread(final Runnable r, final String name) {
		final Thread t = new Thread(r, name);
		t.setUncaughtExceptionHandler(UEH);
		return t;
	}

	// ================ REGISTRATIONS ================

	private static boolean acceptRegistrations = true;

	public static boolean isAcceptRegistrations() {
		if (instance == null)
			throw new IllegalStateException("Skript was never loaded");
		return acceptRegistrations && instance.isEnabled();
	}

	public static void checkAcceptRegistrations() {
		if (!isAcceptRegistrations() && !Skript.testing())
			throw new SkriptAPIException("Registration can only be done during plugin initialization");
	}

	private static void stopAcceptingRegistrations() {
		Converters.createChainedConverters();

		acceptRegistrations = false;

		Classes.onRegistrationsStop();
	}

	// ================ ADDONS ================

	@Deprecated
	private static final Set<SkriptAddon> addons = new HashSet<>();

	/**
	 * Registers an addon to Skript. This is currently not required for addons to work, but the returned {@link SkriptAddon} provides useful methods for registering syntax elements
	 * and adding new strings to Skript's localization system (e.g. the required "types.[type]" strings for registered classes).
	 *
	 * @param plugin The plugin
	 */
	public static SkriptAddon registerAddon(JavaPlugin plugin) {
		checkAcceptRegistrations();
		SkriptAddon addon = new SkriptAddon(plugin);
		addons.add(addon);
		return addon;
	}

	public static @Nullable SkriptAddon getAddon(JavaPlugin plugin) {
		if (plugin == Skript.getInstance()) {
			return Skript.getAddonInstance();
		}
		for (SkriptAddon addon : getAddons()) {
			if (addon.plugin == plugin) {
				return addon;
			}
		}
		return null;
	}

	public static @Nullable SkriptAddon getAddon(String name) {
		if (name.equals(Skript.getInstance().getName())) {
			return Skript.getAddonInstance();
		}
		for (SkriptAddon addon : getAddons()) {
			if (addon.getName().equals(name)) {
				return addon;
			}
		}
		return null;
	}

	public static @Unmodifiable Collection<SkriptAddon> getAddons() {
		Set<SkriptAddon> addons = new HashSet<>(Skript.addons);
		addons.addAll(instance().addons().stream()
			.filter(addon -> addons.stream().noneMatch(oldAddon -> oldAddon.name().equals(addon.name())))
			.map(SkriptAddon::fromModern)
			.collect(Collectors.toSet())
		);
		return Collections.unmodifiableCollection(addons);
	}

	@Deprecated
	private static @Nullable SkriptAddon addon;

	/**
	 * @return A {@link SkriptAddon} representing Skript.
	 */
	public static SkriptAddon getAddonInstance() {
		if (addon == null) {
			addon = SkriptAddon.fromModern(instance());
		}
		return addon;
	}

	// ================ CONDITIONS & EFFECTS & SECTIONS ================

	/**
	 * Attempts to create a SyntaxOrigin from a provided class.
	 * @deprecated This method exists solely for compatibility reasons.
	 */
	@ApiStatus.Internal
	@Deprecated(since = "INSERT VERSION", forRemoval = true)
	public static Origin getSyntaxOrigin(Class<?> source) {
		JavaPlugin plugin;
		try {
			plugin = JavaPlugin.getProvidingPlugin(source);
		} catch (IllegalArgumentException e) { // Occurs when the method fails to determine the providing plugin
			return Origin.UNKNOWN;
		}
		SkriptAddon addon = getAddon(plugin);
		if (addon != null) {
			return Origin.of(addon);
		}
		return Origin.UNKNOWN;
	}

	/**
	 * Registers a {@link Condition}.
	 *
	 * @param conditionClass The condition's class
	 * @param patterns Skript patterns to match this condition
	 */
	public static <E extends Condition> void registerCondition(Class<E> conditionClass, String... patterns) throws IllegalArgumentException {
		registerCondition(conditionClass, Condition.ConditionType.COMBINED, patterns);
	}

	/**
	 * Registers a {@link Condition}.
	 *
	 * @param conditionClass The condition's class
	 * @param type The type of condition which affects its priority in the parsing search
	 * @param patterns Skript patterns to match this condition
	 */
	public static <E extends Condition> void registerCondition(Class<E> conditionClass, Condition.ConditionType type, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.CONDITION, SyntaxInfo.builder(conditionClass)
			.priority(type.priority())
			.origin(getSyntaxOrigin(conditionClass))
			.addPatterns(patterns)
			.build()
		);
	}

	/**
	 * Registers an {@link Effect}.
	 *
	 * @param effectClass The effect's class
	 * @param patterns Skript patterns to match this effect
	 */
	public static <E extends Effect> void registerEffect(Class<E> effectClass, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(effectClass)
			.origin(getSyntaxOrigin(effectClass))
			.addPatterns(patterns)
			.build()
		);
	}

	/**
	 * Registers a {@link Section}.
	 *
	 * @param sectionClass The section's class
	 * @param patterns Skript patterns to match this section
	 * @see Section
	 */
	public static <E extends Section> void registerSection(Class<E> sectionClass, String... patterns) throws IllegalArgumentException {
		checkAcceptRegistrations();

		skript.syntaxRegistry().register(SyntaxRegistry.SECTION, SyntaxInfo.builder(sectionClass)
			.origin(getSyntaxOrigin(sectionClass))
			.addPatterns(patterns)
			.build()
		);
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Statement>> getStatements() {
		return instance().syntaxRegistry()
			.syntaxes(SyntaxRegistry.STATEMENT).stream()
			.map(SyntaxElementInfo::<SyntaxElementInfo<Statement>, Statement>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Condition>> getConditions() {
		return instance().syntaxRegistry()
			.syntaxes(SyntaxRegistry.CONDITION).stream()
			.map(SyntaxElementInfo::<SyntaxElementInfo<Condition>, Condition>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Effect>> getEffects() {
		return instance().syntaxRegistry()
			.syntaxes(SyntaxRegistry.EFFECT).stream()
			.map(SyntaxElementInfo::<SyntaxElementInfo<Effect>, Effect>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable Collection<SyntaxElementInfo<? extends Section>> getSections() {
		return instance().syntaxRegistry()
			.syntaxes(SyntaxRegistry.SECTION).stream()
			.map(SyntaxElementInfo::<SyntaxElementInfo<Section>, Section>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	// ================ EXPRESSIONS ================

	/**
	 * Registers an expression.
	 *
	 * @param expressionType The expression's class
	 * @param returnType The superclass of all values returned by the expression
	 * @param type The expression's {@link ExpressionType type}. This is used to determine in which order to try to parse expressions.
	 * @param patterns Skript patterns that match this expression
	 * @throws IllegalArgumentException if returnType is not a normal class
	 */
	public static <E extends Expression<T>, T> void registerExpression(
		Class<E> expressionType, Class<T> returnType, ExpressionType type, String... patterns
	) throws IllegalArgumentException {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.EXPRESSION, SyntaxInfo.Expression.builder(expressionType, returnType)
			.priority(type.priority())
			.origin(getSyntaxOrigin(expressionType))
			.addPatterns(patterns)
			.build()
		);
	}

	@SuppressWarnings("null")
	public static Iterator<ExpressionInfo<?, ?>> getExpressions() {
		List<ExpressionInfo<?, ?>> list = new ArrayList<>();
		for (SyntaxInfo.Expression<?, ?> info : instance().syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION))
			list.add((ExpressionInfo<?, ?>) SyntaxElementInfo.fromModern(info));
		return list.iterator();
	}

	public static Iterator<ExpressionInfo<?, ?>> getExpressions(final Class<?>... returnTypes) {
		return new CheckedIterator<>(getExpressions(), info -> {
			if (info == null || info.returnType == Object.class)
				return true;
			for (Class<?> returnType : returnTypes) {
				assert returnType != null;
				if (Converters.converterExists(info.returnType, returnType))
						return true;
				}
				return false;
		});
	}

	// ================ EVENTS ================

	/**
	 * Registers an event.
	 *
	 * @param name Capitalised name of the event without leading "On" which is added automatically (Start the name with an asterisk to prevent this). Used for error messages and
	 *            the documentation.
	 * @param c The event's class
	 * @param event The Bukkit event this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */
	@SuppressWarnings("unchecked")
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(String name, Class<E> c, Class<? extends Event> event, String... patterns) {
		return registerEvent(name, c, new Class[] {event}, patterns);
	}

	/**
	 * Registers an event.
	 *
	 * @param name The name of the event, used for error messages
	 * @param eventClass The event's class
	 * @param events The Bukkit events this event applies to
	 * @param patterns Skript patterns to match this event
	 * @return A SkriptEventInfo representing the registered event. Used to generate Skript's documentation.
	 */

	@SuppressWarnings("ConstantConditions") // caused by bad array annotations
	public static <E extends SkriptEvent> SkriptEventInfo<E> registerEvent(
		String name, Class<E> eventClass, Class<? extends Event>[] events, String... patterns
	) {
		checkAcceptRegistrations();
		for (int i = 0; i < patterns.length; i++)
			patterns[i] = BukkitSyntaxInfos.fixPattern(patterns[i]);
		var legacy = new SkriptEventInfo.ModernSkriptEventInfo<>(name, patterns, eventClass, "", events);
		skript.syntaxRegistry().register(BukkitSyntaxInfos.Event.KEY, legacy);
		return legacy;
	}

	public static <E extends Structure> void registerStructure(Class<E> structureClass, String... patterns) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
			.origin(getSyntaxOrigin(structureClass))
			.addPatterns(patterns)
			.build()
		);
	}

	public static <E extends Structure> void registerSimpleStructure(Class<E> structureClass, String... patterns) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
			.origin(getSyntaxOrigin(structureClass))
			.addPatterns(patterns)
			.nodeType(SyntaxInfo.Structure.NodeType.SIMPLE)
			.build()
		);
	}

	public static <E extends Structure> void registerStructure(
		Class<E> structureClass, EntryValidator entryValidator, String... patterns
	) {
		checkAcceptRegistrations();
		skript.syntaxRegistry().register(SyntaxRegistry.STRUCTURE, SyntaxInfo.Structure.builder(structureClass)
			.origin(getSyntaxOrigin(structureClass))
			.addPatterns(patterns)
			.entryValidator(entryValidator)
			.build()
		);
	}

	public static @Unmodifiable Collection<SkriptEventInfo<?>> getEvents() {
		return instance().syntaxRegistry()
			.syntaxes(BukkitSyntaxInfos.Event.KEY).stream()
			.map(SyntaxElementInfo::<SkriptEventInfo<SkriptEvent>, SkriptEvent>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	public static @Unmodifiable List<StructureInfo<? extends Structure>> getStructures() {
		return instance().syntaxRegistry()
			.syntaxes(SyntaxRegistry.STRUCTURE).stream()
			.map(SyntaxElementInfo::<StructureInfo<Structure>, Structure>fromModern)
			.collect(Collectors.toUnmodifiableList());
	}

	// ================ COMMANDS ================

	// ================ LOGGING ================

	public static boolean logNormal() {
		return SkriptLogger.log(Verbosity.NORMAL);
	}

	public static boolean logHigh() {
		return SkriptLogger.log(Verbosity.HIGH);
	}

	public static boolean logVeryHigh() {
		return SkriptLogger.log(Verbosity.VERY_HIGH);
	}

	public static boolean debug() {
		return SkriptLogger.debug();
	}

	public static boolean testing() {
		return debug() || Skript.class.desiredAssertionStatus();
	}

	public static boolean log(final Verbosity minVerb) {
		return SkriptLogger.log(minVerb);
	}

	public static void debug(final String info) {
		if (!debug())
			return;
		SkriptLogger.log(SkriptLogger.DEBUG, info);
	}

	/**
	 * Sends a debug message with formatted objects if {@link #debug()} returns true.
	 *
	 * @param message The message to send
	 * @param objects The objects to format the message with
	 * @see String#formatted(Object...)
	 */
	public static void debug(String message, Object... objects) {
		if (!debug())
			return;
		debug(message.formatted(objects));
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void info(final String info) {
		SkriptLogger.log(Level.INFO, info);
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void warning(final String warning) {
		SkriptLogger.log(Level.WARNING, warning);
	}

	/**
	 * @see SkriptLogger#log(Level, String)
	 */
	@SuppressWarnings("null")
	public static void error(final @Nullable String error) {
		if (error != null)
			SkriptLogger.log(Level.SEVERE, error);
	}

	/**
	 * Sends an error message with formatted objects.
	 *
	 * @param message The message to send
	 * @param objects The objects to format the message with
	 * @see String#formatted(Object...)
	 */
	public static void error(String message, Object... objects) {
		error(message.formatted(objects));
	}

	/**
	 * Use this in {@link Expression#init(Expression[], int, Kleenean, ch.njol.skript.lang.SkriptParser.ParseResult)} (and other methods that are called during the parsing) to log
	 * errors with a specific {@link ErrorQuality}.
	 *
	 * @param error
	 * @param quality
	 */
	public static void error(final String error, final ErrorQuality quality) {
		SkriptLogger.log(new LogEntry(SkriptLogger.SEVERE, quality, error));
	}

	private final static String EXCEPTION_PREFIX = "#!#! ";

	/**
	 * Used if something happens that shouldn't happen
	 *
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public static EmptyStacktraceException exception(final String... info) {
		return exception(null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final String... info) {
		return exception(cause, null, null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable Thread thread, final String... info) {
		return exception(cause, thread, null, info);
	}

	public static EmptyStacktraceException exception(final @Nullable Throwable cause, final @Nullable TriggerItem item, final String... info) {
		return exception(cause, null, item, info);
	}

	/**
	 * Maps Java packages of plugins to descriptions of said plugins.
	 * This is only done for plugins that depend or soft-depend on Skript.
	 */
	private static Map<String, PluginDescriptionFile> pluginPackages = new HashMap<>();
	private static boolean checkedPlugins = false;

	/**
	 * Set by Skript when doing something that users shouldn't do.
	 */
	private static boolean tainted = false;

	/**
	 * Set to true when an exception is thrown.
	 */
	private static boolean errored = false;

	/**
	 * Mark that an exception has occurred at some point during runtime.
	 * Only used for Skript's testing system.
	 */
	public static void markErrored() {
		errored = true;
	}

	/**
	 * Used if something happens that shouldn't happen
	 *
	 * @param cause exception that shouldn't occur
	 * @param info Description of the error and additional information
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public static EmptyStacktraceException exception(@Nullable Throwable cause, final @Nullable Thread thread, final @Nullable TriggerItem item, final String... info) {
		errored = true;

		// Don't send full exception message again, when caught exception (likely) comes from this method
		if (cause instanceof EmptyStacktraceException) {
			return new EmptyStacktraceException();
		}

		// First error: gather plugin package information
		if (!checkedPlugins) {
			for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
				if (plugin.getName().equals("Skript")) // Don't track myself!
					continue;

				PluginDescriptionFile desc = plugin.getDescription();
				if (desc.getDepend().contains("Skript") || desc.getSoftDepend().contains("Skript")) {
					// Take actual main class out from the qualified name
					String[] parts = desc.getMain().split("\\."); // . is special in regexes...
					StringBuilder name = new StringBuilder(desc.getMain().length());
					for (int i = 0; i < parts.length - 1; i++) {
						name.append(parts[i]).append('.');
					}

					// Put this to map
					pluginPackages.put(name.toString(), desc);
					if (Skript.debug())
						Skript.info("Identified potential addon: " + desc.getFullName() + " (" + name.toString() + ")");
				}
			}

			checkedPlugins = true; // No need to do this next time
		}

		String issuesUrl = "https://github.com/SkriptLang/Skript/issues";

		logEx();
		logEx("<skript_minestom_tag> Severe Error:");
		logEx(info);
		logEx();

		// Parse something useful out of the stack trace
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		Set<PluginDescriptionFile> stackPlugins = new HashSet<>();
		for (StackTraceElement s : stackTrace) { // Look through stack trace
			for (Entry<String,PluginDescriptionFile> e : pluginPackages.entrySet()) { // Look through plugins
				if (s.getClassName().contains(e.getKey())) // Hey, is this plugin in that stack trace?
					stackPlugins.add(e.getValue()); // Yes? Add it to list
			}
		}

		SkriptUpdater updater = Skript.getInstance().getUpdater();

		// Check if server platform is supported
		if (tainted) {
			logEx("Skript is running with developer command-line options.");
			logEx("If you are not a developer, consider disabling them.");
		} else if (getInstance().getDescription().getVersion().contains("nightly")) {
			logEx("You're running a (buggy) nightly version of Skript.");
			logEx("If this is not a test server, switch to a more stable release NOW!");
			logEx("Your players are unlikely to appreciate crashes and/or data loss due to Skript bugs.");
			logEx("");
			logEx("Just testing things? Good. Please report this bug, so that we can fix it before a stable release.");
			logEx("Issue tracker: " + issuesUrl);
		} else if (!serverPlatform.supported){
			logEx("Your server platform appears to be unsupported by Skript. It might not work reliably.");
			logEx("You can report this at " + issuesUrl + ". However, we may be unable to fix the issue.");
			logEx("It is recommended that you switch to Paper or Spigot, should you encounter more problems.");
		} else if (updater != null && updater.getReleaseStatus() == ReleaseStatus.OUTDATED) {
			logEx("You're running outdated version of Skript! Please try updating it NOW; it might fix this.");
			logEx("Run /sk update check to get a download link to latest Skript!");
			logEx("You will be given instructions how to report this error if it persists after update.");
		} else {
			logEx("Something went horribly wrong with Skript.");
			logEx("This issue is NOT your fault! You probably can't fix it yourself, either.");
			if (pluginPackages.isEmpty()) {
				logEx("You should report it at " + issuesUrl + ". Please copy paste this report there (or use paste service).");
				logEx("This ensures that your issue is noticed and will be fixed as soon as possible.");
			} else {
				logEx("It looks like you are using some plugin(s) that alter how Skript works (addons).");
				if (stackPlugins.isEmpty()) {
					logEx("Here is full list of them:");
					StringBuilder pluginsMessage = new StringBuilder();
					for (PluginDescriptionFile desc : pluginPackages.values()) {
						pluginsMessage.append(desc.getFullName());
						String website = desc.getWebsite();
						if (website != null && !website.isEmpty()) // Add website if found
							pluginsMessage.append(" (").append(desc.getWebsite()).append(")");

						pluginsMessage.append(" ");
					}
					logEx(pluginsMessage.toString());
					logEx("We could not identify which of those are specially related, so this might also be Skript issue.");
				} else {
					logEx("Following plugins are probably related to this error in some way:");
					StringBuilder pluginsMessage = new StringBuilder();
					for (PluginDescriptionFile desc : stackPlugins) {
						pluginsMessage.append(desc.getName());
						String website = desc.getWebsite();
						if (website != null && !website.isEmpty()) // Add website if found
							pluginsMessage.append(" (").append(desc.getWebsite()).append(")");

						pluginsMessage.append(" ");
					}
					logEx(pluginsMessage.toString());
				}

				logEx("You should try disabling those plugins one by one, trying to find which one causes it.");
				logEx("If the error doesn't disappear even after disabling all listed plugins, it is probably Skript issue.");
				logEx("In that case, you will be given instruction on how should you report it.");
				logEx("On the other hand, if the error disappears when disabling some plugin, report it to author of that plugin.");
				logEx("Only if the author tells you to do so, report it to Skript's issue tracker.");
			}
		}

		logEx();
		logEx("Stack trace:");
		if (cause == null || cause.getStackTrace().length == 0) {
			logEx("  warning: no/empty exception given, dumping current stack trace instead");
			cause = new Exception(cause);
		}
		boolean first = true;
		while (cause != null) {
			logEx((first ? "" : "Caused by: ") + cause.toString());
			for (final StackTraceElement e : cause.getStackTrace())
				logEx("    at " + e.toString());
			cause = cause.getCause();
			first = false;
		}

		logEx();
		logEx("Version Information:");
		if (updater != null) {
			ReleaseStatus status = updater.getReleaseStatus();
			logEx("  Skript: " + getVersion() + (status == ReleaseStatus.LATEST ? " (latest)"
					: status == ReleaseStatus.OUTDATED ? " (OUTDATED)"
					: status == ReleaseStatus.CUSTOM ? " (custom version)" : ""));
			ReleaseManifest current = updater.getCurrentRelease();
			logEx("    Flavor: " + current.flavor);
			logEx("    Date: " + current.date);
		} else {
			logEx("  Skript: " + getVersion() + " (unknown; likely custom)");
		}
		logEx("  Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + ")");
		logEx("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
		logEx();
		logEx("Server platform: " + serverPlatform.name + (serverPlatform.supported ? "" : " (unsupported)"));
		logEx();
		logEx("Current node: " + SkriptLogger.getNode());
		logEx("Current item: " + (item == null ? "null" : item.toString(null, true)));
		if (item != null && item.getTrigger() != null) {
			Trigger trigger = item.getTrigger();
			Script script = trigger.getScript();
			logEx("Current trigger: " + trigger.toString(null, true) + " (" + (script == null ? "null" : script.getConfig().getFileName()) + ", line " + trigger.getLineNumber() + ")");
		}
		logEx();
		logEx("Thread: " + (thread == null ? Thread.currentThread() : thread).getName());
		logEx();
		logEx("Language: " + Language.getName());
		logEx();
		logEx("End of Error.");
		logEx();

		return new EmptyStacktraceException();
	}

	static void logEx() {
		LoggerUtils.log(SkriptLogger.LOGGER, Level.SEVERE, EXCEPTION_PREFIX);
	}

	static void logEx(final String... lines) {
		for (final String line : lines)
			LoggerUtils.log(SkriptLogger.LOGGER, Level.SEVERE, EXCEPTION_PREFIX + line);
	}

	private static final Message SKRIPT_PREFIX_MESSAGE = new Message("skript.prefix");

	public static String getSkriptPrefix() {
		return SKRIPT_PREFIX_MESSAGE.getValueOrDefault("<skript_minestom_tag> <reset>");
	}

	/**
	 * Gets the updater instance currently used by Skript.
	 * @return SkriptUpdater instance.
	 */
	@Nullable
	public SkriptUpdater getUpdater() {
		return updater;
	}

}
