package com.example.mypermissions;

import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Permission Provider Template
 *
 * This template provides a starting point for implementing a custom permission provider.
 * Replace the in-memory storage with your preferred backend (database, config, etc.).
 *
 * Usage:
 *   PermissionsModule.get().addProvider(new MyPermissionProvider());
 *
 * Important considerations:
 *   - All methods must be thread-safe (multiple threads may call simultaneously)
 *   - Return empty sets, never null
 *   - The first registered provider receives all write operations
 *   - Permission checks iterate through ALL providers
 */
public class CustomProviderTemplate implements PermissionProvider {

    // =========================================================================
    // STORAGE - Replace with your backend (database, config file, etc.)
    // =========================================================================

    // User UUID -> Set of permission nodes
    private final Map<UUID, Set<String>> userPermissions = new ConcurrentHashMap<>();

    // Group name -> Set of permission nodes
    private final Map<String, Set<String>> groupPermissions = new ConcurrentHashMap<>();

    // User UUID -> Set of group names
    private final Map<UUID, Set<String>> userGroups = new ConcurrentHashMap<>();

    // =========================================================================
    // IDENTITY
    // =========================================================================

    /**
     * Returns the unique name of this provider.
     * Used in command output (e.g., /perm user list) to identify which provider
     * returned which permissions.
     */
    @Nonnull
    @Override
    public String getName() {
        return "CustomProviderTemplate";
    }

    // =========================================================================
    // USER PERMISSIONS
    // =========================================================================

    /**
     * Add permissions directly to a user.
     *
     * @param uuid        The user's UUID
     * @param permissions Set of permission nodes to add (e.g., "my.perm", "other.perm")
     */
    @Override
    public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        userPermissions.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                       .addAll(permissions);

        // TODO: Persist to your backend
        // saveToDatabase(uuid, permissions);
    }

    /**
     * Remove permissions from a user.
     *
     * @param uuid        The user's UUID
     * @param permissions Set of permission nodes to remove
     */
    @Override
    public void removeUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        Set<String> userPerms = userPermissions.get(uuid);
        if (userPerms != null) {
            userPerms.removeAll(permissions);
            if (userPerms.isEmpty()) {
                userPermissions.remove(uuid);
            }
        }

        // TODO: Persist to your backend
        // removeFromDatabase(uuid, permissions);
    }

    /**
     * Get all direct permissions for a user.
     * Does NOT include permissions inherited from groups.
     *
     * @param uuid The user's UUID
     * @return Unmodifiable set of permission nodes (never null)
     */
    @Nonnull
    @Override
    public Set<String> getUserPermissions(@Nonnull UUID uuid) {
        Set<String> perms = userPermissions.get(uuid);
        if (perms != null) {
            return Collections.unmodifiableSet(perms);
        }
        return Collections.emptySet();
    }

    // =========================================================================
    // GROUP PERMISSIONS
    // =========================================================================

    /**
     * Add permissions to a group.
     * All users in this group will inherit these permissions.
     *
     * @param group       The group name (e.g., "VIP", "Moderator")
     * @param permissions Set of permission nodes to add
     */
    @Override
    public void addGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        groupPermissions.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet())
                        .addAll(permissions);

        // TODO: Persist to your backend
    }

    /**
     * Remove permissions from a group.
     *
     * @param group       The group name
     * @param permissions Set of permission nodes to remove
     */
    @Override
    public void removeGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        Set<String> groupPerms = groupPermissions.get(group);
        if (groupPerms != null) {
            groupPerms.removeAll(permissions);
            if (groupPerms.isEmpty()) {
                groupPermissions.remove(group);
            }
        }

        // TODO: Persist to your backend
    }

    /**
     * Get all permissions for a group.
     *
     * @param group The group name
     * @return Unmodifiable set of permission nodes (never null)
     */
    @Nonnull
    @Override
    public Set<String> getGroupPermissions(@Nonnull String group) {
        Set<String> perms = groupPermissions.get(group);
        if (perms != null) {
            return Collections.unmodifiableSet(perms);
        }
        return Collections.emptySet();
    }

    // =========================================================================
    // USER-GROUP MEMBERSHIP
    // =========================================================================

    /**
     * Add a user to a group.
     * The user will inherit all permissions from that group.
     *
     * @param uuid  The user's UUID
     * @param group The group name to add the user to
     */
    @Override
    public void addUserToGroup(@Nonnull UUID uuid, @Nonnull String group) {
        userGroups.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet())
                  .add(group);

        // TODO: Persist to your backend
    }

    /**
     * Remove a user from a group.
     *
     * @param uuid  The user's UUID
     * @param group The group name to remove the user from
     */
    @Override
    public void removeUserFromGroup(@Nonnull UUID uuid, @Nonnull String group) {
        Set<String> groups = userGroups.get(uuid);
        if (groups != null) {
            groups.remove(group);
            if (groups.isEmpty()) {
                userGroups.remove(uuid);
            }
        }

        // TODO: Persist to your backend
    }

    /**
     * Get all groups a user belongs to.
     *
     * Note: The vanilla provider returns ["Default"] for users without explicit groups.
     * You may want to replicate this behavior or handle defaults differently.
     *
     * @param uuid The user's UUID
     * @return Unmodifiable set of group names (never null)
     */
    @Nonnull
    @Override
    public Set<String> getGroupsForUser(@Nonnull UUID uuid) {
        Set<String> groups = userGroups.get(uuid);
        if (groups != null && !groups.isEmpty()) {
            return Collections.unmodifiableSet(groups);
        }

        // Option 1: Return empty (let other providers handle defaults)
        return Collections.emptySet();

        // Option 2: Return default group (like vanilla)
        // return Set.of("Default");
    }

    // =========================================================================
    // OPTIONAL: Additional utility methods for your implementation
    // =========================================================================

    /**
     * Check if a user has a specific permission (convenience method).
     * Note: This bypasses the PermissionsModule resolution algorithm.
     * For full resolution, use PermissionsModule.get().hasPermission() instead.
     */
    public boolean hasDirectPermission(UUID uuid, String permission) {
        return getUserPermissions(uuid).contains(permission);
    }

    /**
     * Get all effective permissions for a user (direct + inherited from groups).
     * Note: This is a convenience method; PermissionsModule handles resolution.
     */
    public Set<String> getEffectivePermissions(UUID uuid) {
        Set<String> effective = new HashSet<>(getUserPermissions(uuid));
        for (String group : getGroupsForUser(uuid)) {
            effective.addAll(getGroupPermissions(group));
        }
        return Collections.unmodifiableSet(effective);
    }

    /**
     * Clear all data (useful for testing or reset functionality).
     */
    public void clearAll() {
        userPermissions.clear();
        groupPermissions.clear();
        userGroups.clear();
    }

    // =========================================================================
    // OPTIONAL: Lifecycle methods (if extending JavaPlugin)
    // =========================================================================

    /**
     * Called when the provider should load data from backend.
     * Implement this if you need async loading from a database.
     */
    public void load() {
        // TODO: Load from database/config
        // userPermissions.putAll(loadUserPermissionsFromDb());
        // groupPermissions.putAll(loadGroupPermissionsFromDb());
        // userGroups.putAll(loadUserGroupsFromDb());
    }

    /**
     * Called when the provider should save all data to backend.
     * Implement this for graceful shutdown.
     */
    public void save() {
        // TODO: Save to database/config
        // saveAllToDatabase();
    }
}
