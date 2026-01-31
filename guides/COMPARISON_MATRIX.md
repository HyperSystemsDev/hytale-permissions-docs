# Permission System Comparison Matrix

Comparison of Hytale's permission system with popular Minecraft permission plugins. Useful for developers familiar with these systems.

---

## Quick Comparison

| Feature | Hytale | LuckPerms | PermissionsEx | UltraPermissions |
|---------|--------|-----------|---------------|------------------|
| Provider Pattern | Yes | No (monolithic) | No | No |
| Group Inheritance | No | Yes | Yes | Yes |
| Permission Weight | No | Yes | Yes | Yes |
| Temporary Permissions | No | Yes | Yes | Yes |
| Context System | Virtual Groups | Full contexts | Worlds | Limited |
| Negation Syntax | `-permission` | `permission: false` | `-permission` | GUI-based |
| Wildcard | `*`, `prefix.*` | `*`, `prefix.*` | `*`, `prefix.*` | `*` |
| Storage | JSON file | Multiple | Multiple | MySQL/SQLite |
| Web Interface | No | Yes | No | Yes |
| API Style | Provider interface | Service API | Manager API | API class |

---

## Detailed Feature Comparison

### 1. Permission Resolution

#### Hytale
```
User Direct → User Group[0] → Group[0] Virtual → User Group[1] → ... → Default
```

**Characteristics:**
- First match wins
- No priority/weight system
- Providers checked in order
- Virtual groups for game-mode permissions

#### LuckPerms
```
User → Primary Group → Parent Groups (weighted) → Default
```

**Characteristics:**
- Weight-based priority
- Full inheritance chains
- Context-aware (world, server, etc.)
- Meta/options support

#### PermissionsEx
```
User → Groups (priority ordered) → Default Group
```

**Characteristics:**
- Priority numbers on groups
- Inheritance chains
- World-specific permissions
- Timed permissions

---

### 2. Permission Syntax

| System | Grant | Deny | Wildcard | Prefix Wildcard |
|--------|-------|------|----------|-----------------|
| **Hytale** | `perm.node` | `-perm.node` | `*` | `prefix.*` |
| **LuckPerms** | `perm.node` | `perm.node` (value: false) | `*` | `prefix.*` |
| **PermissionsEx** | `perm.node` | `-perm.node` | `*` | `prefix.*` |

#### Hytale Example
```json
{
  "permissions": ["fly.enabled", "-pvp.damage", "admin.*"]
}
```

#### LuckPerms Equivalent (YAML)
```yaml
permissions:
  - fly.enabled
  - pvp.damage:
      value: false
  - admin.*
```

#### PermissionsEx Equivalent
```yaml
permissions:
  - fly.enabled
  - -pvp.damage
  - admin.*
```

---

### 3. Group System

#### Hytale Groups

```json
{
  "groups": {
    "OP": ["*"],
    "Admin": ["admin.*", "mod.*"],
    "Moderator": ["mod.*"],
    "VIP": ["vip.*"],
    "Default": []
  }
}
```

**Features:**
- Flat structure (no inheritance)
- User can belong to multiple groups
- All groups checked sequentially
- `Default` group for users without groups

#### LuckPerms Groups

```yaml
# Admin inherits from Moderator
Admin:
  permissions:
    - admin.*
  parents:
    - Moderator

Moderator:
  permissions:
    - mod.*
  parents:
    - Default
```

**Features:**
- Inheritance hierarchy
- Weight-based priority
- Meta data (prefix, suffix)
- Tracks/promotion paths

#### Migrating from LuckPerms to Hytale

**Challenge:** No native inheritance.

**Solution:** Flatten the hierarchy:

```java
// LuckPerms: Admin inherits Moderator
// Hytale equivalent:
groupPermissions.put("Admin", Set.of(
    "admin.*",    // Admin's own
    "mod.*"       // Inherited from Moderator
));
```

Or implement inheritance in your custom provider:

```java
@Override
public Set<String> getGroupPermissions(String group) {
    Set<String> perms = new HashSet<>(directGroupPerms.get(group));

    // Add inherited permissions
    for (String parent : groupParents.get(group)) {
        perms.addAll(getGroupPermissions(parent)); // Recursive
    }

    return perms;
}
```

---

### 4. API Comparison

#### Checking Permissions

**Hytale:**
```java
// Via module
PermissionsModule.get().hasPermission(uuid, "perm.node");

// Via player
player.hasPermission("perm.node");
```

**LuckPerms:**
```java
LuckPerms api = LuckPermsProvider.get();
User user = api.getUserManager().getUser(uuid);
boolean has = user.getCachedData().getPermissionData().checkPermission("perm.node").asBoolean();
```

**PermissionsEx:**
```java
PermissionsEx pex = PermissionsEx.getPlugin();
PermissionUser user = pex.getUser(uuid);
boolean has = user.has("perm.node");
```

#### Modifying Permissions

**Hytale:**
```java
PermissionsModule perms = PermissionsModule.get();
perms.addUserPermission(uuid, Set.of("new.perm"));
perms.addUserToGroup(uuid, "VIP");
```

**LuckPerms:**
```java
User user = api.getUserManager().getUser(uuid);
user.data().add(Node.builder("new.perm").build());
user.data().add(InheritanceNode.builder("VIP").build());
api.getUserManager().saveUser(user);
```

**PermissionsEx:**
```java
PermissionUser user = pex.getUser(uuid);
user.addPermission(null, "new.perm");
user.addGroup(pex.getGroup("VIP"));
```

---

### 5. Events

#### Hytale Events

| Event | Trigger |
|-------|---------|
| `PlayerPermissionChangeEvent.PermissionsAdded` | User perms added |
| `PlayerPermissionChangeEvent.PermissionsRemoved` | User perms removed |
| `PlayerGroupEvent.Added` | User added to group |
| `PlayerGroupEvent.Removed` | User removed from group |
| `GroupPermissionChangeEvent.Added` | Group perms added |
| `GroupPermissionChangeEvent.Removed` | Group perms removed |

#### LuckPerms Events

| Event | Equivalent |
|-------|------------|
| `NodeAddEvent` | PermissionsAdded / GroupAdded |
| `NodeRemoveEvent` | PermissionsRemoved / GroupRemoved |
| `UserDataRecalculateEvent` | (no direct equivalent) |
| `GroupDataRecalculateEvent` | (no direct equivalent) |

#### Migration Pattern

```java
// LuckPerms style
eventBus.subscribe(NodeAddEvent.class, event -> {
    if (event.isUser()) {
        handleUserChange(event.getTarget().getUniqueId());
    }
});

// Hytale equivalent
eventBus.subscribe(PlayerPermissionChangeEvent.PermissionsAdded.class, event -> {
    handleUserChange(event.getPlayerUuid());
});
eventBus.subscribe(PlayerGroupEvent.Added.class, event -> {
    handleUserChange(event.getPlayerUuid());
});
```

---

### 6. Storage Comparison

| System | File | Database | Redis | Custom |
|--------|------|----------|-------|--------|
| **Hytale** | JSON | No | No | Via Provider |
| **LuckPerms** | YAML/JSON | MySQL/PostgreSQL/etc | Yes | Yes |
| **PermissionsEx** | YAML | MySQL | No | Via backend |

---

### 7. Feature Gap Analysis

#### Features Hytale Lacks (vs LuckPerms)

| Feature | Workaround |
|---------|------------|
| Group inheritance | Flatten groups or implement in provider |
| Permission weight | Use group ordering |
| Temporary permissions | Implement with scheduler in provider |
| Contexts (world, server) | Use virtual groups or custom logic |
| Meta (prefix/suffix) | Store separately, not part of perms |
| Verbose debugging | Implement logging in provider |
| Web editor | Build separate tool |

#### Features Hytale Has Uniquely

| Feature | Description |
|---------|-------------|
| Provider pattern | Pluggable permission backends |
| Virtual groups | Game-mode based permissions |
| Event system | Built-in permission change events |
| Simple API | Straightforward interface |

---

### 8. Migration Cheat Sheet

#### From LuckPerms

```java
// LuckPerms permission node
// group.admin -> Hytale group membership
// meta.prefix.100.&c[Admin] -> Store separately

public void migrateLuckPerms(LuckPerms lp, PermissionProvider target) {
    for (User user : lp.getUserManager().getLoadedUsers()) {
        UUID uuid = user.getUniqueId();

        // Direct permissions (non-inherited)
        Set<String> perms = user.getNodes().stream()
            .filter(n -> n instanceof PermissionNode)
            .filter(n -> !n.hasExpiry())
            .map(Node::getKey)
            .collect(Collectors.toSet());
        target.addUserPermissions(uuid, perms);

        // Group memberships
        user.getNodes().stream()
            .filter(n -> n instanceof InheritanceNode)
            .map(n -> ((InheritanceNode) n).getGroupName())
            .forEach(group -> target.addUserToGroup(uuid, group));
    }
}
```

#### From PermissionsEx

```java
public void migratePex(PermissionsEx pex, PermissionProvider target) {
    for (PermissionUser user : pex.getUsers()) {
        UUID uuid = user.getIdentifier();

        // Direct permissions
        Set<String> perms = new HashSet<>(user.getOwnPermissions(null));
        target.addUserPermissions(uuid, perms);

        // Groups
        for (PermissionGroup group : user.getGroups()) {
            target.addUserToGroup(uuid, group.getName());
        }
    }
}
```

---

## Terminology Mapping

| LuckPerms | PermissionsEx | Hytale |
|-----------|---------------|--------|
| User | PermissionUser | UUID (no wrapper) |
| Group | PermissionGroup | String (group name) |
| Node | Permission | String (permission node) |
| InheritanceNode | Group assignment | Group membership |
| Track | Ladder | (not built-in) |
| Weight | Priority | (not built-in) |
| Context | World | Virtual groups |
| Meta | Options | (not built-in) |

---

*See [PERMISSIONS_SYSTEM.md](../PERMISSIONS_SYSTEM.md) for complete Hytale documentation*
