package com.github.hapily04.skriptminestom.command;

import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import com.github.hapily04.skriptminestom.update.MinestomUpdateService;
import com.github.hapily04.skriptminestom.update.ReleaseNotesFormatter;
import com.github.hapily04.skriptminestom.update.UpdateInfo;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class UpdateCommand extends Command {

	private static final Component CHECKING = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <base_grey>Checking for updates...");
	private static final Component UP_TO_DATE = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <success_color>You are running the latest version.");
	private static final Component CHECK_FAILED = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <error_color>Could not check for updates. Try again later.");
	private static final Component CONFIRM_HINT = SKRIPT_MINI_MESSAGE.deserialize(
		"<base_grey>Run <yellow>/skript update confirm <base_grey>to download and install this release.");
	private static final Component NEED_UPDATE_FIRST = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <error_color>Run <yellow>/skript update <error_color>first when an update is available.");
	private static final Component DOWNLOADING = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <base_grey>Downloading update...");
	private static final Component APPLY_FAILED = SKRIPT_MINI_MESSAGE.deserialize(
		"<skript_minestom_tag> <error_color>Update failed. Check the console for details.");

	public UpdateCommand() {
		super("update");
		setCondition((sender, _) -> LuckPermsLookup.hasPermission(sender, "skript.update"));
		setDefaultExecutor((sender, _) -> {
			sender.sendMessage(CHECKING);
			MinestomUpdateService.checkAsync(result -> {
				switch (result.status()) {
					case FAILED -> {
						MinestomUpdateService.clearConfirmArm();
						sender.sendMessage(CHECK_FAILED);
					}
					case UP_TO_DATE -> {
						MinestomUpdateService.clearConfirmArm();
						sender.sendMessage(UP_TO_DATE);
					}
					case UPDATE_AVAILABLE -> {
						UpdateInfo update = result.update();
						if (update == null) {
							sender.sendMessage(CHECK_FAILED);
							return;
						}
						MinestomUpdateService.armConfirm(update);
						for (Component line : ReleaseNotesFormatter.formatConsoleLines(update)) {
							sender.sendMessage(line);
						}
						sender.sendMessage(CONFIRM_HINT);
					}
				}
			});
		});

		ArgumentLiteral confirm = new ArgumentLiteral("confirm");
		addSyntax((sender, _) -> {
			UpdateInfo update = MinestomUpdateService.consumeConfirmUpdate();
			if (update == null) {
				sender.sendMessage(NEED_UPDATE_FIRST);
				return;
			}
			if (update.downloadUrl() == null || update.assetName() == null) {
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize(
					"<skript_minestom_tag> <error_color>This release has no downloadable jar asset."));
				return;
			}
			sender.sendMessage(DOWNLOADING);
			MinestomUpdateService.applyUpdateAsync(update, shuttingDown -> {
				if (shuttingDown) {
					sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize(
						"<skript_minestom_tag> <success_color>Update installed. Shutting down so the server can restart with the new jar."));
				} else {
					sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize(
						"<skript_minestom_tag> <success_color>Update installed. Restart the server manually to load the new version."));
				}
			}, () -> sender.sendMessage(APPLY_FAILED));
		}, confirm);
	}

}
