package org.skriptlang.skript.log.runtime;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;

/**
 * Handles passing runtime errors between producers and consumers via a frame collection system.
 * <br>
 * The manager should be treated as a singleton and accessed via {@link #getInstance()}
 * or {@link Skript#getRuntimeErrorManager()}. Changing the frame length or limits requires edits to the
 * {@link SkriptConfig} values and a call to {@link #refresh()}. Reloading the config will automatically
 * call {@link #refresh()}.
 *
 * @see RuntimeErrorConsumer
 * @see RuntimeErrorProducer
 * @see Frame
 */
public class RuntimeErrorManager implements Closeable {

	private static RuntimeErrorManager instance;

	/**
	 * Prefer using {@link Skript#getRuntimeErrorManager()} instead.
	 * @return The singleton instance of the runtime error manager.
	 */
	@ApiStatus.Internal
	public static RuntimeErrorManager getInstance() {
		return instance;
	}

	/**
	 * Creates a new error manager, which also creates its own frames.
	 * <br>
	 * Must be closed when no longer being used.
	 *
	 * @param frameLength The length of a frame in ticks.
	 */
	public RuntimeErrorManager(long frameLength) {
	}

	/**
	 * Emits a warning or error depending on severity.
	 * @param error The error to emit.
	 */
	public void error(@NotNull RuntimeError error) {
	}

	@Override
	public void close() {
	}

}