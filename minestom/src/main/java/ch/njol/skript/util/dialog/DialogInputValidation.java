package ch.njol.skript.util.dialog;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.log.SkriptLogger;
import net.minestom.server.dialog.DialogInput;

public final class DialogInputValidation {

	private DialogInputValidation() {}

	public static boolean isValidKey(String key) {
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_') return false;
		}
		return true;
	}

	public static boolean checkLiteralKey(Expression<String> keyExpression, String syntaxName) {
		if (keyExpression instanceof Literal<String> literal) {
			String key = literal.getSingle();
			if (key != null && !isValidKey(key)) {
				Skript.error("'" + key + "' is not a valid name for a " + syntaxName
					+ ". Names may only contain letters, digits and underscores.");
				return false;
			}
		}
		return true;
	}

	public static boolean checkRuntimeKey(String key, String syntaxName) {
		if (isValidKey(key)) return true;
		SkriptLogger.LOGGER.error("'{}' is not a valid name for a {}. Names may only contain letters, digits and underscores.", key, syntaxName);
		return false;
	}

}
