package com.github.hapily04.skriptminestom.command;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import com.github.hapily04.skriptminestom.Version;
import com.github.hapily04.skriptminestom.luckperms.LuckPermsLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.Git;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

import java.util.Collection;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class InfoCommand extends Command {

	private static final String INFO_MESSAGE = """
		<skript_minestom_tag> <base_grey>Info
		  <base_grey>GitHub: <yellow><click:open_url:https://github.com/skript-minestom/skript-minestom>https://github.com/skript-minestom/skript-minestom
		  <base_grey>Minestom Version: <yellow><minestom_version>
		  <base_grey>Skript-Minestom Version: <yellow><skript_minestom_version>""";
	private static final Component INSTALLED_ADDONS = SKRIPT_MINI_MESSAGE.deserialize("  <base_grey>Installed Addons:");
	private static final String INSTALLED_ADDON = "   <base_grey>- <yellow><addon_name> v<addon_version>";

	public InfoCommand() {
		super("info");
		setCondition((sender, _) -> LuckPermsLookup.hasPermission(sender, "skript.info"));
		setDefaultExecutor((sender, _) -> {
			TagResolver minestomVersion = Placeholder.unparsed("minestom_version", Git.version() + " (MC " + MinecraftServer.VERSION_NAME + ")");
			TagResolver skriptMinestomVersion = Placeholder.unparsed("skript_minestom_version", Version.VERSION);
			sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize(INFO_MESSAGE, minestomVersion, skriptMinestomVersion));
			Collection<SkriptAddon> addons = Skript.getAddons();
			if (addons.isEmpty()) return;
			sender.sendMessage(INSTALLED_ADDONS);
			for (SkriptAddon skriptAddon : addons) {
				TagResolver addonName = Placeholder.unparsed("addon_name", skriptAddon.name());
				TagResolver addonVersion = Placeholder.unparsed("addon_version", skriptAddon.version.toString());
				String installedAddon = INSTALLED_ADDON;
				String website = skriptAddon.plugin.getDescription().getWebsite();
				if (website != null) installedAddon += " (<click:open_url:" + website + ">" + website + ")";
				sender.sendMessage(SKRIPT_MINI_MESSAGE.deserialize(installedAddon, addonName, addonVersion));
			}
		});
	}

}
