package com.github.hapily04.skriptminestom.util;

import net.minestom.server.Auth;
import net.minestom.server.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class PropertyUtils {

	public static final String ADDRESS_KEY = "server-ip";
	public static final String PORT_KEY = "server-port";
	public static final String AUTH_TYPE_KEY = "auth-type";
	public static final String PROXY_KEY = "proxy-key";
	public static final String CHUNK_VIEW_DISTANCE = "chunk-view-distance";
	public static final String ENTITY_VIEW_DISTANCE = "entity-view-distance";
	public static final String DISPATCHER_THREADS = "dispatcher-threads";
	public static final String LOG_MINESTOM_EXCEPTIONS = "log-minestom-exceptions";
    public static final String SERVER_STOPPING_MINIMESSAGE = "server-stopping-minimessage";

	private static final File SERVER_PROPERTIES_FILE = new File(FileUtils.getServerDirectory(), "server.properties");

	private PropertyUtils() {}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static Properties loadServerProperties() {
		Properties properties = populateDefaults(new Properties());
		try {
			if (!SERVER_PROPERTIES_FILE.exists()) {
				SERVER_PROPERTIES_FILE.createNewFile();
			} else properties.load(new FileReader(SERVER_PROPERTIES_FILE));
			// pterodactyl creates file first, so ensure defaults are set in the file for users
			properties.store(new FileWriter(SERVER_PROPERTIES_FILE), null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return properties;
	}

	private static Properties populateDefaults(Properties properties) {
		properties.setProperty(ADDRESS_KEY, "0.0.0.0");
		properties.setProperty(PORT_KEY, "25565");
		properties.setProperty(AUTH_TYPE_KEY, "");
		properties.setProperty(PROXY_KEY, "");
		properties.setProperty(CHUNK_VIEW_DISTANCE, "12");
		properties.setProperty(ENTITY_VIEW_DISTANCE, "12");
		properties.setProperty(DISPATCHER_THREADS, "1");
		properties.setProperty(LOG_MINESTOM_EXCEPTIONS, "true");
        properties.setProperty(SERVER_STOPPING_MINIMESSAGE, "<white>Server is stopping.");
		return properties;
	}

	public static Auth getAuth(Properties properties) {
		String authType = properties.getProperty(AUTH_TYPE_KEY);
		String proxyKey = properties.getProperty(PROXY_KEY);
		return switch (authType.toLowerCase(Locale.ENGLISH)) {
			case "mojang" -> new Auth.Online();
			case "velocity" -> new Auth.Velocity(proxyKey);
			case "bungee", "bungeecord" -> {
				Set<String> tokens = null;
				if (!proxyKey.isBlank()) tokens = Set.of(proxyKey);
				yield new Auth.Bungee(tokens);
			}
			default -> new Auth.Offline();
		};
	}

}
