package com.github.hapily04.skriptminestom.command;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.util.FileUtils;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentStringArray;
import org.skriptlang.skript.lang.script.Script;

import java.io.File;
import java.io.IOException;

import static com.github.hapily04.skriptminestom.command.reload.ReloadCommand.fileNotFoundMessage;
import static com.github.hapily04.skriptminestom.command.reload.ReloadCommand.initSuggestions;
import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

// todo fix suggestions for this command
// todo make disabling not temporary (- in file name)
public class DisableCommand extends Command {

	private static final Component DISABLE_USAGE = SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Usage: /skript disable <file>");

	public DisableCommand() {
		super("disable");
		setCondition((sender, _) -> LuckPermsLookup.hasPermission(sender, "skript.disable"));
		setDefaultExecutor((sender, _) -> sender.sendMessage(DISABLE_USAGE));
		Argument<String[]> fileArg = new ArgumentStringArray("to_disable")
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
			Script script = ScriptLoader.getScript(scriptFile);
			if (script == null || !ScriptLoader.getLoadedScripts().contains(script)) {
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>File '<yellow>" + originalProvidedLocation + "<error_color>' isn't an enabled script!"));
				return;
			}
			ScriptLoader.unloadScript(script);
			try {
				FileUtils.move(
					scriptFile,
					new File(scriptFile.getParentFile(), ScriptLoader.DISABLED_SCRIPT_PREFIX + scriptFile.getName()),
					false
				);
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <success_color>Successfully unloaded and disabled <yellow>" + originalProvidedLocation + "<success_color>."));
			} catch (IOException e) {
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize("<skript_minestom_tag> <error_color>Error occurred whilst disabling '<yellow>" + originalProvidedLocation + "<error_color>'!"));
				e.printStackTrace();
			}
		}, fileArg);
	}

}
