package com.github.hapily04.skriptminestom.bukkit;

import net.minestom.server.Auth;
import net.minestom.server.Git;
import net.minestom.server.MinecraftServer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Collection;
import java.util.UUID;

public class BukkitServer implements Server {

	private static final ConsoleCommandSenderImpl CONSOLE_SENDER = new ConsoleCommandSenderImpl();

	private File serverDirectory;
	private boolean usedServerDirectory = false;

	@Override
	public ConsoleCommandSender getConsoleSender() {
		return CONSOLE_SENDER;
	}

	@Override
	public Collection<Player> getOnlinePlayers() {
		return MinecraftServer.getConnectionManager().getOnlinePlayers().stream().map(p -> new Player(p.getUsername(), p.getUuid())).toList();
	}

	@Override
	public boolean getOnlineMode() {
		return MinecraftServer.process().auth().getClass().equals(Auth.Online.class);
	}

	@Override
	public String getVersion() {
		return MinecraftServer.VERSION_NAME;
	}

	@Override
	public String getName() {
		return "Skript-Minestom (" + Git.version() + ")";
	}

	@Override
	public Player getPlayer(UUID uuid) {
		net.minestom.server.entity.Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
		if (player == null) return null;
		return new Player(player.getUsername(), player.getUuid());
	}

	@Override
	public File getServerDirectory() {
		if (serverDirectory == null) serverDirectory = resolveServerDirectory();
		usedServerDirectory = true;
		return serverDirectory;
	}

	@Override
	public void setServerDirectory(File serverDirectory) {
		if (usedServerDirectory) throw new IllegalStateException("Server directory has already been used!");
		this.serverDirectory = serverDirectory;
	}

	private static File resolveServerDirectory() {
		CodeSource source = BukkitServer.class.getProtectionDomain().getCodeSource();
		if (source != null) {
			try {
				File location = new File(source.getLocation().toURI());
				if (location.isFile()) return location.getParentFile();
			} catch (URISyntaxException | IllegalArgumentException ignored) {}
		}
		return new File(System.getProperty("user.dir"));
	}

}
