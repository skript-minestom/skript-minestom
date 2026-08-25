package com.github.hapily04.skriptminestom.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtils {

	public static final MiniMessage BASIC_MINI_MESSAGE = MiniMessage.builder()
		.postProcessor(component -> component.compact().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
		.tags(TagResolver.resolver(TagResolver.standard(), headTextureTag()))
		.build();

	public static final MiniMessage SKRIPT_MINI_MESSAGE = MiniMessage.builder()
		.postProcessor(component -> component.compact().decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
		.tags(TagResolver.resolver(
			TagResolver.standard(),
			TagResolver.resolver("error_color", Tag.styling(TextColor.color(0xFF7E66))),
			TagResolver.resolver("success_color", Tag.styling(TextColor.color(0x66FF96))),
			TagResolver.resolver("base_grey", Tag.styling(TextColor.color(0xCCC4C4))),
			Placeholder.parsed("skript_minestom_tag", "<dark_gray>[</dark_gray><gradient:#ff6c2f:#ff76b6>Skript-Minestom</gradient><dark_gray>]</dark_gray>")
		)).build();

	public static TagResolver headTextureTag() {
		return TagResolver.resolver("head64", (args, _) -> {
			String texture = args.popOr("Expected texture value").value().strip();
			ObjectContents contents = ObjectContents.playerHead()
				.profileProperty(PlayerHeadObjectContents.property("textures", texture))
				.hat(false)
				.build();
			return Tag.selfClosingInserting(Component.object(contents).color(NamedTextColor.WHITE));
		});
	}

}
