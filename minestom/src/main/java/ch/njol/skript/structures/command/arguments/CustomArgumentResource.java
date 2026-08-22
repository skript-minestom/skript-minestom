package ch.njol.skript.structures.command.arguments;

import net.kyori.adventure.key.Key;
import net.minestom.server.command.ArgumentParserType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class CustomArgumentResource<T> extends Argument<T> {

	public static final int SPACE_ERROR = 1;
	public static final int PARSE_ERROR = 2;

	private final String identifier;

	public CustomArgumentResource(String id, String identifier) {
		super(id);
		this.identifier = identifier;
	}

	@Override
	public T parse(@NonNull CommandSender sender, String input) throws ArgumentSyntaxException {
		if (input.contains(StringUtils.SPACE))
			throw new ArgumentSyntaxException("Resource location cannot contain space character", input, SPACE_ERROR);

		if (!Key.parseable(input))
			throw new ArgumentSyntaxException("Invalid resource location", input, PARSE_ERROR);

		return getValue(sender, Key.key(input));
	}

	protected abstract T getValue(CommandSender sender, Key key);

	@Override
	public @NonNull ArgumentParserType parser() {
		return ArgumentParserType.RESOURCE;
	}

	@Override
	public String toString() {
		return String.format("Resource<%s>", getId());
	}

	@Override
	public byte @Nullable [] nodeProperties() {
		return NetworkBuffer.makeArray(NetworkBuffer.STRING, identifier);
	}

}
