# Hytale Permissions - Quick Reference

> One-page cheat sheet for daily development

---

## Permission Check

```java
// Via PermissionsModule (works for offline players)
PermissionsModule.get().hasPermission(uuid, "perm.node");
PermissionsModule.get().hasPermission(uuid, "perm.node", false);  // with default

// Via PermissionHolder (online players)
player.hasPermission("perm.node");
player.hasPermission("perm.node", false);
```

---

## Wildcard Patterns

| Pattern | Effect | Example |
|---------|--------|---------|
| `*` | Grant ALL | User has every permission |
| `-*` | Deny ALL | User blocked from everything |
| `prefix.*` | Grant prefix | `hytale.command.*` = all commands |
| `-prefix.*` | Deny prefix | `-hytale.editor.*` = no editor |
| `exact.node` | Grant exact | `my.feature.use` |
| `-exact.node` | Deny exact | `-my.feature.admin` |

**Resolution:** First match wins. Deny (`-`) takes precedence when matched.

---

## Resolution Order

```
1. User direct permissions
2. User's Group[0] permissions
3. User's Group[0] virtual permissions
4. User's Group[1] permissions
   ...
5. Provider[1] (if multiple providers)
   ...
N. Default value (false)
```

---

## Modify Permissions

```java
PermissionsModule perms = PermissionsModule.get();

// User permissions
perms.addUserPermission(uuid, Set.of("perm1", "perm2"));
perms.removeUserPermission(uuid, Set.of("perm1"));

// Group permissions
perms.addGroupPermission("VIP", Set.of("vip.feature"));
perms.removeGroupPermission("VIP", Set.of("vip.feature"));

// Group membership
perms.addUserToGroup(uuid, "VIP");
perms.removeUserFromGroup(uuid, "VIP");

// Get user's groups
Set<String> groups = perms.getGroupsForUser(uuid);
```

---

## Default Groups

| Group | Permissions | Notes |
|-------|-------------|-------|
| `OP` | `["*"]` | Full access |
| `Default` | `[]` | Auto-assigned if no groups |

---

## Commands

### /op
```
/op self                    # Toggle own OP (special conditions)
/op add <uuid>              # Requires: hytale.command.op.add
/op remove <uuid>           # Requires: hytale.command.op.remove
```

### /perm user
```
/perm user list <uuid>
/perm user add <uuid> <perm...>
/perm user remove <uuid> <perm...>
/perm user group list <uuid>
/perm user group add <uuid> <group>
/perm user group remove <uuid> <group>
```

### /perm group
```
/perm group list <group>
/perm group add <group> <perm...>
/perm group remove <group> <perm...>
```

### /perm test
```
/perm test <node...>        # Test if sender has permissions
```

---

## Events

| Event | When Fired |
|-------|------------|
| `PlayerPermissionChangeEvent.PermissionsAdded` | User perms added |
| `PlayerPermissionChangeEvent.PermissionsRemoved` | User perms removed |
| `PlayerGroupEvent.Added` | User added to group |
| `PlayerGroupEvent.Removed` | User removed from group |
| `GroupPermissionChangeEvent.Added` | Group perms added |
| `GroupPermissionChangeEvent.Removed` | Group perms removed |

```java
eventBus.subscribe(PlayerGroupEvent.Added.class, event -> {
    UUID uuid = event.getPlayerUuid();
    String group = event.getGroupName();
});
```

---

## Custom Provider

```java
public class MyProvider implements PermissionProvider {
    public String getName() { return "MyProvider"; }

    // 9 methods to implement:
    // addUserPermissions, removeUserPermissions, getUserPermissions
    // addGroupPermissions, removeGroupPermissions, getGroupPermissions
    // addUserToGroup, removeUserFromGroup, getGroupsForUser
}

// Register
PermissionsModule.get().addProvider(new MyProvider());
```

---

## Built-in Permission Nodes

```
hytale.command.<name>           # Command access
hytale.command.op.add           # /op add
hytale.command.op.remove        # /op remove
hytale.editor.builderTools      # Builder tools
hytale.editor.brush.*           # Brush tools
hytale.editor.prefab.*          # Prefabs
hytale.editor.selection.*       # Selection tools
hytale.editor.history           # Undo/redo
hytale.camera.flycam            # Fly camera
hytale.world_map.teleport.*     # Map teleport
hytale.system.update.notify     # Update notifications
```

---

## Storage (permissions.json)

```json
{
  "users": {
    "uuid-here": {
      "permissions": ["perm1", "perm2"],
      "groups": ["VIP"]
    }
  },
  "groups": {
    "OP": ["*"],
    "Default": [],
    "VIP": ["vip.*"]
  }
}
```

---

## Key Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `PermissionsModule` | `...permissions` | Singleton coordinator |
| `PermissionProvider` | `...permissions.provider` | Provider interface |
| `HytalePermissionsProvider` | `...permissions.provider` | Default file provider |
| `PermissionHolder` | `...permissions` | Entity with perms |
| `NoPermissionException` | `...command.system.exceptions` | Permission denied |

---

*See [PERMISSIONS_SYSTEM.md](PERMISSIONS_SYSTEM.md) for complete documentation*
