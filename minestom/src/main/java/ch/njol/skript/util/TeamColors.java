package ch.njol.skript.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.color.TeamColor;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Locale;

public final class TeamColors {

	private TeamColors() {}

	public static @Nullable TeamColor toTeamColor(@Nullable NamedTextColor color) {
		if (color == null) return null;
		try {
			return TeamColor.valueOf(color.toString().toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static @Nullable NamedTextColor toNamedTextColor(@Nullable TeamColor color) {
		if (color == null) return null;
		return NamedTextColor.NAMES.value(color.name().toLowerCase(Locale.ENGLISH));
	}

}
