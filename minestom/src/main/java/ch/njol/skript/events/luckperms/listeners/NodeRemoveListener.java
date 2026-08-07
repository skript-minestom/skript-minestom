package ch.njol.skript.events.luckperms.listeners;

import ch.njol.skript.Skript;
import ch.njol.skript.events.luckperms.GroupRemoveEvent;
import ch.njol.skript.events.luckperms.PermissionRemoveEvent;
import ch.njol.skript.events.luckperms.PrefixRemoveEvent;
import ch.njol.skript.events.luckperms.SuffixRemoveEvent;
import com.github.hapily04.skriptminestom.SkriptMinestom;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public class NodeRemoveListener {

	static {
		LuckPerms luckPerms = SkriptMinestom.getLuckPerms();
		if (luckPerms != null) luckPerms.getEventBus().subscribe(Skript.getInstance(), NodeRemoveEvent.class, e -> {
			if (!e.isUser()) {
				return;
			}

			User target = (User) e.getTarget();
			Node node = e.getNode();

			Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(target.getUniqueId());
			if (player == null) return;

			Event event = switch (node) {
				case PermissionNode permissionNode -> new PermissionRemoveEvent(player, permissionNode.getPermission());
				case InheritanceNode inheritanceNode -> new GroupRemoveEvent(player, inheritanceNode.getGroupName());
				case PrefixNode prefixNode -> new PrefixRemoveEvent(player, prefixNode.getMetaValue());
				case SuffixNode suffixNode -> new SuffixRemoveEvent(player, suffixNode.getMetaValue());
				default -> null;
			};

			if (event == null) return;
			Bukkit.getPluginManager().callEvent(event);
		});
	}

}
