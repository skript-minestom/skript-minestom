package org.bukkit.plugin.java;

import com.google.common.base.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginClassLoader;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.LoggerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public abstract class JavaPlugin extends PluginBase {
	private PluginClassLoader loader;
	private PluginDescriptionFile description;
	private boolean enabled = false;
	private Logger logger;
	private File dataFolder;
	private File file;
	private FileConfiguration newConfig = null;
	private File configFile = null;
	private final org.slf4j.Logger trueLogger = LoggerFactory.getLogger(getClass());

	@Override
	public void onEnable() {}

	@Override
	public void onDisable() {}

	public final void init(PluginDescriptionFile description, PluginClassLoader loader) {
		this.description = description;
		this.loader = loader;
		configFile = new File(getDataFolder(), "config.yml");
	}

	public @Nullable InputStream getResource(@NotNull String filename) {
		return this.getClass().getResourceAsStream("/" + filename);
	}

	public File getDataFolder() {
		if (dataFolder == null) dataFolder = new File(Bukkit.getServerDirectory(), getName());
		return dataFolder;
	}

	protected File getFile() {
		if (file == null) {
			try {
				URI path = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
				file = new File(path);
			} catch (URISyntaxException ex) {
				getLogger().severe("There was an error getting the file of plugin '" + getName() + "':");
				ex.printStackTrace();
				return null;
			}
		}
		return file;
	}

	@Override
	public Logger getLogger() {
		if (logger == null) {
			logger = Bukkit.generateBadLogger(getName(), trueLogger);
		}
		return logger;
	}

	/**
	 * Gets the command with the given name, specific to this plugin. Commands
	 * need to be registered in the {@link PluginDescriptionFile#getCommands()
	 * PluginDescriptionFile} to exist at runtime.
	 *
	 * @param name name or alias of the command
	 * @return the plugin command if found, otherwise null
	 */
	@Nullable
	public PluginCommand getCommand(@NotNull String name) {
		/*String alias = name.toLowerCase(Locale.ROOT);
		PluginCommand command = getServer().getPluginCommand(alias);

		if (command == null || command.getPlugin() != this) {
			command = getServer().getPluginCommand(description.getName().toLowerCase(Locale.ROOT) + ":" + alias);
		}

		if (command != null && command.getPlugin() == this) {
			return command;
		} else {
			return null;
		}*/
		return new PluginCommand(name, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Nullable
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return null;
	}

	@Override
	public PluginDescriptionFile getDescription() {
		return description;
	}

	@Override
	public @NotNull FileConfiguration getConfig() {
		if (newConfig == null) {
			reloadConfig();
		}
		return newConfig;
	}

	/**
	 * Provides a reader for a text file located inside the jar.
	 * <p>
	 * The returned reader will read text with the UTF-8 charset.
	 *
	 * @param file the filename of the resource to load
	 * @return null if {@link #getResource(String)} returns null
	 * @throws IllegalArgumentException if file is null
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	@Nullable
	protected final Reader getTextResource(@NotNull String file) {
		final InputStream in = getResource(file);

		return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
	}

	@Override
	public void reloadConfig() {
		newConfig = YamlConfiguration.loadConfiguration(configFile);

		final InputStream defConfigStream = getResource("config.yml");
		if (defConfigStream == null) {
			return;
		}

		newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
	}

	@Override
	public void saveConfig() {
		try {
			getConfig().save(configFile);
		} catch (IOException ex) {
			getLogger().severe("Could not save config to " + configFile);
		}
	}

	@Override
	public void saveDefaultConfig() {
		if (!configFile.exists()) {
			saveResource("config.yml", false);
		}
	}

	@Override
	public void saveResource(@NotNull String resourcePath, boolean replace) {
		if (resourcePath == null || resourcePath.equals("")) {
			throw new IllegalArgumentException("ResourcePath cannot be null or empty");
		}

		resourcePath = resourcePath.replace('\\', '/');
		InputStream in = getResource(resourcePath);
		if (in == null) {
			throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + getFile());
		}

		File outFile = new File(dataFolder, resourcePath);
		int lastIndex = resourcePath.lastIndexOf('/');
		File outDir = new File(dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

		if (!outDir.exists()) {
			outDir.mkdirs();
		}

		try {
			if (!outFile.exists() || replace) {
				OutputStream out = new FileOutputStream(outFile);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				in.close();
			} else {
				logger.log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public PluginClassLoader getLoader() {
		return loader;
	}

	public static @Nullable JavaPlugin getProvidingPlugin(Class<?> clazz) {
		for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
			if (!(plugin instanceof JavaPlugin))
				continue;

			JavaPlugin javaPlugin = (JavaPlugin) plugin;

			try {
				javaPlugin.getLoader().loadClass(clazz.getName());
				return javaPlugin;
			} catch (ClassNotFoundException ignored) {}
		}

		return null;
	}
}
