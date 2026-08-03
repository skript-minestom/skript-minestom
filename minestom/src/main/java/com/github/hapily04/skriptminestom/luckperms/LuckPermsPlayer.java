package com.github.hapily04.skriptminestom.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class LuckPermsPlayer extends Player implements Permissible {

    private final @NotNull LuckPerms luckPerms;
    private final @NonNull PlayerAdapter<Player> playerAdapter;
    private boolean op;

    public LuckPermsPlayer(@NotNull LuckPerms luckPerms, @NotNull PlayerConnection connection, @NotNull GameProfile profile) {
        super(connection, profile);
        this.luckPerms = luckPerms;
        this.playerAdapter = this.luckPerms.getPlayerAdapter(Player.class);
    }

    private @NotNull User getLuckPermsUser() {
        return this.playerAdapter.getUser(this);
    }

    private @NotNull CachedMetaData getLuckPermsMetaData() {
        return this.getLuckPermsUser().getCachedData().getMetaData();
    }

    public @NotNull String getPrimaryGroup() {
        return getLuckPermsUser().getPrimaryGroup();
    }

    public @NotNull List<String> getAllGroups() {
        User user = getLuckPermsUser();
        Collection<InheritanceNode> nodes = user.getNodes(NodeType.INHERITANCE);
        List<String> groups = new ArrayList<>(nodes.size());
        for (InheritanceNode node : nodes) {
            String groupName = node.getGroupName();
            if (luckPerms.getGroupManager().getGroup(groupName) == null) continue; // group not found don't include it
            groups.add(groupName);
        }
        return groups;
    }

    public boolean isInGroup(String groupName) {
        return getAllGroups().contains(groupName);
    }

    /**
     * Adds a permission to the player. You may choose not to implement
     * this method on a production server, and leave permission management
     * to the LuckPerms web interface or in-game commands.
     *
     * @param permission the permission to add
     * @return the result of the operation
     */
    public @NotNull CompletableFuture<DataMutateResult> addPermission(@NotNull String permission) {
        User user = getLuckPermsUser();
        DataMutateResult result = user.data().add(Node.builder(permission).build());
        return this.luckPerms.getUserManager().saveUser(user).thenApply(ignored -> result);
    }

    /**
     * Sets a permission for the player. This method uses a {@link Node} rather
     * than a permission name, this allows for permissions that rely on context.
     * You may choose not to implement this method on a production server, and
     * leave permission management to the LuckPerms web interface or in-game
     * commands.
     *
     * @param permission the permission to set
     * @param value the value of the permission
     * @return the result of the operation
     */
    public @NotNull CompletableFuture<DataMutateResult> setPermission(@NotNull Node permission, boolean value) {
        User user = getLuckPermsUser();
        DataMutateResult result = value
                ? user.data().add(permission)
                : user.data().remove(permission);
        return this.luckPerms.getUserManager().saveUser(user).thenApply(ignored -> result);
    }

    /**
     * Removes a permission from the player. You may choose not to implement
     * this method on a production server, and leave permission management
     * to the LuckPerms web interface or in-game commands.
     *
     * @param permissionName the name of the permission to remove
     */
    public @NotNull CompletableFuture<DataMutateResult> removePermission(@NotNull String permissionName) {
        User user = getLuckPermsUser();
        DataMutateResult result = user.data().remove(Node.builder(permissionName).build());
        return this.luckPerms.getUserManager().saveUser(user).thenApply(ignored -> result);
    }

    /**
     * Checks if the player has a permission.
     *
     * @param permissionName the name of the permission to check
     * @return true if the player has the permission
     */
    @Override
    public boolean hasPermission(@NotNull String permissionName) {
		return this.getPermission(permissionName).asBoolean();
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return getPermission(name) != Tristate.UNDEFINED;
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return false;
    }

    @Override
    public boolean isOp() {
        return op;
    }

    @Override
    public void setOp(boolean value) {
        this.op = value;
    }

    /**
     * Gets the value of a permission. This passes a {@link Tristate} value
     * straight from LuckPerms, which may be a better option than using
     * boolean values in some cases.
     *
     * @param permissionName the name of the permission to check
     * @return the value of the permission
     */
    public @NotNull Tristate getPermission(@NotNull String permissionName) {
        User user = getLuckPermsUser();
        return user.getCachedData().getPermissionData().checkPermission(permissionName);
    }

    /**
     * Gets the prefix of the player.
     *
     * @return the prefix of the player
     */
    public @NotNull String getPrefix() {
        String prefix = getLuckPermsMetaData().getPrefix();
        return prefix == null ? "" : prefix;
    }

    /**
     * Gets the suffix of the player.
     *
     * @return the suffix of the player
     */
    public @NotNull String getSuffix() {
        String suffix = getLuckPermsMetaData().getSuffix();
        return suffix == null ? "" : suffix;
    }

    public static boolean hasPermission(CommandSender sender, String permissionNode) {
        return sender instanceof ConsoleSender || (sender instanceof Permissible p && p.hasPermission(permissionNode));
    }

}
