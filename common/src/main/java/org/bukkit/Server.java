package org.bukkit;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

public interface Server {

	default PluginManager getPluginManager() {
		return Bukkit.getPluginManager();
	}

	ConsoleCommandSender getConsoleSender();

	Collection<Player> getOnlinePlayers();

	boolean getOnlineMode();

	String getVersion();

	String getName();

	Player getPlayer(UUID uuid);

	File getServerDirectory();

	void setServerDirectory(File serverDirectory);

}
