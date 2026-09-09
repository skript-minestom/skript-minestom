package com.github.hapily04.skriptminestom.update;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

/**
 * Formats update summaries for console / chat (InfoCommand style).
 */
public final class ReleaseNotesFormatter {

	private ReleaseNotesFormatter() {}

	public static List<Component> formatConsoleLines(UpdateInfo info) {
		return formatConsoleLines(info, false);
	}

	public static List<Component> formatConsoleLines(UpdateInfo info, boolean afterUpdate) {
		List<Component> lines = new ArrayList<>();
		TagResolver version = Placeholder.unparsed("version", info.tagName());
		TagResolver title = Placeholder.unparsed("title", info.releaseName() == null || info.releaseName().isBlank()
			? info.tagName() : info.releaseName());

		if (afterUpdate) {
			lines.add(SKRIPT_MINI_MESSAGE.deserialize(
				"<skript_minestom_tag> <success_color>Updated to <yellow><version>", version));
		} else {
			lines.add(SKRIPT_MINI_MESSAGE.deserialize(
				"<skript_minestom_tag> <yellow>Update available: <version>", version));
		}

		lines.add(SKRIPT_MINI_MESSAGE.deserialize(
			"  <base_grey>Release: <yellow><title>", title));

		String releaseUrl = releasePageUrl(info);
		if (releaseUrl != null) {
			lines.add(SKRIPT_MINI_MESSAGE.deserialize("  <base_grey>Release notes: ")
				.append(releaseNotesLink(releaseUrl)));
		}

		return lines;
	}

	public static Component releaseNotesLink(String url) {
		return Component.text("View on GitHub", TextColor.color(0xFFFF00))
			.clickEvent(ClickEvent.openUrl(url));
	}

	private static @Nullable String releasePageUrl(UpdateInfo info) {
		if (info.htmlUrl() != null && !info.htmlUrl().isBlank())
			return info.htmlUrl();
		return null;
	}

}
