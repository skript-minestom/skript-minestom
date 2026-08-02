package org.bukkit.plugin;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimplePluginManager implements PluginManager {

	private final Map<String, Plugin> plugins = new HashMap<>();

	public List<Plugin> getPlugins() {
		return plugins.values().stream().toList();
	}

	public void registerEvent(
		Class<? extends Event> event,
		Listener listener,
		EventPriority priority,
		EventExecutor executor,
		Plugin plugin
	) {
		registerEvent(event, listener, priority, executor, plugin, false);
	}

	@Override
	public void registerEvent(Class<? extends Event> event, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
		try {
			HandlerList handlerList = getHandlerList(event);
			handlerList.register(new RegisteredListener(plugin, executor, priority, ignoreCancelled, listener));
		} catch (ReflectiveOperationException exception) {
			exception.printStackTrace();
		}
	}

	public void registerEvents(Plugin plugin, Listener listener) {
		for (Method method : listener.getClass().getMethods()) {
			if (!method.isAnnotationPresent(EventHandler.class) || method.getParameterCount() != 1)
				continue;

			Class<?> event = method.getParameterTypes()[0];

			if (!Event.class.isAssignableFrom(event))
				continue;

			try {
				@SuppressWarnings("unchecked")
				HandlerList handlerList = getHandlerList((Class<? extends Event>) event);
				handlerList.register(new RegisteredListener(plugin, listener, method));
			} catch (ReflectiveOperationException exception) {
				exception.printStackTrace();
			}
		}
	}

	public void callEvent(Event event) {
		HandlerList handlerList = event.getHandlers();

		for (RegisteredListener handler : handlerList.getBakedListeners()) {
			if (event instanceof Cancellable && ((Cancellable) event).isCancelled() && handler.isIgnoreCancelled())
				continue;
			handler.getExecutor().execute(handler.getListener(), event);
		}
	}

	public JavaPlugin loadPlugin(File file) {
		try {
			PluginClassLoader loader = new PluginClassLoader(this, file, this.getClass().getClassLoader());
			PluginDescriptionFile description = loader.getDescription();

			if (description == null)
				return null;

			try {
				Class<?> clazz = loader.loadClass(description.getMain());
				JavaPlugin plugin = (JavaPlugin) clazz.getConstructor().newInstance();
				plugin.init(description, loader);
				plugins.put(description.getName(), plugin);
				return plugin;
			} catch (ClassNotFoundException | NoSuchMethodException exception) {
				Bukkit.getBetterLogger().warn("Found plugin '{}' with an invalid main class.", description.getName());
			}
		} catch (MalformedURLException | ReflectiveOperationException exception) {
			exception.printStackTrace();
		}

		return null;
	}

	public static HandlerList getHandlerList(Class<? extends Event> event) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method getHandlerList = event.getMethod("getHandlerList");
		return (HandlerList) getHandlerList.invoke(null);
	}

}
