package org.bukkit;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitSchedulerImpl;
import org.bukkit.scheduler.DefaultTicker;
import org.bukkit.scheduler.Ticker;
import org.bukkit.util.LoggerUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Collection;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class Bukkit {
	private static final Thread primaryThread = Thread.currentThread();
	private static BooleanSupplier primaryThreadCheck = () -> Thread.currentThread().equals(primaryThread);
	private static final PluginManager pluginManager = new SimplePluginManager();
	private static final Logger trueLogger = LoggerFactory.getLogger(Bukkit.class);
	private static final java.util.logging.Logger logger = generateBadLogger("Bukkit", trueLogger);
	private static BukkitScheduler scheduler = null;
	private static ServicesManager servicesManager = null;
	private static Ticker ticker = new DefaultTicker();
	private static Server server = null;

	public static PluginManager getPluginManager() {
		return pluginManager;
	}

	public static Server getServer() {
		return server;
	}

	public static void setServer(Server server) {
		if (server == null) throw new IllegalStateException("Server has already been set!");
		Bukkit.server = server;
	}

	public static File getServerDirectory() {
		if (server == null) throw new IllegalStateException("Server has not been defined yet!");
		return server.getServerDirectory();
	}

	public static void setServerDirectory(File serverDirectory) {
		server.setServerDirectory(serverDirectory);
	}

	public static java.util.logging.Logger getLogger() {
		return logger;
	}

	public static Logger getBetterLogger() {
		return trueLogger;
	}

	public static @NotNull BukkitScheduler getScheduler() {
		if (scheduler == null)
			scheduler = new BukkitSchedulerImpl();

		return scheduler;
	}

	public static @NotNull ServicesManager getServicesManager() {
		if (servicesManager == null)
			servicesManager = new SimpleServicesManager();
		return servicesManager;
	}

	public static boolean getOnlineMode() {
		return server.getOnlineMode();
	}

	public static String getVersion() {
		return server.getVersion();
	}

	public static String getName() {
		return server.getName();
	}

	public static Collection<Player> getOnlinePlayers() {
		return server.getOnlinePlayers();
	}

	public static Player getPlayer(UUID uuid) {
		return server.getPlayer(uuid);
	}

	public static ConsoleCommandSender getConsoleSender() {
		return server.getConsoleSender();
	}

	public static boolean isPrimaryThread() {
		return primaryThreadCheck.getAsBoolean();
	}

	public static void setPrimaryThreadCheck(BooleanSupplier check) {
		Bukkit.primaryThreadCheck = check;
	}

	public static Thread getPrimaryThread() {
		return primaryThread;
	}

	public static Ticker getTicker() {
		return ticker;
	}

	public static void setTicker(Ticker ticker) {
		Bukkit.ticker = ticker;
	}

	public static java.util.logging.Logger generateBadLogger(@NotNull String name, Logger trueLogger) {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
		logger.addHandler(new Handler() {
			@Override
			public void publish(LogRecord record) {
				LoggerUtils.log(trueLogger, record.getLevel(), ChatColor.stripColor(record.getMessage()));
			}

			@Override
			public void flush() {}

			@Override
			public void close() {}
		});
		logger.setUseParentHandlers(false);
		return logger;
	}

}
