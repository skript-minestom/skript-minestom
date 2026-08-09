package com.github.hapily04.skriptminestom.luckperms;

import com.github.hapily04.skriptminestom.SkriptMinestom;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class LuckPermsLookup {

    private static LuckPerms adapterSource;
    private static PlayerAdapter<Player> playerAdapter;

    private LuckPermsLookup() {
    }

    private static @Nullable PlayerAdapter<Player> getPlayerAdapter() {
        LuckPerms luckPerms = SkriptMinestom.getLuckPerms();
        if (luckPerms == null) return null;
        if (playerAdapter == null || adapterSource != luckPerms) {
            playerAdapter = luckPerms.getPlayerAdapter(Player.class);
            adapterSource = luckPerms;
        }
        return playerAdapter;
    }

    public static @Nullable User getUser(@NotNull Player player) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getLuckPermsUser();
        PlayerAdapter<Player> adapter = getPlayerAdapter();
        return adapter == null ? null : adapter.getUser(player);
    }

    private static @Nullable CachedMetaData getMetaData(@NotNull Player player) {
        User user = getUser(player);
        return user == null ? null : user.getCachedData().getMetaData();
    }

    public static @NotNull Tristate getPermission(@NotNull Player player, @NotNull String permissionName) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getPermission(permissionName);
        User user = getUser(player);
        if (user == null) return Tristate.UNDEFINED;
        return user.getCachedData().getPermissionData().checkPermission(permissionName);
    }

    public static boolean hasPermission(@NotNull Player player, @NotNull String permissionName) {
        return getPermission(player, permissionName).asBoolean();
    }

    public static boolean hasPermission(@NotNull CommandSender sender, @NotNull String permissionName) {
        return sender instanceof ConsoleSender || (sender instanceof Player player && hasPermission(player, permissionName));
    }

    public static @NotNull String getPrimaryGroup(@NotNull Player player) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getPrimaryGroup();
        User user = getUser(player);
        return user == null ? "" : user.getPrimaryGroup();
    }

    public static @NotNull List<String> getAllGroups(@NotNull Player player) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getAllGroups();
        LuckPerms luckPerms = SkriptMinestom.getLuckPerms();
        User user = getUser(player);
        if (user == null || luckPerms == null) return List.of();
        Collection<InheritanceNode> nodes = user.getNodes(NodeType.INHERITANCE);
        List<String> groups = new ArrayList<>(nodes.size());
        for (InheritanceNode node : nodes) {
            String groupName = node.getGroupName();
            if (luckPerms.getGroupManager().getGroup(groupName) == null) continue;
            groups.add(groupName);
        }
        return groups;
    }

    public static @NotNull String getPrefix(@NotNull Player player) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getPrefix();
        CachedMetaData metaData = getMetaData(player);
        String prefix = metaData == null ? null : metaData.getPrefix();
        return prefix == null ? "" : prefix;
    }

    public static @NotNull String getSuffix(@NotNull Player player) {
        if (player instanceof LuckPermsPlayer luckPermsPlayer) return luckPermsPlayer.getSuffix();
        CachedMetaData metaData = getMetaData(player);
        String suffix = metaData == null ? null : metaData.getSuffix();
        return suffix == null ? "" : suffix;
    }

}
