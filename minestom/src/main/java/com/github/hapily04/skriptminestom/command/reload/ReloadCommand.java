package com.github.hapily04.skriptminestom.command.reload;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.TimingLogHandler;
import ch.njol.util.OpenCloseable;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsPlayer;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class ReloadCommand extends Command {

	private static final Component RELOAD_USAGE = SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Usage: /skript reload <all/folder/file/config>");
	private static final Component NO_PERMISSION = SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>You don't have permission to do that.");

	public ReloadCommand() {
		super("reload");
		setCondition((sender, _) -> LuckPermsPlayer.hasPermission(sender, "skript.reload"));
		setDefaultExecutor((sender, _) -> sender.sendMessage(RELOAD_USAGE));
		Argument<String[]> folderFileArg = new ArgumentStringArray("to_reload")
			.setSuggestionCallback((sender, ctx, suggestion) -> {
				if (LuckPermsPlayer.hasPermission(sender, "skript.reload.all"))
					suggestion.addEntry(new SuggestionEntry("all"));
				if (LuckPermsPlayer.hasPermission(sender, "skript.reload.config"))
					suggestion.addEntry(new SuggestionEntry("config"));
				if (LuckPermsPlayer.hasPermission(sender, "skript.reload.scripts"))
					initSuggestions(suggestion, ctx.getInput(), false);
			});
		addSyntax((sender, context) -> {
			String locationProvided = context.get(folderFileArg)[0];
			String originalProvidedLocation = locationProvided;
			locationProvided = locationProvided.replace('/', File.separatorChar);
			locationProvided = locationProvided.replace('\\', File.separatorChar);
			if (locationProvided.equalsIgnoreCase("all")) {
				if (cantExecute(sender, "skript.reload.all")) return;
				reloadingMessage(sender, "all scripts and config");
				try (TimingLogHandler timingLogHandler = new TimingLogHandler().start()) {
					try (RedirectingLogHandler redirectingLogHandler = new RedirectingLogHandler(sender, null).start()) {
						SkriptConfig.load();
						ScriptLoader.unloadScripts(ScriptLoader.getLoadedScripts());
						ScriptLoader.loadScripts(Skript.getInstance().getScriptsFolder(), OpenCloseable.combine(redirectingLogHandler, timingLogHandler))
									.whenComplete((_, _) -> reloadedMessage(sender, timingLogHandler, "all scripts and config"));
					}
				}
			} else if (locationProvided.equalsIgnoreCase("config")) {
				if (cantExecute(sender, "skript.reload.config")) return;
				reloadingMessage(sender, "config");
				try (TimingLogHandler timingLogHandler = new TimingLogHandler().start()) {
					SkriptConfig.load();
					reloadedMessage(sender, timingLogHandler, "config");
				}
			} else {
				if (cantExecute(sender, "skript.reload.scripts")) return;
				File scriptFile = ScriptLoader.getScriptFromName(locationProvided);
				if (scriptFile == null) {
					fileNotFoundMessage(sender, originalProvidedLocation);
					return;
				}
				boolean directory = scriptFile.isDirectory();
				reloadingMessage(sender, directory ? "scripts in " + originalProvidedLocation : originalProvidedLocation);
				try (TimingLogHandler timingLogHandler = new TimingLogHandler().start()) {
					try (RedirectingLogHandler redirectingLogHandler = new RedirectingLogHandler(sender, null).start()) {
						if (directory) {
							ScriptLoader.unloadScripts(ScriptLoader.getScripts(scriptFile));
							ScriptLoader.loadScripts(scriptFile, OpenCloseable.combine(redirectingLogHandler, timingLogHandler))
										.whenComplete((_, _) -> {
											reloadedMessage(sender, timingLogHandler, "scripts in " + originalProvidedLocation);
										});
							return;
						}
						Script script = ScriptLoader.getScript(scriptFile);
						if (script != null) ScriptLoader.unloadScript(script);
						ScriptLoader.loadScripts(scriptFile, OpenCloseable.combine(redirectingLogHandler, timingLogHandler))
									.whenComplete((_, _) -> reloadedMessage(sender, timingLogHandler, originalProvidedLocation));
					}
				}
			}
		}, folderFileArg);
	}

	public static void initSuggestions(@NotNull Suggestion suggestion, String input, boolean enable) {
		String[] tokens = input.split(" ");
		String scriptArg = tokens.length > 2 ? tokens[2].replace("\0", "") : "";
		File scripts = Skript.getInstance().getScriptsFolder();
		String scriptsPathString = scripts.toPath().toString();
		int scriptsPathLength = scriptsPathString.length();

		String fs = File.separator;

		// Live update, this will get all old and new (even not loaded) scripts
		try (Stream<Path> files = Files.walk(scripts.toPath())) {
			files.map(Path::toFile)
				.forEach(file -> {
					if (!(enable ? ScriptLoader.getDisabledScriptsFilter() : ScriptLoader.getLoadedScriptsFilter()).accept(file))
						return;

					// Ignore hidden files like .git/ for users that use git source control.
					if (file.isHidden())
						return;

					String fileString = file.toString().substring(scriptsPathLength);
					if (fileString.isEmpty())
						return;

					if (file.isDirectory()) {
						fileString = fileString + fs; // Add file separator at the end of directories
					} else if (file.getParentFile().toPath().toString().equals(scriptsPathString)) {
						fileString = fileString.substring(1); // Remove file separator from the beginning of files or directories in root only
						if (fileString.isEmpty())
							return;
					}

					// Make sure the user's argument matches with the file's name or beginning of file path
					if (!scriptArg.isEmpty() && !file.getName().startsWith(scriptArg) && !fileString.startsWith(scriptArg))
						return;

					suggestion.addEntry(new SuggestionEntry(fileString.replace('\\', '/')));
				});
		} catch (Exception e) {
			//noinspection ThrowableNotThrown
			Skript.exception(e, "An error occurred while trying to update the list of disabled scripts!");
		}
	}

	public static void fileNotFoundMessage(CommandSender sender, String fileInQuestion) {
		sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>File '<yellow>" + fileInQuestion + "<error_color>' was not found in the scripts folder."));
	}

	static void reloadingMessage(CommandSender sender, String whatToReload) {
		sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <base_grey>Reloading <yellow>" + whatToReload + "<base_grey>..."));
	}

	static void reloadedMessage(CommandSender sender, TimingLogHandler timingLogHandler, String whatToReload) {
		long time = timingLogHandler.getTimeTaken();
		sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <success_color>Successfully reloaded <yellow>" + whatToReload + " <success_color>in " + time + "ms."));
	}

	private static boolean cantExecute(CommandSender sender, String permission) {
		boolean hasPermission = LuckPermsPlayer.hasPermission(sender, permission);
		if (!hasPermission) sender.sendMessage(NO_PERMISSION);
		return !hasPermission;
	}

}
