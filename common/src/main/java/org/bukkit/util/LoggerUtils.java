package org.bukkit.util;

import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.slf4j.Logger;

import java.util.logging.Level;

import static org.bukkit.util.MessageUtils.SKRIPT_MINI_MESSAGE;

public class LoggerUtils {

	public static void log(Logger logger, Level level, String msg, Exception e) {
		msg = ansify(msg);
		if (level.equals(Level.SEVERE)) logger.error(msg, e);
		else if (level.equals(Level.WARNING)) logger.warn(msg, e);
		else logger.info(msg, e);
	}

	public static void log(Logger logger, Level level, String msg) {
		msg = ansify(msg);
		if (level.equals(Level.SEVERE)) logger.error(msg);
		else if (level.equals(Level.WARNING)) logger.warn(msg);
		else logger.info(msg);
	}

	private static String ansify(String msg) {
		return ANSIComponentSerializer.ansi().serialize(SKRIPT_MINI_MESSAGE.deserialize(msg));
	}

}
