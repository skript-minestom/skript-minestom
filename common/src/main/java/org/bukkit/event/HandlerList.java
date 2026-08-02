package org.bukkit.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HandlerList {
	private static final List<HandlerList> allLists = new ArrayList<>();

	private static final RegisteredListener[] EMPTY = new RegisteredListener[0];

	private final List<RegisteredListener> listeners = new ArrayList<>();

	private volatile RegisteredListener[] baked = EMPTY;

	public HandlerList() {
		synchronized (allLists) {
			allLists.add(this);
		}
	}

	public synchronized void register(RegisteredListener handler) {
		listeners.add(handler);
		baked = bake();
	}

	public synchronized void unregister(Listener listener) {
		listeners.removeIf((registeredListener) -> registeredListener.getListener() == listener);
		baked = bake();
	}

	public static void unregisterAll(Listener listener) {
		List<HandlerList> snapshot;
		synchronized (allLists) {
			snapshot = List.copyOf(allLists);
		}
		for (HandlerList handlerList : snapshot) {
			handlerList.unregister(listener);
		}
	}

	public RegisteredListener[] getBakedListeners() {
		return baked;
	}

	public synchronized List<RegisteredListener> getRegisteredListeners() {
		return List.copyOf(listeners);
	}

	private RegisteredListener[] bake() {
		RegisteredListener[] baked = listeners.toArray(EMPTY);
		Arrays.sort(baked, (a, b) -> {
			int priorityA = a.getPriority().ordinal();
			int priorityB = b.getPriority().ordinal();

			return Integer.compare(priorityB, priorityA);
		});
		return baked;
	}
}
