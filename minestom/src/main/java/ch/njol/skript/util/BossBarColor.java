package ch.njol.skript.util;

import net.kyori.adventure.bossbar.BossBar;

public enum BossBarColor {

	PINK_BAR(BossBar.Color.PINK),
	BLUE_BAR(BossBar.Color.BLUE),
	RED_BAR(BossBar.Color.RED),
	GREEN_BAR(BossBar.Color.GREEN),
	YELLOW_BAR(BossBar.Color.YELLOW),
	PURPLE_BAR(BossBar.Color.PURPLE),
	WHITE_BAR(BossBar.Color.WHITE);

	private final BossBar.Color color;

	BossBarColor(BossBar.Color color) {
		this.color = color;
	}

	public BossBar.Color getColor() {
		return color;
	}

	public static BossBarColor of(BossBar.Color color) {
		for (BossBarColor barColor : values()) {
			if (barColor.color == color) return barColor;
		}
		return null;
	}

}
