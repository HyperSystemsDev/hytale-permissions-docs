# Glossary

> **Definitions of terms used in Hytale permissions documentation**

---

## A

### Action
The final component of a permission node that describes what operation is being permitted. Examples: `.use`, `.create`, `.delete`, `.view`.

---

## C

### Category
A grouping within the permission hierarchy that organizes related permissions. Example: In `hytale.editor.brush.use`, `editor` and `brush` are categories.

---

## D

### Default Group
The built-in group (`"Default"`) automatically assigned to users who have no explicit group membership. Contains no permissions by default.

### Default Provider
See [HytalePermissionsProvider](#hytalepermissionsprovider).

### Default Value
The boolean value returned when no permission match is found. Typically `false`, but can be overridden via `hasPermission(uuid, perm, defaultValue)`.

---

## E

### EventBus
Hytale's event dispatch system. Permission changes trigger events that plugins can subscribe to for reactive behavior.

### Exact Match
A permission check where the requested permission exactly matches a stored permission node (e.g., requesting `fly.use` when `fly.use` is granted).

---

## G

### Grant
The act of giving a permission to a user or group. Permissions are granted by adding them to the permission set.

### Group
A named collection of permissions that can be assigned to users. Users inherit all permissions from their assigned groups.

### Group Inheritance
A feature (not in vanilla Hytale) where groups can inherit permissions from other groups, forming a hierarchy.

---

## H

### Hierarchy
The dot-separated structure of permission nodes that enables prefix wildcards. Example: `hytale.command.gamemode` has ancestors `hytale.command.*` and `hytale.*`.

### HytalePermissionsProvider
The default, file-based permission provider that ships with Hytale. Stores data in `permissions.json`.

---

## N

### Namespace
The top-level identifier in a permission node. Example: `hytale` in `hytale.command.op.add`. Custom plugins should use their own namespace.

### Negation
Explicitly denying a permission by prefixing it with `-`. Example: `-fly.use` denies the `fly.use` permission.

### Node
A single permission string. Can be an exact permission (`hytale.command.op.add`) or a wildcard (`hytale.command.*`).

---

## O

### OP (Operator)
A special group with full permissions (`*` wildcard). Members of the `OP` group can perform any action.

---

## P

### Permission
A string identifier that controls access to a feature or action. Follows the format `namespace.category.action`.

### PermissionHolder
The interface implemented by entities that can have permissions checked against them (players, command senders).

### PermissionProvider
The interface that defines how permissions are stored and retrieved. Can be implemented for custom backends (database, API, etc.).

### PermissionsModule
The central singleton that coordinates the permissions system. Manages providers, handles permission checks, and dispatches events.

### Prefix Wildcard
A wildcard ending with `.*` that grants all permissions starting with that prefix. Example: `hytale.editor.*` grants all editor permissions.

### Priority
The order in which permission checks are evaluated. User permissions are checked before group permissions; first definitive match wins.

### Provider
Short for [PermissionProvider](#permissionprovider).

### Provider Chain
The ordered list of registered permission providers. Permissions are checked across all providers in order. Both grants and denials stop the chain — the first definitive match (whether grant or deny) wins, and no further providers are consulted.

---

## R

### ReadWriteLock
A concurrency mechanism used by `HytalePermissionsProvider` to allow multiple concurrent reads while ensuring exclusive writes.

### Resolution
The process of determining whether a user has a specific permission by checking against all applicable rules.

### Resolution Order
The sequence in which permission sources are checked:
1. User direct permissions
2. Group permissions (for each group)
3. Virtual group permissions (for each group)
4. Default value

---

## S

### Singleton
A design pattern ensuring only one instance exists. `PermissionsModule.get()` returns the single permissions system instance.

---

## T

### Tamper Detection
The `areProvidersTampered()` method that checks if custom providers have been registered, affecting `/op self` functionality.

### Thread-Safe
Code that can be safely executed by multiple threads simultaneously without causing data corruption.

---

## U

### UUID
Universally Unique Identifier. The 128-bit identifier used to reference players across sessions.

---

## V

### Virtual Group
A context-based permission set that grants additional permissions based on game state. By default, only Creative mode has a virtual group, which grants `hytale.editor.builderTools`. Custom virtual groups can be defined via `PermissionsModule.setVirtualGroups()`. Virtual groups are global (not per-provider) — they are stored on `PermissionsModule` itself and applied regardless of which provider returned the group membership.

---

## W

### Wildcard
A pattern that matches multiple permissions:
- `*` - Matches all permissions
- `prefix.*` - Matches all permissions under a prefix
- `-*` - Denies all permissions
- `-prefix.*` - Denies all permissions under a prefix

---

## Quick Reference Table

| Term | Definition |
|------|------------|
| Category | Organizational grouping in permission hierarchy |
| Default Group | Auto-assigned group for users without groups |
| Grant | Adding a permission to a user/group |
| Group | Named collection of permissions |
| Namespace | Top-level permission identifier |
| Negation | Denying a permission with `-` prefix |
| Node | A single permission string |
| OP | Operator group with full permissions |
| Provider | Permission storage/retrieval implementation |
| Resolution | Process of determining permission status |
| Virtual Group | Context-based permission set |
| Wildcard | Pattern matching multiple permissions |

---

*Glossary for Hytale permissions system*
