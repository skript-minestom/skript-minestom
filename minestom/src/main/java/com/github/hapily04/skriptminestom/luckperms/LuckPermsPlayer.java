package com.github.hapily04.skriptminestom.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsPlayer extends Player {

    private final @Nullable LuckPerms luckPerms;
    private @Nullable PlayerAdapter<Player> playerAdapter;

    public LuckPermsPlayer(@Nullable LuckPerms luckPerms, @NotNull PlayerConnection connection, @NotNull GameProfile profile) {
        super(connection, profile);
        this.luckPerms = luckPerms;
		if (luckPerms != null) {
			UserManager userManager = luckPerms.getUserManager();
			UUID uuid = profile.uuid();
			if (!userManager.isLoaded(uuid)) userManager.loadUser(uuid).join(); // it's ok to block here because it's blocking async configuration
		}
    }

    private @Nullable PlayerAdapter<Player> getPlayerAdapter() {
        if (playerAdapter == null && luckPerms != null) {
            playerAdapter = luckPerms.getPlayerAdapter(Player.class);
        }
        return playerAdapter;
    }

    protected @Nullable User getLuckPermsUser() {
        PlayerAdapter<Player> adapter = getPlayerAdapter();
        return adapter == null ? null : adapter.getUser(this);
    }

    private @Nullable CachedMetaData getLuckPermsMetaData() {
        User user = getLuckPermsUser();
        return user == null ? null : user.getCachedData().getMetaData();
    }

    public @NotNull String getPrimaryGroup() {
        User user = getLuckPermsUser();
        return user == null ? "" : user.getPrimaryGroup();
    }

    public @NotNull List<String> getAllGroups() {
        User user = getLuckPermsUser();
        if (user == null || luckPerms == null) return List.of();
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
        if (user == null || luckPerms == null) return CompletableFuture.completedFuture(DataMutateResult.FAIL);
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
        if (user == null || luckPerms == null) return CompletableFuture.completedFuture(DataMutateResult.FAIL);
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
        if (user == null || luckPerms == null) return CompletableFuture.completedFuture(DataMutateResult.FAIL);
        DataMutateResult result = user.data().remove(Node.builder(permissionName).build());
        return this.luckPerms.getUserManager().saveUser(user).thenApply(ignored -> result);
    }

    /**
     * Checks if the player has a permission. {@code false} when no LuckPerms user is
     * available.
     *
     * @param permissionName the name of the permission to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(@NotNull String permissionName) {
		return this.getPermission(permissionName).asBoolean();
    }

    /**
     * Gets the value of a permission. This passes a {@link Tristate} value
     * straight from LuckPerms, which may be a better option than using
     * boolean values in some cases. {@link Tristate#UNDEFINED} when no LuckPerms user is
     * available.
     *
     * @param permissionName the name of the permission to check
     * @return the value of the permission
     */
    public @NotNull Tristate getPermission(@NotNull String permissionName) {
        User user = getLuckPermsUser();
        if (user == null) return Tristate.UNDEFINED;
        return user.getCachedData().getPermissionData().checkPermission(permissionName);
    }

    /**
     * Gets the prefix of the player. {@code ""} when no LuckPerms user is available.
     *
     * @return the prefix of the player
     */
    public @NotNull String getPrefix() {
        CachedMetaData metaData = getLuckPermsMetaData();
        String prefix = metaData == null ? null : metaData.getPrefix();
        return prefix == null ? "" : prefix;
    }

    /**
     * Gets the suffix of the player. {@code ""} when no LuckPerms user is available.
     *
     * @return the suffix of the player
     */
    public @NotNull String getSuffix() {
        CachedMetaData metaData = getLuckPermsMetaData();
        String suffix = metaData == null ? null : metaData.getSuffix();
        return suffix == null ? "" : suffix;
    }

}
