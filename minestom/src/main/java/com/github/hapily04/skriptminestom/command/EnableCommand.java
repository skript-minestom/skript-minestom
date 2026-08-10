package com.github.hapily04.skriptminestom.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.TimingLogHandler;
import ch.njol.skript.util.FileUtils;
import ch.njol.util.OpenCloseable;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static com.github.hapily04.skriptminestom.command.reload.ReloadCommand.fileNotFoundMessage;
import static com.github.hapily04.skriptminestom.command.reload.ReloadCommand.initSuggestions;
import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class EnableCommand extends Command {

	private static final Component ENABLE_USAGE = SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Usage: /skript enable <file>");

	public EnableCommand() {
		super("enable");
		setCondition((sender, _) -> LuckPermsLookup.hasPermission(sender, "skript.enable"));
		setDefaultExecutor((sender, _) -> sender.sendMessage(ENABLE_USAGE));
		Argument<String[]> fileArg = new ArgumentStringArray("to_enable")
			.setSuggestionCallback((_, ctx, suggestion) -> initSuggestions(suggestion, ctx.getInput(), false));
		addSyntax((sender, context) -> {
			String locationProvided = context.get(fileArg)[0];
			String originalProvidedLocation = locationProvided;
			locationProvided = locationProvided.replace('/', File.separatorChar);
			locationProvided = locationProvided.replace('\\', File.separatorChar);
			File scriptFile = ScriptLoader.getScriptFromName(locationProvided);
			if (scriptFile == null) {
				fileNotFoundMessage(sender, originalProvidedLocation);
				return;
			}
			if (ScriptLoader.getScript(scriptFile) != null) {
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <yellow>" + originalProvidedLocation + " <error_color>is already enabled."));
				return;
			}
			FileFilter filter = ScriptLoader.getDisabledScriptsFilter();
			if (filter.accept(scriptFile)) {
				File newFile = new File(scriptFile.getParentFile(), scriptFile.getName()
					.substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH));
				try {
					FileUtils.move(scriptFile, newFile, false);
					scriptFile = newFile;
				} catch (IOException e) {
					sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Error occurred whilst enabling '<yellow>" + originalProvidedLocation + "<error_color>'!"));
					e.printStackTrace();
				}
			}
			try (TimingLogHandler timingLogHandler = new TimingLogHandler().start()) {
				ScriptLoader.loadScripts(scriptFile, OpenCloseable.combine(new RedirectingLogHandler(sender, null), timingLogHandler))
					.whenComplete((_, _) -> {
						long time = timingLogHandler.getTimeTaken();
						sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <success_color>Successfully enabled <yellow>" + originalProvidedLocation + " <success_color>in " + time + "ms."));
					});
			}

		}, fileArg);
	}

}
