package com.example.mypermissions;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.EventBus;
import com.hypixel.hytale.server.core.event.events.permissions.GroupPermissionChangeEvent;
import com.hypixel.hytale.server.core.event.events.permissions.PlayerGroupEvent;
import com.hypixel.hytale.server.core.event.events.permissions.PlayerPermissionChangeEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Permission Event Subscription Examples
 *
 * This file demonstrates how to subscribe to all permission-related events
 * in the Hytale server. Use these patterns in your plugin to react to
 * permission changes in real-time.
 *
 * Event Hierarchy:
 *   PlayerPermissionChangeEvent (abstract)
 *   ├── PermissionsAdded      - User's direct permissions added
 *   ├── PermissionsRemoved    - User's direct permissions removed
 *   ├── GroupAdded            - User added to group (inner class)
 *   └── GroupRemoved          - User removed from group (inner class)
 *
 *   PlayerGroupEvent extends PlayerPermissionChangeEvent
 *   ├── Added                 - User added to group (standalone) [DISPATCHED BY SYSTEM]
 *   └── Removed               - User removed from group (standalone) [DISPATCHED BY SYSTEM]
 *
 *   NOTE: Both PlayerPermissionChangeEvent.GroupAdded/GroupRemoved (inner classes) AND
 *   PlayerGroupEvent.Added/Removed exist in the codebase. However, PermissionsModule
 *   dispatches PlayerGroupEvent.Added/Removed — NOT the inner class versions.
 *   Subscribe to PlayerGroupEvent.Added/Removed for group membership changes.
 *
 *   GroupPermissionChangeEvent (abstract)
 *   ├── Added                 - Permissions added to a group
 *   └── Removed               - Permissions removed from a group
 */
public class EventSubscriptionExamples extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("PermissionEvents");

    public EventSubscriptionExamples(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        EventBus eventBus = HytaleServer.get().getEventBus();

        // Subscribe to all permission events
        subscribeToUserPermissionEvents(eventBus);
        subscribeToGroupMembershipEvents(eventBus);
        subscribeToGroupPermissionEvents(eventBus);
    }

    // =========================================================================
    // USER PERMISSION EVENTS
    // =========================================================================

    private void subscribeToUserPermissionEvents(EventBus eventBus) {

        // -----------------------------------------------------------------
        // Event: User's direct permissions were added
        // Fired by: PermissionsModule.addUserPermission(uuid, permissions)
        // -----------------------------------------------------------------
        eventBus.subscribe(PlayerPermissionChangeEvent.PermissionsAdded.class, event -> {
            UUID uuid = event.getPlayerUuid();
            Set<String> added = event.getAddedPermissions();

            LOGGER.info("Permissions added to " + uuid + ": " + added);

            // Example: Notify the player if they're online
            PlayerRef player = Universe.get().getPlayer(uuid);
            if (player != null) {
                // player.sendMessage(Message.raw("You received new permissions!"));
            }

            // Example: Update cached permissions
            // permissionCache.invalidate(uuid);

            // Example: Log to audit trail
            // auditLog.record(uuid, "PERM_ADD", added);
        });

        // -----------------------------------------------------------------
        // Event: User's direct permissions were removed
        // Fired by: PermissionsModule.removeUserPermission(uuid, permissions)
        // -----------------------------------------------------------------
        eventBus.subscribe(PlayerPermissionChangeEvent.PermissionsRemoved.class, event -> {
            UUID uuid = event.getPlayerUuid();
            Set<String> removed = event.getRemovedPermissions();

            LOGGER.info("Permissions removed from " + uuid + ": " + removed);

            // Example: Force re-check of current actions
            // if (removed.contains("build.allowed")) {
            //     cancelBuildingActions(uuid);
            // }
        });
    }

    // =========================================================================
    // GROUP MEMBERSHIP EVENTS
    // =========================================================================

    private void subscribeToGroupMembershipEvents(EventBus eventBus) {

        // -----------------------------------------------------------------
        // Event: User was added to a group
        // Fired by: PermissionsModule.addUserToGroup(uuid, group)
        // -----------------------------------------------------------------
        eventBus.subscribe(PlayerGroupEvent.Added.class, event -> {
            UUID uuid = event.getPlayerUuid();
            String group = event.getGroupName();

            LOGGER.info("Player " + uuid + " added to group: " + group);

            // Example: Grant group-specific rewards
            if ("VIP".equals(group)) {
                // grantVipRewards(uuid);
            }

            // Example: Update player display name/prefix
            // updatePlayerPrefix(uuid, group);

            // Example: Special handling for OP group
            if ("OP".equals(group)) {
                LOGGER.warning("Player " + uuid + " was granted OP status!");
                // notifyAdmins(uuid, "granted OP");
            }
        });

        // -----------------------------------------------------------------
        // Event: User was removed from a group
        // Fired by: PermissionsModule.removeUserFromGroup(uuid, group)
        // -----------------------------------------------------------------
        eventBus.subscribe(PlayerGroupEvent.Removed.class, event -> {
            UUID uuid = event.getPlayerUuid();
            String group = event.getGroupName();

            LOGGER.info("Player " + uuid + " removed from group: " + group);

            // Example: Revoke group-specific items/abilities
            if ("VIP".equals(group)) {
                // revokeVipPerks(uuid);
            }

            // Example: Special handling for OP removal
            if ("OP".equals(group)) {
                LOGGER.warning("Player " + uuid + " had OP status revoked!");
                // notifyAdmins(uuid, "lost OP");
            }
        });

        // -----------------------------------------------------------------
        // Alternative: Subscribe to base PlayerGroupEvent for both add/remove
        // Note: You'll need to check the actual event type
        // -----------------------------------------------------------------
        // eventBus.subscribe(PlayerGroupEvent.class, event -> {
        //     if (event instanceof PlayerGroupEvent.Added) {
        //         // Handle addition
        //     } else if (event instanceof PlayerGroupEvent.Removed) {
        //         // Handle removal
        //     }
        // });
    }

    // =========================================================================
    // GROUP PERMISSION EVENTS
    // =========================================================================

    private void subscribeToGroupPermissionEvents(EventBus eventBus) {

        // -----------------------------------------------------------------
        // Event: Permissions were added to a group
        // Fired by: PermissionsModule.addGroupPermission(group, permissions)
        // -----------------------------------------------------------------
        eventBus.subscribe(GroupPermissionChangeEvent.Added.class, event -> {
            String group = event.getGroupName();
            Set<String> added = event.getAddedPermissions();

            LOGGER.info("Permissions added to group " + group + ": " + added);

            // Example: Invalidate cache for all users in this group
            // invalidateCacheForGroup(group);

            // Example: Notify all online users in this group
            // notifyGroupMembers(group, "Your group received new permissions!");
        });

        // -----------------------------------------------------------------
        // Event: Permissions were removed from a group
        // Fired by: PermissionsModule.removeGroupPermission(group, permissions)
        // -----------------------------------------------------------------
        eventBus.subscribe(GroupPermissionChangeEvent.Removed.class, event -> {
            String group = event.getGroupName();
            Set<String> removed = event.getRemovedPermissions();

            LOGGER.info("Permissions removed from group " + group + ": " + removed);

            // Example: Force permission re-check for affected users
            // recheckPermissionsForGroup(group, removed);
        });
    }

    // =========================================================================
    // ADVANCED: Centralized Event Handler
    // =========================================================================

    /**
     * Example of a centralized permission change handler that processes
     * all permission-related events in one place.
     */
    private void setupCentralizedHandler(EventBus eventBus) {

        // Handle all user permission changes
        eventBus.subscribe(PlayerPermissionChangeEvent.PermissionsAdded.class,
            this::handleUserPermissionChange);
        eventBus.subscribe(PlayerPermissionChangeEvent.PermissionsRemoved.class,
            this::handleUserPermissionChange);

        // Handle all group membership changes
        eventBus.subscribe(PlayerGroupEvent.Added.class,
            this::handleGroupMembershipChange);
        eventBus.subscribe(PlayerGroupEvent.Removed.class,
            this::handleGroupMembershipChange);

        // Handle all group permission changes
        eventBus.subscribe(GroupPermissionChangeEvent.Added.class,
            this::handleGroupPermissionChange);
        eventBus.subscribe(GroupPermissionChangeEvent.Removed.class,
            this::handleGroupPermissionChange);
    }

    private void handleUserPermissionChange(PlayerPermissionChangeEvent event) {
        UUID uuid = event.getPlayerUuid();

        // Invalidate any caches
        // permissionCache.invalidate(uuid);

        // Log for audit
        if (event instanceof PlayerPermissionChangeEvent.PermissionsAdded added) {
            LOGGER.info("AUDIT: User " + uuid + " +perms: " + added.getAddedPermissions());
        } else if (event instanceof PlayerPermissionChangeEvent.PermissionsRemoved removed) {
            LOGGER.info("AUDIT: User " + uuid + " -perms: " + removed.getRemovedPermissions());
        }
    }

    private void handleGroupMembershipChange(PlayerGroupEvent event) {
        UUID uuid = event.getPlayerUuid();
        String group = event.getGroupName();
        boolean added = event instanceof PlayerGroupEvent.Added;

        LOGGER.info("AUDIT: User " + uuid + (added ? " joined " : " left ") + group);

        // Update any group-based features
        // updateGroupFeatures(uuid, group, added);
    }

    private void handleGroupPermissionChange(GroupPermissionChangeEvent event) {
        String group = event.getGroupName();

        // Invalidate cache for all users in this group
        // This might require iterating through all users
        // permissionCache.invalidateGroup(group);

        if (event instanceof GroupPermissionChangeEvent.Added added) {
            LOGGER.info("AUDIT: Group " + group + " +perms: " + added.getAddedPermissions());
        } else if (event instanceof GroupPermissionChangeEvent.Removed removed) {
            LOGGER.info("AUDIT: Group " + group + " -perms: " + removed.getRemovedPermissions());
        }
    }

    // =========================================================================
    // ANTI-PATTERNS: What NOT to do
    // =========================================================================

    /**
     * WARNING: Do NOT modify permissions in response to permission events!
     * This can cause infinite loops.
     */
    private void badExample(EventBus eventBus) {
        // DON'T DO THIS - Infinite loop risk!
        // eventBus.subscribe(PlayerGroupEvent.Added.class, event -> {
        //     if ("VIP".equals(event.getGroupName())) {
        //         // This will fire another event, which fires another, etc.
        //         PermissionsModule.get().addUserPermission(
        //             event.getPlayerUuid(),
        //             Set.of("vip.bonus")
        //         );
        //     }
        // });

        // INSTEAD: Use the provider directly or queue for later
        // eventBus.subscribe(PlayerGroupEvent.Added.class, event -> {
        //     if ("VIP".equals(event.getGroupName())) {
        //         // Queue for next tick or handle in provider
        //         scheduler.runLater(() -> {
        //             grantVipBonus(event.getPlayerUuid());
        //         });
        //     }
        // });
    }
}
