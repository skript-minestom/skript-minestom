package com.github.hapily04.skriptminestom;

import ch.njol.skript.bstats.MetricsBase;
import ch.njol.skript.bstats.charts.CustomChart;
import ch.njol.skript.bstats.json.JsonObjectBuilder;
import ch.njol.skript.log.SkriptLogger;
import com.github.hapily04.skriptminestom.util.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Metrics {

	private final MetricsBase metricsBase;

	Metrics(int serviceId) {
		File bStatsFolder = new File(FileUtils.getServerDirectory(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		if (!config.isSet("serverUuid")) {
			config.addDefault("enabled", true);
			config.addDefault("serverUuid", UUID.randomUUID().toString());
			config.addDefault("logFailedRequests", false);
			config.addDefault("logSentData", false);
			config.addDefault("logResponseStatusText", false);
			config.options().header("bStats (https://bStats.org) collects some basic information for plugin authors, like how\nmany people use their plugin and their total player count. It's recommended to keep bStats\nenabled, but if you're not comfortable with this, you can turn this setting off. There is no\nperformance penalty associated with having metrics enabled, and data sent to bStats is fully\nanonymous.\nLearn more here: https://bstats.org/docs/server-owners").copyDefaults(true);

			try {
				config.save(configFile);
			} catch (IOException _) {}
		}

		boolean enabled = config.getBoolean("enabled", true);
		String serverUUID = config.getString("serverUuid");
		boolean logErrors = config.getBoolean("logFailedRequests", false);
		boolean logSentData = config.getBoolean("logSentData", false);
		boolean logResponseStatusText = config.getBoolean("logResponseStatusText", false);

		metricsBase = new MetricsBase("server-implementation", serverUUID, serviceId, enabled, this::appendPlatformData,
			_ -> {}, null, () -> true, SkriptLogger.LOGGER::error, SkriptLogger.LOGGER::info,
			logErrors, logSentData, logResponseStatusText, false);
	}

	public Metrics addCustomChart(CustomChart chart) {
		metricsBase.addCustomChart(chart);
		return this;
	}

	void shutdown() {
		metricsBase.shutdown();
	}

	private void appendPlatformData(JsonObjectBuilder builder) {
		builder.appendField("osName", System.getProperty("os.name"));
		builder.appendField("osArch", System.getProperty("os.arch"));
		builder.appendField("osVersion", System.getProperty("os.version"));
		builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
	}

}
