# Permission Nodes Registry

> **Complete reference of all known Hytale permission nodes**

---

## Table of Contents

1. [Namespace Structure](#namespace-structure)
2. [Command Permissions](#command-permissions)
3. [Editor Permissions](#editor-permissions)
4. [Camera Permissions](#camera-permissions)
5. [World Map Permissions](#world-map-permissions)
6. [System Permissions](#system-permissions)
7. [Mods Permissions](#mods-permissions)
8. [Wildcards](#wildcards)
9. [Permission Generation](#permission-generation)

---

## Namespace Structure

All Hytale permissions follow the hierarchical pattern:

```
hytale.<category>.<subcategory>.<action>
```

**Base Namespace:** `hytale`

```
hytale.
├── command.*        # Command execution permissions
├── editor.*         # Creative/editor tool permissions
├── camera.*         # Camera mode permissions
├── world_map.*      # Map feature permissions
├── system.*         # System-level permissions
└── mods.*           # Mod-related permissions
```

---

## Command Permissions

**Base:** `hytale.command`

### Operator Commands

| Permission | Description |
|------------|-------------|
| `hytale.command.op.add` | Add a player to the OP group |
| `hytale.command.op.remove` | Remove a player from the OP group |

> **Note:** `/op self` does NOT require a permission - it has special conditions (singleplayer ownership or `--allow-op` flag).

### Permission Management Commands

| Permission | Description |
|------------|-------------|
| `hytale.command.perm` | Base /perm command access |
| `hytale.command.perm.user` | User permission management |
| `hytale.command.perm.user.list` | List user permissions |
| `hytale.command.perm.user.add` | Add user permissions |
| `hytale.command.perm.user.remove` | Remove user permissions |
| `hytale.command.perm.user.group` | User group management |
| `hytale.command.perm.user.group.list` | List user groups |
| `hytale.command.perm.user.group.add` | Add user to group |
| `hytale.command.perm.user.group.remove` | Remove user from group |
| `hytale.command.perm.group` | Group permission management |
| `hytale.command.perm.group.list` | List group permissions |
| `hytale.command.perm.group.add` | Add group permissions |
| `hytale.command.perm.group.remove` | Remove group permissions |
| `hytale.command.perm.test` | Test permissions |

### Dynamic Command Permissions

Commands auto-generate permissions using:

```java
HytalePermissions.fromCommand("commandname");
// Result: "hytale.command.commandname"

HytalePermissions.fromCommand("command", "subcommand");
// Result: "hytale.command.command.subcommand"
```

**Common dynamic permissions:**

| Permission | Command |
|------------|---------|
| `hytale.command.gamemode` | `/gamemode` |
| `hytale.command.gamemode.survival` | `/gamemode survival` |
| `hytale.command.gamemode.creative` | `/gamemode creative` |
| `hytale.command.teleport` | `/teleport` (likely) |
| `hytale.command.give` | `/give` (likely) |
| `hytale.command.time` | `/time` (likely) |
| `hytale.command.weather` | `/weather` (likely) |

---

## Editor Permissions

**Base:** `hytale.editor`

### Asset Editor

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.asset` | `ASSET_EDITOR` | Access asset editor |

### Content Packs

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.packs.create` | `ASSET_EDITOR_PACKS_CREATE` | Create content packs |
| `hytale.editor.packs.edit` | `ASSET_EDITOR_PACKS_EDIT` | Edit content packs |
| `hytale.editor.packs.delete` | `ASSET_EDITOR_PACKS_DELETE` | Delete content packs |

### Builder Tools

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.builderTools` | `BUILDER_TOOLS_EDITOR` | Access builder tools |

### Brush Tools

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.brush.use` | `EDITOR_BRUSH_USE` | Use brush tool |
| `hytale.editor.brush.config` | `EDITOR_BRUSH_CONFIG` | Configure brush settings |

### Prefab Tools

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.prefab.use` | `EDITOR_PREFAB_USE` | Use prefabs |
| `hytale.editor.prefab.manage` | `EDITOR_PREFAB_MANAGE` | Create/edit/delete prefabs |

### Selection Tools

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.selection.use` | `EDITOR_SELECTION_USE` | Use selection tool |
| `hytale.editor.selection.clipboard` | `EDITOR_SELECTION_CLIPBOARD` | Copy/paste operations |
| `hytale.editor.selection.modify` | `EDITOR_SELECTION_MODIFY` | Fill/replace/transform |

### History

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.editor.history` | `EDITOR_HISTORY` | Undo/redo operations |

---

## Camera Permissions

**Base:** `hytale.camera`

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.camera.flycam` | `FLY_CAM` | Use fly camera mode |

---

## World Map Permissions

**Base:** `hytale.world_map`

### Teleportation

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.world_map.teleport.coordinate` | `WORLD_MAP_COORDINATE_TELEPORT` | Teleport to coordinates via map |
| `hytale.world_map.teleport.marker` | `WORLD_MAP_MARKER_TELEPORT` | Teleport to map markers |

---

## System Permissions

**Base:** `hytale.system`

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.system.update.notify` | `UPDATE_NOTIFY` | Receive update notifications |

---

## Mods Permissions

**Base:** `hytale.mods`

| Permission | Constant | Description |
|------------|----------|-------------|
| `hytale.mods.outdated.notify` | `MODS_OUTDATED_NOTIFY` | Receive outdated mod notifications |

---

## Wildcards

### Grant Patterns

| Pattern | Effect |
|---------|--------|
| `*` | Grants ALL permissions |
| `prefix.*` | Grants all permissions under prefix |
| `exact.perm` | Grants a specific permission |

**Examples:**

| Pattern | Effect |
|---------|--------|
| `hytale.*` | Grants all Hytale permissions |
| `hytale.command.*` | Grants all command permissions |
| `hytale.editor.*` | Grants all editor permissions |
| `hytale.editor.brush.*` | Grants all brush permissions |

### Deny Patterns

| Pattern | Effect |
|---------|--------|
| `-*` | Denies ALL permissions |
| `-prefix.*` | Denies all permissions under prefix |
| `-exact.perm` | Denies a specific permission |

**Examples:**

| Pattern | Effect |
|---------|--------|
| `-hytale.command.*` | Denies all command permissions |
| `-hytale.editor.*` | Denies all editor permissions |
| `-hytale.editor.packs.*` | Denies all pack management permissions |

### Wildcard Restrictions

> **Important:** Middle wildcards (e.g., `hytale.*.ban`) are **NOT** supported. The `*` character in `hytale.*.ban` is treated as a literal asterisk, not a wildcard pattern. Wildcards only work in two positions:
> - Standalone: `*` (grant all) or `-*` (deny all)
> - Trailing: `prefix.*` (grant all under prefix) or `-prefix.*` (deny all under prefix)

---

## Permission Generation

### Command Permission Helper

```java
// Single command
String perm = HytalePermissions.fromCommand("gamemode");
// Result: "hytale.command.gamemode"

// Command with subcommand
String perm = HytalePermissions.fromCommand("gamemode", "creative");
// Result: "hytale.command.gamemode.creative"
```

### Custom Permission Patterns

When creating custom plugins, follow the pattern:

```
<namespace>.<category>.<action>

Examples:
hyperperms.admin.reload
hyperperms.user.info
myplugin.feature.use
```

---

## Default Group Permissions

### OP Group

```json
{
  "OP": ["*"]
}
```

The `OP` group grants all permissions via the `*` wildcard.

### Default Group

```json
{
  "Default": []
}
```

The `Default` group has no permissions by default.

---

## Quick Permission Lookup

### By Category

<details>
<summary><strong>All Command Permissions</strong></summary>

```
hytale.command.*
hytale.command.op.add
hytale.command.op.remove
hytale.command.perm.*
hytale.command.gamemode
hytale.command.teleport
```
</details>

<details>
<summary><strong>All Editor Permissions</strong></summary>

```
hytale.editor.*
hytale.editor.asset
hytale.editor.builderTools
hytale.editor.brush.use
hytale.editor.brush.config
hytale.editor.prefab.use
hytale.editor.prefab.manage
hytale.editor.selection.use
hytale.editor.selection.clipboard
hytale.editor.selection.modify
hytale.editor.history
hytale.editor.packs.create
hytale.editor.packs.edit
hytale.editor.packs.delete
```
</details>

<details>
<summary><strong>All Other Permissions</strong></summary>

```
hytale.camera.flycam
hytale.world_map.teleport.coordinate
hytale.world_map.teleport.marker
hytale.system.update.notify
hytale.mods.outdated.notify
```
</details>

---

*Complete permission node registry for Hytale server*
