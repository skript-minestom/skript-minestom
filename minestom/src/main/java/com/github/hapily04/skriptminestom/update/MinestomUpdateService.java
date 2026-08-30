package com.github.hapily04.skriptminestom.update;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Version;
import com.github.hapily04.skriptminestom.SkriptMinestom;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import com.github.hapily04.skriptminestom.util.NumberUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public final class MinestomUpdateService {

	private static final String LATEST_RELEASE_API = "https://api.github.com/repos/skript-minestom/skript-minestom/releases/latest";
	private static final String RELEASES_PAGE = "https://github.com/skript-minestom/skript-minestom/releases";
	private static final String STATE_FILE_NAME = "update-state.json";
	private static final String USER_AGENT = "Skript-Minestom-Updater";

	private static final Gson GSON = new Gson();
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "skript-minestom-updater");
		thread.setDaemon(true);
		return thread;
	});
	private static final AtomicReference<UpdateInfo> availableUpdate = new AtomicReference<>();
	private static final AtomicReference<UpdateInfo> cachedNotes = new AtomicReference<>();
	private static final AtomicBoolean confirmArmed = new AtomicBoolean(false);
	private static volatile boolean pendingNotesPrinted;

	private MinestomUpdateService() {}

	/**
	 * Deletes a previous jar marked for removal after an update restart,
	 * and loads cached release notes for console display.
	 */
	public static void cleanupAfterRestart() {
		UpdateState state = readState();
		if (state == null) return;

		if (state.oldJar != null && !state.oldJar.isBlank()) {
			Path oldJar = Path.of(state.oldJar);
			Path currentJar = currentJarPath();
			if (currentJar == null || !oldJar.toAbsolutePath().normalize().equals(currentJar.toAbsolutePath().normalize())) {
				deleteQuietlyWithRetry(oldJar);
			}
			state.oldJar = null;
			writeState(state);
		}

		if (state.releaseNotes != null || state.newVersion != null) {
			cachedNotes.set(new UpdateInfo(
				state.newVersion != null ? state.newVersion : com.github.hapily04.skriptminestom.Version.VERSION,
				state.releaseName != null ? state.releaseName : "",
				state.releaseNotes != null ? state.releaseNotes : "",
				state.htmlUrl != null ? state.htmlUrl : RELEASES_PAGE,
				null,
				null
			));
		}
	}

	/** Fire-and-forget startup update check on the updater thread. */
	public static void runStartupCheck() {
		if (!Boolean.TRUE.equals(SkriptConfig.checkForNewVersion.value())) return;
		EXECUTOR.execute(() -> {
			UpdateCheckResult result = performCheck();
			switch (result.status()) {
				case FAILED -> sendStartupLog(SKRIPT_MINI_MESSAGE.deserialize(
					"<skript_minestom_tag> <error_color>Update checker: could not fetch latest release information."));
				case UP_TO_DATE -> {
					TagResolver version = Placeholder.unparsed(
						"version", com.github.hapily04.skriptminestom.Version.VERSION);
					sendStartupLog(SKRIPT_MINI_MESSAGE.deserialize(
						"<skript_minestom_tag> <success_color>Update checker: running the latest version (<yellow><version><success_color>).",
						version));
				}
				case UPDATE_AVAILABLE -> {
					TagResolver latest = Placeholder.unparsed("latest", result.update().tagName());
					TagResolver current = Placeholder.unparsed(
						"current", com.github.hapily04.skriptminestom.Version.VERSION);
					sendStartupLog(SKRIPT_MINI_MESSAGE.deserialize(
						"<skript_minestom_tag> <error_color>Update checker: new version available: <latest> <base_grey>(current: <yellow><current><base_grey>). Use <yellow>/skript update <base_grey>then <yellow>/skript update confirm <base_grey>to install.",
						latest, current));
				}
			}
		});
	}

	private static void sendStartupLog(Component message) {
		MinecraftServer.getSchedulerManager().scheduleNextTick(() ->
			MinecraftServer.getCommandManager().getConsoleSender().sendMessage(message));
	}

	public static void registerFinishedLoadingHook() {
		Skript.onFinishedLoading(MinestomUpdateService::printPendingReleaseNotes);
	}

	public static void registerJoinListener(EventNode<net.minestom.server.event.Event> node) {
		node.addListener(PlayerSpawnEvent.class, event -> {
			if (!event.isFirstSpawn()) return;
			UpdateInfo update = availableUpdate.get();
			if (update == null) return;
			Player player = event.getPlayer();
			String permission = SkriptConfig.updateNotificationPermission.value();
			if (permission == null || permission.isBlank()) permission = "skript.admin";
			if (!LuckPermsLookup.hasPermission(player, permission)) return;

			TagResolver version = Placeholder.unparsed("version", update.tagName());
			TagResolver url = Placeholder.unparsed("url", update.htmlUrl() != null ? update.htmlUrl() : RELEASES_PAGE);
			player.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("""
				<skript_minestom_tag> <yellow>A new version is available: <version>
				  <base_grey>Download: <yellow><click:open_url:<url>><url>
				  <base_grey>Run <yellow>/skript update <base_grey>then <yellow>/skript update confirm <base_grey>to install.""",
				version, url));
		});
	}

	/**
	 * Schedules periodic update checks after the server is running.
	 * The scheduler only kicks off work; HTTP runs on {@link #EXECUTOR}.
	 */
	public static void schedulePeriodicChecks() {
		if (!Boolean.TRUE.equals(SkriptConfig.checkForNewVersion.value())) return;
		Timespan interval = SkriptConfig.updateCheckInterval.value();
		if (interval == null || interval.getMilliSeconds() <= 0) return;

		long ticks = Math.max(1, NumberUtils.ticksFrom(interval));
		MinecraftServer.getSchedulerManager().scheduleTask(
			() -> EXECUTOR.execute(MinestomUpdateService::performCheck),
			TaskSchedule.tick((int) ticks),
			TaskSchedule.tick((int) ticks)
		);
	}

	/**
	 * Runs an update check on the updater thread and delivers the result on the next server tick.
	 */
	public static void checkAsync(Consumer<UpdateCheckResult> callback) {
		EXECUTOR.execute(() -> {
			UpdateCheckResult result = performCheck();
			MinecraftServer.getSchedulerManager().scheduleNextTick(() -> callback.accept(result));
		});
	}

	public static void armConfirm(UpdateInfo update) {
		availableUpdate.set(update);
		cachedNotes.set(update);
		confirmArmed.set(true);
	}

	public static void clearConfirmArm() {
		confirmArmed.set(false);
	}

	public static boolean isConfirmArmed() {
		return confirmArmed.get() && availableUpdate.get() != null;
	}

	/**
	 * Clears the confirm arm and returns the armed update, or {@code null} if confirm was not armed.
	 */
	public static @Nullable UpdateInfo consumeConfirmUpdate() {
		if (!confirmArmed.getAndSet(false)) return null;
		return availableUpdate.get();
	}

	/**
	 * Downloads the update and relaunches on the updater thread.
	 * Invokes {@code onFailure} on the next tick if something goes wrong before exit.
	 */
	public static void applyUpdateAsync(UpdateInfo update, Runnable onFailure) {
		EXECUTOR.execute(() -> {
			try {
				applyUpdate(update);
			} catch (Exception e) {
				SkriptLogger.LOGGER.error("Update failed: " + e.getMessage());
				e.printStackTrace();
				MinecraftServer.getSchedulerManager().scheduleNextTick(onFailure);
			}
		});
	}

	public static @Nullable UpdateInfo getAvailableUpdate() {
		return availableUpdate.get();
	}

	private static UpdateCheckResult performCheck() {
		try {
			UpdateInfo latest = fetchLatestRelease();
			if (latest == null) return UpdateCheckResult.failed();
			if (!isNewer(latest.tagName(), com.github.hapily04.skriptminestom.Version.VERSION)) {
				availableUpdate.set(null);
				cachedNotes.set(latest);
				clearConfirmArm();
				return UpdateCheckResult.upToDate(latest);
			}
			availableUpdate.set(latest);
			cachedNotes.set(latest);
			return UpdateCheckResult.available(latest);
		} catch (Exception e) {
			SkriptLogger.LOGGER.warn("Update check failed: " + e.getMessage());
			return UpdateCheckResult.failed();
		}
	}

	private static void printPendingReleaseNotes() {
		UpdateState state = readState();
		if (state == null || !state.printNotesOnLoad) return;
		if (pendingNotesPrinted) return;
		pendingNotesPrinted = true;

		UpdateInfo info = new UpdateInfo(
			state.newVersion != null ? state.newVersion : com.github.hapily04.skriptminestom.Version.VERSION,
			state.releaseName != null ? state.releaseName : "",
			state.releaseNotes != null ? state.releaseNotes : "",
			state.htmlUrl != null ? state.htmlUrl : RELEASES_PAGE,
			null,
			null
		);
		ConsoleSender consoleSender = MinecraftServer.getCommandManager().getConsoleSender();
		for (Component line : ReleaseNotesFormatter.formatConsoleLines(info, true)) {
			consoleSender.sendMessage(line);
		}

		state.printNotesOnLoad = false;
		writeState(state);
	}

	private static void applyUpdate(UpdateInfo latest) throws IOException {
		if (latest.downloadUrl() == null || latest.assetName() == null)
			throw new IOException("Latest release has no downloadable jar asset.");

		Path currentJar = currentJarPath();
		if (currentJar == null || !Files.isRegularFile(currentJar))
			throw new IOException("Could not resolve the currently running jar.");

		Path targetJar = currentJar.getParent().resolve(latest.assetName());
		if (targetJar.toAbsolutePath().normalize().equals(currentJar.toAbsolutePath().normalize()))
			throw new IOException("Download target matches the running jar path; aborting.");

		SkriptLogger.LOGGER.info("Update: downloading " + latest.assetName() + "...");
		download(latest.downloadUrl(), targetJar);
		SkriptLogger.LOGGER.info("Update: download complete. Restarting into the new jar...");

		writeState(new UpdateState(currentJar.toAbsolutePath().toString(), latest.tagName(), latest.releaseName(),
			latest.body(), latest.htmlUrl(), true));

		relaunch(targetJar);
	}

	private static void relaunch(Path newJar) throws IOException {
		Optional<String> command = ProcessHandle.current().info().command();
		if (command.isEmpty())
			throw new IOException("Could not determine the Java executable for relaunch.");

		List<String> cmd = new ArrayList<>();
		cmd.add(command.get());
		cmd.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
		cmd.add("-jar");
		cmd.add(newJar.toAbsolutePath().toString());

		Optional<String[]> rawArgs = ProcessHandle.current().info().arguments();
		if (rawArgs.isPresent()) {
			String[] args = rawArgs.get();
			boolean afterJar = false;
			for (int i = 0; i < args.length; i++) {
				if (afterJar) cmd.add(args[i]);
				else if ("-jar".equals(args[i]) && i + 1 < args.length) {
					i++;
					afterJar = true;
				}
			}
		}

		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.directory(newJar.getParent() != null ? newJar.getParent().toFile() : new File(".").getAbsoluteFile());
		builder.inheritIO();
		builder.start();
		System.exit(0);
	}

	static @Nullable UpdateInfo fetchLatestRelease() throws IOException {
		URL url = URI.create(LATEST_RELEASE_API).toURL();
		var connection = url.openConnection();
		connection.setRequestProperty("Accept", "application/vnd.github+json");
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setConnectTimeout(15_000);
		connection.setReadTimeout(30_000);

		try (InputStream in = connection.getInputStream();
			 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			GithubRelease release = GSON.fromJson(reader, GithubRelease.class);
			if (release == null || release.tagName == null) return null;

			String downloadUrl = null;
			String assetName = null;
			if (release.assets != null) {
				for (GithubAsset asset : release.assets) {
					if (asset == null || asset.name == null || asset.browserDownloadUrl == null) continue;
					if (asset.name.endsWith(".jar")) {
						downloadUrl = asset.browserDownloadUrl;
						assetName = asset.name;
						break;
					}
				}
			}

			return new UpdateInfo(release.tagName, release.name != null ? release.name : release.tagName,
				release.body != null ? release.body : "", release.htmlUrl != null ? release.htmlUrl : RELEASES_PAGE, downloadUrl,
				assetName);
		}
	}

	static boolean isNewer(String remoteTag, String currentVersion) {
		if (remoteTag == null || currentVersion == null) return false;
		String remote = stripLeadingV(remoteTag.trim());
		String current = stripLeadingV(currentVersion.trim());
		if (remote.equals(current)) return false;
		try {
			return new Version(remote).isLargerThan(new Version(current));
		} catch (IllegalArgumentException e) {
			return remote.compareTo(current) > 0;
		}
	}

	private static String stripLeadingV(String version) {
		if (version.length() > 1 && (version.charAt(0) == 'v' || version.charAt(0) == 'V')
			&& Character.isDigit(version.charAt(1))) return version.substring(1);
		return version;
	}

	private static void download(String downloadUrl, Path target) throws IOException {
		Path temp = target.resolveSibling(target.getFileName() + ".download");
		URL url = URI.create(downloadUrl).toURL();
		var connection = url.openConnection();
		connection.setRequestProperty("User-Agent", USER_AGENT);
		connection.setConnectTimeout(15_000);
		connection.setReadTimeout(120_000);
		try (InputStream in = connection.getInputStream()) {
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
		}
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
	}

	private static @Nullable Path currentJarPath() {
		try {
			return Path.of(SkriptMinestom.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (Exception e) {
			return null;
		}
	}

	private static Path stateFile() {
		return Skript.getInstance().getDataFolder().toPath().resolve(STATE_FILE_NAME);
	}

	private static @Nullable UpdateState readState() {
		try {
			Path file = stateFile();
			if (!Files.isRegularFile(file)) return null;
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				return GSON.fromJson(reader, UpdateState.class);
			}
		} catch (Exception e) {
			return null;
		}
	}

	private static void writeState(UpdateState state) {
		try {
			Path file = stateFile();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(state), StandardCharsets.UTF_8);
		} catch (IOException e) {
			SkriptLogger.LOGGER.warn("Could not write update state: " + e.getMessage());
		}
	}

	private static void deleteQuietlyWithRetry(Path path) {
		if (path == null || !Files.exists(path)) return;
		for (int i = 0; i < 10; i++) {
			try {
				Files.deleteIfExists(path);
				SkriptLogger.LOGGER.info("Update: deleted previous jar " + path.getFileName());
				return;
			} catch (IOException e) {
				try {
					Thread.sleep(200L * (i + 1));
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
		SkriptLogger.LOGGER.warn("Update: could not delete previous jar (will retry next boot): " + path);
	}

	@SuppressWarnings("unused")
	private static final class GithubRelease {
		@SerializedName("tag_name")
		String tagName;
		String name;
		String body;
		@SerializedName("html_url")
		String htmlUrl;
		List<GithubAsset> assets;
	}

	@SuppressWarnings("unused")
	private static final class GithubAsset {
		String name;
		@SerializedName("browser_download_url")
		String browserDownloadUrl;
	}

}
