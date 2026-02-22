# Hytale Permissions System - Complete Documentation

> **Source:** Vineflower-decompiled Hytale server sources
> **Version:** Based on current Hytale server implementation
> **Purpose:** Definitive reference for HyperPerms and other permissions plugins

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Core Interfaces & Classes](#3-core-interfaces--classes)
4. [Permission Resolution Algorithm](#4-permission-resolution-algorithm)
5. [Default Provider: HytalePermissionsProvider](#5-default-provider-hytalepermissionsprovider)
6. [Built-in Permission Nodes](#6-built-in-permission-nodes)
7. [Events System](#7-events-system)
8. [Commands Reference](#8-commands-reference)
9. [Exception Handling](#9-exception-handling)
10. [Plugin Developer Guide](#10-plugin-developer-guide)
11. [Storage Format (permissions.json)](#11-storage-format-permissionsjson)
12. [HyperPerms Compatibility Notes](#12-hyperperms-compatibility-notes)

---

## 1. Executive Summary

The Hytale permissions system is a flexible, provider-based architecture for managing player and group permissions. Key characteristics:

- **Provider Chain:** Multiple permission providers can be registered; permissions are checked across all providers
- **Hierarchical Resolution:** User permissions → Group permissions → Virtual groups → Default value
- **Wildcard Support:** Full wildcard (`*`) and prefix wildcards (`prefix.*`), with negation support (`-*`, `-prefix.*`)
- **Event-Driven:** All permission changes fire events for reactive plugin development
- **Thread-Safe:** ReadWriteLock-based concurrency in the default provider
- **File-Based Default:** JSON storage via `permissions.json`

### Quick Start for Plugin Developers

```java
// Check permission
boolean hasPerm = PermissionsModule.get().hasPermission(playerUuid, "my.permission");

// Or via PermissionHolder
player.hasPermission("my.permission");

// Register custom provider
PermissionsModule.get().addProvider(myCustomProvider);
```

---

## 2. Architecture Overview

### System Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PermissionsModule (Core)                        │
│                                                                         │
│  ┌─────────────────┐    ┌─────────────────────────────────────────┐    │
│  │ Singleton Access│    │           Provider Chain                 │    │
│  │   .get()        │    │  ┌──────────────────────────────────┐   │    │
│  └─────────────────┘    │  │ HytalePermissionsProvider (0)    │   │    │
│                         │  │ (Default, file-based)            │   │    │
│                         │  └──────────────────────────────────┘   │    │
│                         │  ┌──────────────────────────────────┐   │    │
│                         │  │ CustomProvider (1)               │   │    │
│                         │  │ (e.g., HyperPerms, DB-backed)    │   │    │
│                         │  └──────────────────────────────────┘   │    │
│                         └─────────────────────────────────────────┘    │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      Virtual Groups                              │   │
│  │   e.g., "Creative" -> ["hytale.editor.builderTools"]             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      Event Dispatching                           │   │
│  │   PlayerPermissionChangeEvent, GroupPermissionChangeEvent, etc.  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘

            │                           │
            ▼                           ▼
┌───────────────────────┐    ┌─────────────────────────┐
│   PermissionHolder    │    │   Commands              │
│   (Players, Senders)  │    │   /op, /perm            │
└───────────────────────┘    └─────────────────────────┘
```

### Component Relationships

| Component | Role | Dependencies |
|-----------|------|--------------|
| `PermissionsModule` | Central coordinator, singleton access | `HytaleServer`, `EventBus` |
| `PermissionProvider` | Interface for permission storage/retrieval | None (interface) |
| `HytalePermissionsProvider` | Default file-based implementation | `BlockingDiskFile` |
| `PermissionHolder` | Interface for entities that can have permissions | None (interface) |
| Commands (`/op`, `/perm`) | User-facing permission management | `PermissionsModule` |
| Events | Reactive permission change notifications | `EventBus` |

---

## 3. Core Interfaces & Classes

### 3.1 PermissionHolder Interface

**Package:** `com.hypixel.hytale.server.core.permissions`

The base interface for any entity that can hold permissions (players, command senders).

```java
public interface PermissionHolder {
    boolean hasPermission(@Nonnull String permission);
    boolean hasPermission(@Nonnull String permission, boolean defaultValue);
}
```

| Method | Description |
|--------|-------------|
| `hasPermission(String)` | Check if holder has permission; defaults to `false` |
| `hasPermission(String, boolean)` | Check permission with custom default value |

**Implementers:**
- `PlayerRef` (online players)
- `CommandSender` (command execution context)

---

### 3.2 PermissionProvider Interface

**Package:** `com.hypixel.hytale.server.core.permissions.provider`

The heart of the provider system. Any custom permission system must implement this interface.

```java
public interface PermissionProvider {
    // Identity
    @Nonnull String getName();

    // User Permissions
    void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions);
    void removeUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions);
    Set<String> getUserPermissions(@Nonnull UUID uuid);

    // Group Permissions
    void addGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions);
    void removeGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions);
    Set<String> getGroupPermissions(@Nonnull String group);

    // User-Group Assignments
    void addUserToGroup(@Nonnull UUID uuid, @Nonnull String group);
    void removeUserFromGroup(@Nonnull UUID uuid, @Nonnull String group);
    Set<String> getGroupsForUser(@Nonnull UUID uuid);
}
```

#### Method Reference

| Category | Method | Description |
|----------|--------|-------------|
| **Identity** | `getName()` | Returns provider name (e.g., "HytalePermissionsProvider") |
| **User Perms** | `addUserPermissions(uuid, perms)` | Add permissions directly to a user |
| | `removeUserPermissions(uuid, perms)` | Remove permissions from a user |
| | `getUserPermissions(uuid)` | Get all direct permissions for a user |
| **Group Perms** | `addGroupPermissions(group, perms)` | Add permissions to a group |
| | `removeGroupPermissions(group, perms)` | Remove permissions from a group |
| | `getGroupPermissions(group)` | Get all permissions for a group |
| **Membership** | `addUserToGroup(uuid, group)` | Assign user to a group |
| | `removeUserFromGroup(uuid, group)` | Remove user from a group |
| | `getGroupsForUser(uuid)` | Get all groups a user belongs to |

---

### 3.3 PermissionsModule (Core Plugin)

**Package:** `com.hypixel.hytale.server.core.permissions`

The central coordinator for the entire permissions system. Registered as a core plugin.

```java
public class PermissionsModule extends JavaPlugin {
    // Singleton access
    public static PermissionsModule get();

    // Provider management
    public void addProvider(@Nonnull PermissionProvider provider);
    public void removeProvider(@Nonnull PermissionProvider provider);
    public List<PermissionProvider> getProviders();
    public PermissionProvider getFirstPermissionProvider();
    public boolean areProvidersTampered();

    // Permission operations (delegates to first provider + fires events)
    public void addUserPermission(@Nonnull UUID uuid, @Nonnull Set<String> permissions);
    public void removeUserPermission(@Nonnull UUID uuid, @Nonnull Set<String> permissions);
    public void addGroupPermission(@Nonnull String group, @Nonnull Set<String> permissions);
    public void removeGroupPermission(@Nonnull String group, @Nonnull Set<String> permissions);
    public void addUserToGroup(@Nonnull UUID uuid, @Nonnull String group);
    public void removeUserFromGroup(@Nonnull UUID uuid, @Nonnull String group);

    // Virtual groups
    public void setVirtualGroups(@Nonnull Map<String, Set<String>> virtualGroups);

    // Permission checking
    public Set<String> getGroupsForUser(@Nonnull UUID uuid);
    public boolean hasPermission(@Nonnull UUID uuid, @Nonnull String id);
    public boolean hasPermission(@Nonnull UUID uuid, @Nonnull String id, boolean defaultValue);

    // Static utility
    public static Boolean hasPermission(@Nullable Set<String> nodes, @Nonnull String id);
}
```

#### Key Behaviors

1. **Singleton Pattern:** Access via `PermissionsModule.get()`
2. **Provider Chain:** Providers stored in `CopyOnWriteArrayList` for thread-safety
3. **First Provider Writes:** All modification operations use `getFirstPermissionProvider()`
4. **Event Dispatching:** All changes fire corresponding events
5. **Virtual Groups:** Game-mode-based permission groups (e.g., Creative mode grants builder tools)

#### Provider Tamper Detection

```java
public boolean areProvidersTampered() {
    return this.providers.size() != 1 || this.providers.getFirst() != this.standardProvider;
}
```

Used by `/op self` to detect if custom permission providers are installed.

---

## 4. Permission Resolution Algorithm

The core logic resides in `PermissionsModule.hasPermission()`. Understanding this algorithm is critical for plugin developers.

### Algorithm (Pseudocode)

```
function hasPermission(uuid, permissionId, defaultValue):
    FOR EACH provider IN providers:

        // Step 1: Check user's direct permissions
        userNodes = provider.getUserPermissions(uuid)
        result = checkNodes(userNodes, permissionId)
        IF result IS NOT NULL:
            RETURN result

        // Step 2: Check each group the user belongs to
        FOR EACH group IN provider.getGroupsForUser(uuid):

            // Step 2a: Check group's permissions
            groupNodes = provider.getGroupPermissions(group)
            result = checkNodes(groupNodes, permissionId)
            IF result IS NOT NULL:
                RETURN result

            // Step 2b: Check virtual group permissions
            virtualNodes = virtualGroups.get(group)
            result = checkNodes(virtualNodes, permissionId)
            IF result IS NOT NULL:
                RETURN result

    RETURN defaultValue
```

### Node Checking Logic (`hasPermission(Set<String> nodes, String id)`)

```
function checkNodes(nodes, id):
    IF nodes IS NULL:
        RETURN NULL

    // Global wildcards (highest priority)
    IF nodes.contains("*"):
        RETURN TRUE
    IF nodes.contains("-*"):
        RETURN FALSE

    // Exact match
    IF nodes.contains(id):
        RETURN TRUE
    IF nodes.contains("-" + id):
        RETURN FALSE

    // Prefix wildcards (hierarchical)
    // For permission "a.b.c", check: "a.*", "a.b.*"
    parts = id.split("\\.")
    trace = ""
    FOR i = 0 TO parts.length - 1:
        IF i > 0:
            trace += "."
        trace += parts[i]

        IF nodes.contains(trace + ".*"):
            RETURN TRUE
        IF nodes.contains("-" + trace + ".*"):
            RETURN FALSE

    RETURN NULL  // No match found
```

### Wildcard Patterns

| Pattern | Meaning | Example |
|---------|---------|---------|
| `*` | Grant all permissions | User with `*` has every permission |
| `-*` | Deny all permissions | Blocks everything |
| `prefix.*` | Grant all under prefix | `hytale.command.*` grants all command perms |
| `-prefix.*` | Deny all under prefix | `-hytale.editor.*` blocks editor perms |
| `exact.perm` | Exact permission | `hytale.command.op.add` |
| `-exact.perm` | Exact denial | `-hytale.command.gamemode` |

### Resolution Order

```
1. Provider[0] User Direct Permissions
2. Provider[0] Group[0] Permissions
3. Provider[0] Group[0] Virtual Permissions
4. Provider[0] Group[1] Permissions
5. Provider[0] Group[1] Virtual Permissions
   ...
6. Provider[1] User Direct Permissions
7. Provider[1] Group[0] Permissions
   ...
N. Default value (false if not specified)
```

> **Note: Multi-Provider Group Aggregation**
>
> `PermissionsModule.getGroupsForUser()` aggregates non-empty group sets from ALL providers. If Provider[0] returns `["Default"]` (user has no explicit groups) and Provider[1] returns `["VIP"]`, the combined result is `["Default", "VIP"]`. This means a user can end up in the "Default" group even with explicit groups in another provider, if at least one provider has no data for that user and falls back to `["Default"]`.
>
> *Source: `PermissionsModule.java:142-157`*

**Important:** First definitive match wins. Once a permission is granted or denied, no further checking occurs.

> **WARNING: Nondeterministic Group Iteration Order**
>
> The `getGroupsForUser()` return value is backed by a `HashSet`, whose iteration order is undefined. If a user belongs to multiple groups with conflicting permissions (e.g., Group A grants `build.enabled` while Group B has `-build.enabled`), the result is **nondeterministic** — it depends on which group happens to be iterated first. To avoid this: ensure groups don't have conflicting permissions, or use explicit user-level permissions for overrides.
>
> *Source: `PermissionsModule.java:171` — iterates `permissionProvider.getGroupsForUser(uuid)`*

---

## 5. Default Provider: HytalePermissionsProvider

**Package:** `com.hypixel.hytale.server.core.permissions.provider`

The built-in, file-based permission provider that ships with Hytale.

### Key Characteristics

| Property | Value |
|----------|-------|
| **Name** | `"HytalePermissionsProvider"` |
| **Storage File** | `permissions.json` |
| **Thread Safety** | `ReadWriteLock` (read/write lock pattern) |
| **Base Class** | `BlockingDiskFile` (handles file I/O) |

### Default Groups

```java
public static final Map<String, Set<String>> DEFAULT_GROUPS = Map.ofEntries(
    Map.entry("OP", Set.of("*")),      // Full permissions
    Map.entry("Default", Set.of())      // Empty set
);
```

| Group | Permissions | Purpose |
|-------|-------------|---------|
| `OP` | `["*"]` | Full operator access |
| `Default` | `[]` | Base group for new users |

### Default Group Assignment

```java
public Set<String> getGroupsForUser(@Nonnull UUID uuid) {
    Set<String> list = this.userGroups.get(uuid);
    if (list != null) {
        return Collections.unmodifiableSet(list);
    }
    return DEFAULT_GROUP_LIST;  // Set.of("Default")
}
```

Users without explicit group assignments automatically belong to the `"Default"` group.

### Thread Safety Model

All read operations acquire a **read lock**:
```java
this.fileLock.readLock().lock();
try {
    // Read operation
} finally {
    this.fileLock.readLock().unlock();
}
```

All write operations acquire a **write lock** and trigger save:
```java
this.fileLock.writeLock().lock();
try {
    // Modify data
    this.syncSave();  // Persist to disk
} finally {
    this.fileLock.writeLock().unlock();
}
```

### Internal Map Types

> **Note for Plugin Developers:** The internal storage maps in `HytalePermissionsProvider` use `Object2ObjectOpenHashMap` from [FastUtil](https://fastutil.di.unimi.it/), not standard `HashMap`. The virtual groups map in `PermissionsModule` also uses `Object2ObjectOpenHashMap`. This matters if you are serializing, inspecting, or casting these maps — they do not behave identically to `java.util.HashMap` in all edge cases.

### Data Cleanup

Empty permission/group sets are automatically removed:
```java
if (set.isEmpty()) {
    this.userPermissions.remove(uuid);
}
```

---

## 6. Built-in Permission Nodes

**Package:** `com.hypixel.hytale.server.core.permissions.HytalePermissions`

### Constants Reference

| Constant | Permission Node | Description |
|----------|-----------------|-------------|
| `NAMESPACE` | `"hytale"` | Base namespace |
| `COMMAND_BASE` | `"hytale.command"` | Base for all commands |
| `ASSET_EDITOR` | `"hytale.editor.asset"` | Asset editor access |
| `ASSET_EDITOR_PACKS_CREATE` | `"hytale.editor.packs.create"` | Create content packs |
| `ASSET_EDITOR_PACKS_EDIT` | `"hytale.editor.packs.edit"` | Edit content packs |
| `ASSET_EDITOR_PACKS_DELETE` | `"hytale.editor.packs.delete"` | Delete content packs |
| `BUILDER_TOOLS_EDITOR` | `"hytale.editor.builderTools"` | Builder tools access |
| `EDITOR_BRUSH_USE` | `"hytale.editor.brush.use"` | Use brush tool |
| `EDITOR_BRUSH_CONFIG` | `"hytale.editor.brush.config"` | Configure brush |
| `EDITOR_PREFAB_USE` | `"hytale.editor.prefab.use"` | Use prefabs |
| `EDITOR_PREFAB_MANAGE` | `"hytale.editor.prefab.manage"` | Manage prefabs |
| `EDITOR_SELECTION_USE` | `"hytale.editor.selection.use"` | Use selection tool |
| `EDITOR_SELECTION_CLIPBOARD` | `"hytale.editor.selection.clipboard"` | Copy/paste selections |
| `EDITOR_SELECTION_MODIFY` | `"hytale.editor.selection.modify"` | Modify selections |
| `EDITOR_HISTORY` | `"hytale.editor.history"` | Undo/redo access |
| `FLY_CAM` | `"hytale.camera.flycam"` | Fly camera mode |
| `WORLD_MAP_COORDINATE_TELEPORT` | `"hytale.world_map.teleport.coordinate"` | Coordinate teleport |
| `WORLD_MAP_MARKER_TELEPORT` | `"hytale.world_map.teleport.marker"` | Marker teleport |
| `UPDATE_NOTIFY` | `"hytale.system.update.notify"` | Update notifications |
| `MODS_OUTDATED_NOTIFY` | `"hytale.mods.outdated.notify"` | Outdated mod notifications |

### Permission Categories

```
hytale.
├── command.           # Command permissions
│   ├── op.add         # Add player to OP group
│   ├── op.remove      # Remove player from OP group
│   └── <command>      # Any registered command
├── editor.            # Editor/creative tools
│   ├── asset          # Asset editor
│   ├── builderTools   # Builder tools
│   ├── brush.*        # Brush tools
│   ├── prefab.*       # Prefab tools
│   ├── selection.*    # Selection tools
│   ├── history        # Undo/redo
│   └── packs.*        # Content pack management
├── camera.            # Camera modes
│   └── flycam         # Fly camera
├── world_map.         # World map features
│   └── teleport.*     # Teleportation
├── system.            # System features
│   └── update.notify  # Update notifications
└── mods.              # Mod-related features
    └── outdated.notify # Outdated mod notifications
```

### Helper Methods

```java
// Generate command permission: "hytale.command.<name>"
public static String fromCommand(@Nonnull String name);

// Generate subcommand permission: "hytale.command.<name>.<subCommand>"
public static String fromCommand(@Nonnull String name, @Nonnull String subCommand);
```

**Examples:**
```java
HytalePermissions.fromCommand("op.add");      // "hytale.command.op.add"
HytalePermissions.fromCommand("gamemode", "survival");  // "hytale.command.gamemode.survival"
```

---

## 7. Events System

All permission changes fire events through Hytale's EventBus. Events implement `IEvent<Void>`.

### Event Hierarchy

```
IEvent<Void>
├── PlayerPermissionChangeEvent (abstract)
│   ├── PermissionsAdded
│   ├── PermissionsRemoved
│   ├── GroupAdded
│   └── GroupRemoved
├── PlayerGroupEvent extends PlayerPermissionChangeEvent
│   ├── Added
│   └── Removed
└── GroupPermissionChangeEvent (abstract)
    ├── Added
    └── Removed
```

---

### 7.1 PlayerPermissionChangeEvent

**Package:** `com.hypixel.hytale.server.core.event.events.permissions`

Base class for player-related permission changes.

```java
public abstract class PlayerPermissionChangeEvent implements IEvent<Void> {
    public UUID getPlayerUuid();
}
```

#### PermissionsAdded

Fired when permissions are added directly to a user.

```java
public static class PermissionsAdded extends PlayerPermissionChangeEvent {
    public Set<String> getAddedPermissions();  // Unmodifiable
}
```

**Triggered by:** `PermissionsModule.addUserPermission(uuid, permissions)`

#### PermissionsRemoved

Fired when permissions are removed from a user.

```java
public static class PermissionsRemoved extends PlayerPermissionChangeEvent {
    public Set<String> getRemovedPermissions();  // Unmodifiable
}
```

**Triggered by:** `PermissionsModule.removeUserPermission(uuid, permissions)`

#### GroupAdded / GroupRemoved (Inner Classes)

```java
public static class GroupAdded extends PlayerPermissionChangeEvent {
    public String getGroupName();
}

public static class GroupRemoved extends PlayerPermissionChangeEvent {
    public String getGroupName();
}
```

**Note:** These are inner classes of `PlayerPermissionChangeEvent`, not standalone events.

---

### 7.2 PlayerGroupEvent

**Package:** `com.hypixel.hytale.server.core.event.events.permissions`

Standalone event for group membership changes. Extends `PlayerPermissionChangeEvent`.

```java
public class PlayerGroupEvent extends PlayerPermissionChangeEvent {
    public String getGroupName();

    public static class Added extends PlayerGroupEvent { }
    public static class Removed extends PlayerGroupEvent { }
}
```

| Event Class | Triggered By |
|-------------|--------------|
| `PlayerGroupEvent.Added` | `PermissionsModule.addUserToGroup(uuid, group)` |
| `PlayerGroupEvent.Removed` | `PermissionsModule.removeUserFromGroup(uuid, group)` |

---

### 7.3 GroupPermissionChangeEvent

**Package:** `com.hypixel.hytale.server.core.event.events.permissions`

Events for group permission modifications.

```java
public abstract class GroupPermissionChangeEvent implements IEvent<Void> {
    public String getGroupName();

    public static class Added extends GroupPermissionChangeEvent {
        public Set<String> getAddedPermissions();  // Unmodifiable
    }

    public static class Removed extends GroupPermissionChangeEvent {
        public Set<String> getRemovedPermissions();  // Unmodifiable
    }
}
```

| Event Class | Triggered By |
|-------------|--------------|
| `GroupPermissionChangeEvent.Added` | `PermissionsModule.addGroupPermission(group, perms)` |
| `GroupPermissionChangeEvent.Removed` | `PermissionsModule.removeGroupPermission(group, perms)` |

---

### Event Dispatch Pattern

Events are dispatched via Hytale's EventBus:

```java
HytaleServer.get()
    .getEventBus()
    .<Void, EventClass>dispatchFor(EventClass.class)
    .dispatch(new EventClass(...));
```

---

## 8. Commands Reference

### 8.1 /op Commands

The `/op` command manages operator status.

```
/op
├── self     - Toggle own OP status
├── add      - Add player to OP group
└── remove   - Remove player from OP group
```

**Note:** The `/op` command itself does NOT generate a permission (`canGeneratePermission() = false`).

#### /op self

| Property | Value |
|----------|-------|
| **Syntax** | `/op self` |
| **Permission** | None (special conditions) |
| **Description** | Toggle your own OP status |

**Conditions for execution:**
1. **Provider Check:** Fails if `areProvidersTampered() == true` (custom providers installed)
2. **Singleplayer:** Only works if you're the world owner
3. **Multiplayer:** Requires `--allow-op` server launch argument

**Behavior:**
- If user is in "OP" group → removes from "OP" group
- If user is NOT in "OP" group → adds to "OP" group

#### /op add <player>

| Property | Value |
|----------|-------|
| **Syntax** | `/op add <player_uuid>` |
| **Permission** | `hytale.command.op.add` |
| **Description** | Add a player to the OP group |

**Behavior:**
- Checks if player already in "OP" group → error message
- Adds player to "OP" group
- Notifies target player if online

#### /op remove <player>

| Property | Value |
|----------|-------|
| **Syntax** | `/op remove <player_uuid>` |
| **Permission** | `hytale.command.op.remove` |
| **Description** | Remove a player from the OP group |

**Behavior:**
- Checks if player NOT in "OP" group → error message
- Removes player from "OP" group
- Notifies target player if online

---

### 8.2 /perm Commands

The `/perm` command provides full permission management.

```
/perm
├── user
│   ├── list <uuid>                    - List user's permissions
│   ├── add <uuid> <perm...>          - Add permissions to user
│   ├── remove <uuid> <perm...>       - Remove permissions from user
│   └── group
│       ├── list <uuid>                - List user's groups
│       ├── add <uuid> <group>        - Add user to group
│       └── remove <uuid> <group>     - Remove user from group
├── group
│   ├── list <group>                   - List group's permissions
│   ├── add <group> <perm...>         - Add permissions to group
│   └── remove <group> <perm...>      - Remove permissions from group
└── test <node...>                     - Test permissions
```

#### User Permission Commands

| Command | Description |
|---------|-------------|
| `/perm user list <uuid>` | Lists all permissions for a user from all providers |
| `/perm user add <uuid> <perms...>` | Adds one or more permissions to a user |
| `/perm user remove <uuid> <perms...>` | Removes one or more permissions from a user |

#### User Group Commands

| Command | Description |
|---------|-------------|
| `/perm user group list <uuid>` | Lists all groups a user belongs to from all providers |
| `/perm user group add <uuid> <group>` | Adds a user to a group |
| `/perm user group remove <uuid> <group>` | Removes a user from a group |

#### Group Commands

| Command | Description |
|---------|-------------|
| `/perm group list <group>` | Lists all permissions for a group from all providers |
| `/perm group add <group> <perms...>` | Adds permissions to a group |
| `/perm group remove <group> <perms...>` | Removes permissions from a group |

#### Test Command

| Command | Description |
|---------|-------------|
| `/perm test <nodes...>` | Tests if the sender has the specified permission nodes |

---

## 9. Exception Handling

### NoPermissionException

**Package:** `com.hypixel.hytale.server.core.command.system.exceptions`

Thrown when a command requires a permission the sender doesn't have.

```java
public class NoPermissionException extends CommandException {
    public NoPermissionException(@Nonnull String permission);

    @Override
    public void sendTranslatedMessage(@Nonnull CommandSender sender);
}
```

**Behavior:**
- Stores the required permission node
- Sends translated error message with red color
- Translation key: `server.commands.errors.permission`
- Parameter: `permission` (the missing permission node)

**Example output:**
```
You don't have permission: hytale.command.gamemode
```

---

## 10. Plugin Developer Guide

### Creating a Custom PermissionProvider

Implement the `PermissionProvider` interface:

```java
public class MyPermissionProvider implements PermissionProvider {

    @Nonnull
    @Override
    public String getName() {
        return "MyPermissionProvider";
    }

    // Implement all 9 methods...

    @Override
    public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        // Store permissions (database, config, etc.)
    }

    @Override
    public Set<String> getUserPermissions(@Nonnull UUID uuid) {
        // Retrieve permissions
        return Collections.emptySet();
    }

    // ... remaining methods
}
```

### Registering Your Provider

```java
// In your plugin's setup or start method
PermissionsModule.get().addProvider(new MyPermissionProvider());
```

**Provider Priority:**
- Providers are checked in order (index 0 first)
- The standard `HytalePermissionsProvider` is at index 0 by default
- Use `addProvider()` to add to the end of the list
- All write operations go through `getFirstPermissionProvider()` (index 0)

### Checking Permissions

**Via PermissionsModule (recommended for offline players):**
```java
// Simple check (default: false)
boolean hasPerm = PermissionsModule.get().hasPermission(uuid, "my.permission");

// With custom default
boolean hasPerm = PermissionsModule.get().hasPermission(uuid, "my.permission", true);
```

**Via PermissionHolder (for online players):**
```java
// On PlayerRef or CommandSender
boolean hasPerm = player.hasPermission("my.permission");
boolean hasPerm = player.hasPermission("my.permission", false);
```

### Modifying Permissions Programmatically

```java
PermissionsModule perms = PermissionsModule.get();

// User permissions
perms.addUserPermission(uuid, Set.of("my.permission", "my.other"));
perms.removeUserPermission(uuid, Set.of("my.permission"));

// Group permissions
perms.addGroupPermission("VIP", Set.of("vip.feature"));
perms.removeGroupPermission("VIP", Set.of("vip.feature"));

// Group membership
perms.addUserToGroup(uuid, "VIP");
perms.removeUserFromGroup(uuid, "VIP");
```

### Listening for Permission Changes

Subscribe to events via the EventBus:

```java
// Example: Listen for user permission changes
HytaleServer.get().getEventBus()
    .subscribe(PlayerPermissionChangeEvent.PermissionsAdded.class, event -> {
        UUID uuid = event.getPlayerUuid();
        Set<String> added = event.getAddedPermissions();
        // React to changes
    });

// Listen for group membership changes
HytaleServer.get().getEventBus()
    .subscribe(PlayerGroupEvent.Added.class, event -> {
        UUID uuid = event.getPlayerUuid();
        String group = event.getGroupName();
        // React to group addition
    });
```

### Thread-Safety Considerations

- `PermissionsModule.providers` is a `CopyOnWriteArrayList` (thread-safe for iteration)
- `HytalePermissionsProvider` uses `ReadWriteLock` for all operations
- Custom providers **must** be thread-safe
- Permission checks can occur from multiple threads simultaneously

### Best Practices

1. **Provider Registration:** Register early (in `setup()` or `start()`)
2. **Permission Naming:** Follow the `namespace.category.action` pattern
3. **Wildcard Usage:** Prefer specific permissions over wildcards for security
4. **Event Handling:** Don't modify permissions in response to permission events (infinite loop risk)
5. **Caching:** Consider caching permission results if checking frequently

---

## 11. Storage Format (permissions.json)

The default `HytalePermissionsProvider` uses JSON for persistence.

### File Location

```
<server_root>/permissions.json
```

### Structure

```json
{
  "users": {
    "550e8400-e29b-41d4-a716-446655440000": {
      "permissions": ["custom.perm.1", "custom.perm.2"],
      "groups": ["VIP", "Builder"]
    },
    "6ba7b810-9dad-11d1-80b4-00c04fd430c8": {
      "permissions": ["another.permission"],
      "groups": ["Moderator"]
    }
  },
  "groups": {
    "OP": ["*"],
    "Default": [],
    "VIP": ["vip.feature", "vip.chat.color"],
    "Moderator": ["hytale.command.*", "mod.kick", "mod.mute"],
    "Builder": ["hytale.editor.*"]
  }
}
```

### Schema Details

| Path | Type | Description |
|------|------|-------------|
| `users` | Object | Map of UUID strings to user data |
| `users.<uuid>.permissions` | Array | Direct permissions for user |
| `users.<uuid>.groups` | Array | Group names user belongs to |
| `groups` | Object | Map of group names to permission arrays |
| `groups.<name>` | Array | Permissions for the group |

### Notes

- Users without explicit groups default to `["Default"]` group
- Empty arrays are omitted during save
- File is saved with pretty printing (indented JSON)

> **WARNING: OP and Default Group Overwrite Behavior**
>
> After loading `permissions.json`, the server forcibly re-inserts the DEFAULT_GROUPS using `put()` (not `putIfAbsent()`). This means **any custom permissions added to the OP or Default groups in permissions.json are OVERWRITTEN on every server load**. Only the default values (`["*"]` for OP, `[]` for Default) survive a restart. Use custom group names (e.g., `"SuperOP"`, `"Member"`) for additional permission sets.
>
> *Source: `HytalePermissionsProvider.java:257-259`*

### Example Minimal File

When `permissions.json` is first created by the server, the `create()` method writes an empty JSON object:
```json
{}
```

After the first modification triggers a save (e.g., adding a user to a group), the file expands to include the default groups:
```json
{
  "groups": {
    "OP": ["*"],
    "Default": []
  }
}
```

> **Note:** The OP and Default groups only appear in the file after a read+write cycle. The initial file is always `{}`.

---

## 12. HyperPerms Compatibility Notes

### How HyperPerms Integrates

HyperPerms hooks into Hytale's permissions system by:

1. **Registering a Custom Provider:** `PermissionsModule.get().addProvider(hyperPermsProvider)`
2. **Optionally Replacing Default:** Can remove standard provider for full control
3. **Database Backend:** Replaces file-based storage with SQL/NoSQL
4. **Extended Features:** Inheritance, temporary permissions, contexts

### Provider Tamper Detection

Be aware that `areProvidersTampered()` returns `true` when:
- More than one provider is registered
- First provider is not the standard provider

This affects `/op self` functionality (it will refuse to work).

### Differences from Vanilla

| Feature | Vanilla | HyperPerms |
|---------|---------|------------|
| Storage | JSON file | Database |
| Group Inheritance | None | Supported |
| Permission Expiry | None | Supported |
| Contexts (world, etc.) | None | Supported |
| Web Interface | None | Optional |

### Extended Features HyperPerms Provides

1. **Group Inheritance:** Groups can inherit from other groups
2. **Permission Weight:** Priority system for conflicting permissions
3. **Temporary Permissions:** Time-limited permissions/group memberships
4. **Server Groups:** Different permissions per server in a network
5. **Verbose Logging:** Debug permission checks
6. **Migration Tools:** Import from vanilla `permissions.json`

### Recommended Integration Pattern

```java
public class HyperPermsPlugin extends JavaPlugin {
    private HyperPermsProvider provider;

    @Override
    protected void start() {
        // Initialize database connection
        this.provider = new HyperPermsProvider(database);

        // Register as additional provider
        PermissionsModule.get().addProvider(this.provider);

        // Or replace default entirely:
        // PermissionsModule perms = PermissionsModule.get();
        // perms.removeProvider(perms.getFirstPermissionProvider());
        // perms.addProvider(this.provider);
    }

    @Override
    protected void stop() {
        PermissionsModule.get().removeProvider(this.provider);
    }
}
```

---

## Appendix: Source Files Reference

All documentation derived from these decompiled sources:

### Core Permissions
- `com/hypixel/hytale/server/core/permissions/PermissionHolder.java`
- `com/hypixel/hytale/server/core/permissions/PermissionsModule.java`
- `com/hypixel/hytale/server/core/permissions/HytalePermissions.java`

### Providers
- `com/hypixel/hytale/server/core/permissions/provider/PermissionProvider.java`
- `com/hypixel/hytale/server/core/permissions/provider/HytalePermissionsProvider.java`

### Commands
- `com/hypixel/hytale/server/core/permissions/commands/PermCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/PermUserCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/PermGroupCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/PermTestCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/op/OpCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/op/OpSelfCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/op/OpAddCommand.java`
- `com/hypixel/hytale/server/core/permissions/commands/op/OpRemoveCommand.java`

### Events
- `com/hypixel/hytale/server/core/event/events/permissions/PlayerPermissionChangeEvent.java`
- `com/hypixel/hytale/server/core/event/events/permissions/GroupPermissionChangeEvent.java`
- `com/hypixel/hytale/server/core/event/events/permissions/PlayerGroupEvent.java`

### Exceptions
- `com/hypixel/hytale/server/core/command/system/exceptions/NoPermissionException.java`

---

*Documentation generated from Vineflower-decompiled Hytale server sources.*
