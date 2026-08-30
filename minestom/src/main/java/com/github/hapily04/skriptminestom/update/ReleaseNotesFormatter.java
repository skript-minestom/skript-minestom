package com.github.hapily04.skriptminestom.update;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.hapily04.skriptminestom.util.MessageUtils.SKRIPT_MINI_MESSAGE;

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
		lines.add(SKRIPT_MINI_MESSAGE.deserialize("  <base_grey>Release notes:"));

		for (String rawLine : splitBodyLines(info.body())) {
			String cleaned = stripMarkdown(rawLine);
			if (cleaned.isBlank()) continue;
			TagResolver note = Placeholder.unparsed("note", cleaned);
			if (cleaned.startsWith("- ") || cleaned.startsWith("* ")) {
				lines.add(SKRIPT_MINI_MESSAGE.deserialize(
					"   <base_grey><note>", note));
			} else if (cleaned.toLowerCase().startsWith("full changelog")) {
				String url = extractUrl(cleaned, info.htmlUrl());
				if (url != null) {
					TagResolver link = Placeholder.unparsed("url", url);
					lines.add(SKRIPT_MINI_MESSAGE.deserialize(
						"   <base_grey>Full Changelog: <yellow><click:open_url:<url>><url>", link));
				} else {
					lines.add(SKRIPT_MINI_MESSAGE.deserialize(
						"   <base_grey><note>", note));
				}
			} else {
				lines.add(SKRIPT_MINI_MESSAGE.deserialize(
					"   <base_grey><note>", note));
			}
		}

		if (info.htmlUrl() != null && !info.htmlUrl().isBlank()) {
			TagResolver link = Placeholder.unparsed("url", info.htmlUrl());
			lines.add(SKRIPT_MINI_MESSAGE.deserialize(
				"  <base_grey>GitHub: <yellow><click:open_url:<url>><url>", link));
		}
		return lines;
	}

	private static List<String> splitBodyLines(@Nullable String body) {
		if (body == null || body.isBlank()) return List.of();
		return List.of(body.replace("\r\n", "\n").replace('\r', '\n').split("\n"));
	}

	private static String stripMarkdown(String line) {
		String s = line.trim();
		s = s.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1 ($2)");
		s = s.replace("**", "");
		return s;
	}

	private static @Nullable String extractUrl(String line, @Nullable String fallback) {
		int http = line.indexOf("https://");
		if (http >= 0) {
			String rest = line.substring(http).trim();
			int end = rest.indexOf(' ');
			return end < 0 ? rest : rest.substring(0, end);
		}
		return fallback;
	}

}
