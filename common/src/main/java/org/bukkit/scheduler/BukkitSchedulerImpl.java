
package org.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class BukkitSchedulerImpl implements BukkitScheduler {
	// concurrenthashmap fixes concurrentmodificationexception from having a lot of events run at once
	private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
	private final Queue<Task> pendingTasks = new ConcurrentLinkedQueue<>();
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private Task currentTask = null;

	public BukkitSchedulerImpl() {
		Ticker ticker = Bukkit.getTicker();
		ticker.initialize(this::tick);
	}

	public void tick() {
		Task pending;
		while ((pending = pendingTasks.poll()) != null) {
			tasks.put(pending.id, pending);
		}

		for (int taskID : tasks.keySet()) {
			Task task = tasks.get(taskID);
			if (task == null) continue; // Task was removed mid-tick
			if (task.isCancelled()) {
				tasks.remove(taskID);
				continue;
			}
			task.ticksLeft--;

			if (task.ticksLeft > 0) continue;
			currentTask = task;
			try {
				if (task.async) threadPool.submit(task.runnable);
				else task.runnable.run();
			} catch (Throwable t) {
				Bukkit.getLogger().log(java.util.logging.Level.SEVERE,
					"Task " + taskID + " from " + task.getOwner().getName() + " threw an exception", t);
			}

			currentTask = null;
			task.ticksLeft = task.isRepeating() ? task.duration : task.delay;

			if (!task.isRepeating()) tasks.remove(taskID);
		}
	}

	@Override
	public BukkitTask runTask(Plugin plugin, BukkitRunnable task) {
		return scheduleTask(plugin, task, false, 0, null);
	}

	@Override
	public void runTask(Plugin plugin, Consumer<? super BukkitTask> task) {
		var bukkitTask = new BukkitTask() {
			private final Runnable runnable = () -> task.accept(this);
			public final Task delegate = new Task(plugin, runnable, false, 0L, null);
			@Override
			public int getTaskId() {
				return delegate.getTaskId();
			}

			@Override
			public @NotNull Plugin getOwner() {
				return plugin;
			}

			@Override
			public boolean isSync() {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return delegate.isCancelled();
			}

			@Override
			public void cancel() {
				delegate.cancel();
			}
		};
		tasks.put(bukkitTask.getTaskId(), bukkitTask.delegate);
	}

	@Override
	public BukkitTask runTask(Plugin plugin, Runnable task) {
		return scheduleTask(plugin, task, false, 0, null);
	}

	@Override
	public BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay) {
		return scheduleTask(plugin, task, false, delay, null);
	}

	@Override
	public void runTaskLater(Plugin plugin, Consumer<? super BukkitTask> task, long delay) {
		var bukkitTask = new BukkitTask() {
			private final Runnable runnable = () -> task.accept(this);
			public final Task delegate = new Task(plugin, runnable, false, delay, null);
			@Override
			public int getTaskId() {
				return delegate.getTaskId();
			}

			@Override
			public @NotNull Plugin getOwner() {
				return plugin;
			}

			@Override
			public boolean isSync() {
				return false;
			}

			@Override
			public boolean isCancelled() {
				return delegate.isCancelled();
			}

			@Override
			public void cancel() {
				delegate.cancel();
			}
		};
		tasks.put(bukkitTask.getTaskId(), bukkitTask.delegate);
	}

	@Override
	public BukkitTask runTaskLater(Plugin plugin, BukkitRunnable task, long delay) {
		return scheduleTask(plugin, task, false, delay, null);
	}

	@Override
	public BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
		return scheduleTask(plugin, task, false, delay, period);
	}

	@Override
	public BukkitTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
		return runTaskLaterAsynchronously(plugin, runnable, 0);
	}

	public BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay) {
		return scheduleTask(plugin, runnable, true, delay, null);
	}

	public int scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay) {
		return scheduleTask(plugin, runnable, false, delay, null).getTaskId();
	}

	public BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long duration) {
		return scheduleTask(plugin, runnable, true, delay, duration);
	}

	public int scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long duration) {
		return scheduleTask(plugin, runnable, false, delay, duration).getTaskId();
	}

	public boolean isQueued(int taskID) {
		return tasks.containsKey(taskID);
	}

	public boolean isCurrentlyRunning(int taskID) {
		Task task = tasks.get(taskID);
		if (task == null)
			return false;

		return task.isRepeating() || (!task.async && currentTask == task);
	}

	public void cancelTask(int taskID) {
		Task task = tasks.remove(taskID);
		if (task == null) {
			for (Task pending : pendingTasks) {
				if (pending.id != taskID) continue;
				pendingTasks.remove(pending);
				task = pending;
				break;
			}
		}
		if (task != null) task.setCancelled(true);
	}

	public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
		FutureTask<T> future = new FutureTask<>(task);
		scheduleTask(plugin, future, false, 0L, null);
		return future;
	}

	private BukkitTask scheduleTask(Plugin owner, Runnable runnable, boolean async, long delay, @Nullable Long duration) {
		Task task = new Task(owner, runnable, async, delay, duration);
		pendingTasks.add(task);
		return task;
	}
}
