# Best Practices

> **Design patterns, naming conventions, and guidelines for Hytale permissions**

---

## Table of Contents

1. [Permission Naming Conventions](#permission-naming-conventions)
2. [Provider Implementation Patterns](#provider-implementation-patterns)
3. [Wildcard Usage Guidelines](#wildcard-usage-guidelines)
4. [Security Considerations](#security-considerations)
5. [Performance Optimization](#performance-optimization)
6. [Common Anti-Patterns](#common-anti-patterns)
7. [Testing Strategies](#testing-strategies)

---

## Permission Naming Conventions

### Structure

Follow the hierarchical pattern:

```
<namespace>.<category>.<subcategory>.<action>
```

### Naming Rules

| Rule | Good | Bad |
|------|------|-----|
| Use lowercase | `myplugin.user.ban` | `MyPlugin.User.Ban` |
| Use dots as separators | `myplugin.user.ban` | `myplugin_user_ban` |
| Be specific | `myplugin.chat.color.red` | `myplugin.redchat` |
| Use action verbs | `myplugin.item.give` | `myplugin.item.giver` |
| Keep it readable | `myplugin.warp.create` | `myplugin.wc` |

### Recommended Actions

| Action | Usage |
|--------|-------|
| `.use` | Basic feature access |
| `.create` | Create new resources |
| `.edit` / `.modify` | Change existing resources |
| `.delete` / `.remove` | Remove resources |
| `.list` / `.view` | Read-only access |
| `.admin` | Administrative functions |
| `.bypass` | Skip restrictions |
| `.notify` | Receive notifications |

### Examples

```
# Chat plugin
chat.color.use
chat.format.bold
chat.mention.everyone
chat.admin.mute

# Economy plugin
economy.balance.view
economy.balance.view.others
economy.pay
economy.admin.set

# Teleport plugin
teleport.home.use
teleport.home.set
teleport.home.limit.5
teleport.warp.use
teleport.warp.create
teleport.admin.teleportall
```

---

## Provider Implementation Patterns

### Thread Safety

**Always implement thread-safe storage:**

```java
public class MyProvider implements PermissionProvider {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<UUID, Set<String>> userPermissions = new ConcurrentHashMap<>();

    @Override
    public Set<String> getUserPermissions(@Nonnull UUID uuid) {
        lock.readLock().lock();
        try {
            Set<String> perms = userPermissions.get(uuid);
            return perms != null ? Set.copyOf(perms) : Set.of();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        lock.writeLock().lock();
        try {
            userPermissions.computeIfAbsent(uuid, k -> new HashSet<>())
                          .addAll(permissions);
            saveAsync();  // Non-blocking save
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

### Return Immutable Collections

```java
// Good - returns unmodifiable view
@Override
public Set<String> getUserPermissions(@Nonnull UUID uuid) {
    return Collections.unmodifiableSet(permissions.getOrDefault(uuid, Set.of()));
}

// Bad - returns mutable collection
@Override
public Set<String> getUserPermissions(@Nonnull UUID uuid) {
    return permissions.get(uuid);  // Caller can modify!
}
```

### Handle Missing Data Gracefully

```java
@Override
public Set<String> getGroupsForUser(@Nonnull UUID uuid) {
    Set<String> groups = userGroups.get(uuid);
    if (groups == null || groups.isEmpty()) {
        return Set.of("Default");  // Fallback to default group
    }
    return Collections.unmodifiableSet(groups);
}
```

### Validate Input

```java
@Override
public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
    Objects.requireNonNull(uuid, "UUID cannot be null");
    Objects.requireNonNull(permissions, "Permissions cannot be null");

    // Filter invalid permissions
    Set<String> valid = permissions.stream()
        .filter(p -> p != null && !p.isBlank())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());

    if (!valid.isEmpty()) {
        // Store permissions
    }
}
```

---

## Wildcard Usage Guidelines

### When to Use Wildcards

| Use Case | Recommended | Example |
|----------|-------------|---------|
| Admin groups | Yes | `OP` group gets `*` |
| Feature bundles | Sometimes | `myplugin.vip.*` for VIP features |
| Category grants | Sometimes | `myplugin.chat.*` for all chat perms |
| Individual users | Rarely | Prefer explicit permissions |

### Wildcard Hierarchy

```
*                          # Everything
hytale.*                   # All Hytale permissions
hytale.command.*           # All command permissions
hytale.command.gamemode.*  # All gamemode subcommands
```

### Negation Strategy

Use negation sparingly and intentionally:

```java
// Grant everything except dangerous commands
Set<String> modPerms = Set.of(
    "*",
    "-hytale.command.op.*",
    "-hytale.command.perm.group.*"
);
```

### Resolution Order Matters

```
User permissions → Group permissions → Virtual groups → Default

// Example: User has "-fly" but group has "fly"
// Result: DENIED (user perms checked first)
```

---

## Security Considerations

### Principle of Least Privilege

```java
// Bad - overly permissive
addGroupPermissions("Helper", Set.of("*"));

// Good - explicit permissions only
addGroupPermissions("Helper", Set.of(
    "myplugin.help.respond",
    "myplugin.ticket.view",
    "myplugin.chat.color.basic"
));
```

### Protect Sensitive Operations

```java
// Critical permissions should be explicit, never wildcarded
private static final Set<String> SENSITIVE_PERMISSIONS = Set.of(
    "myplugin.admin.database.wipe",
    "myplugin.admin.config.reset",
    "myplugin.admin.user.delete"
);

public boolean hasPermission(UUID uuid, String perm) {
    if (SENSITIVE_PERMISSIONS.contains(perm)) {
        // Only check exact match, no wildcards
        return getUserPermissions(uuid).contains(perm);
    }
    return normalCheck(uuid, perm);
}
```

### Audit Permission Changes

```java
@Override
public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
    // Log the change
    logger.info("Permissions added to {}: {}", uuid, permissions);

    // Actually add permissions
    doAddPermissions(uuid, permissions);
}
```

### Validate Permission Sources

```java
// Only accept permission changes from trusted sources
public void addPermissionFromCommand(CommandSender sender, UUID target, String permission) {
    if (!sender.hasPermission("myplugin.admin.permissions.grant")) {
        throw new NoPermissionException("myplugin.admin.permissions.grant");
    }

    // Prevent privilege escalation
    if (permission.equals("*") && !sender.hasPermission("*")) {
        throw new IllegalArgumentException("Cannot grant permissions you don't have");
    }

    permissionsModule.addUserPermission(target, Set.of(permission));
}
```

---

## Performance Optimization

### Cache Permission Results

```java
public class CachingPermissionChecker {
    private final Cache<PermissionKey, Boolean> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(10_000)
        .build();

    public boolean hasPermission(UUID uuid, String permission) {
        return cache.get(new PermissionKey(uuid, permission),
            key -> computePermission(key.uuid(), key.permission()));
    }

    // Invalidate on permission changes
    public void onPermissionChange(UUID uuid) {
        cache.asMap().keySet().removeIf(key -> key.uuid().equals(uuid));
    }
}
```

### Lazy Load Group Permissions

```java
public class LazyGroupProvider {
    private final Map<String, Supplier<Set<String>>> groupLoaders = new HashMap<>();
    private final Map<String, Set<String>> loadedGroups = new ConcurrentHashMap<>();

    @Override
    public Set<String> getGroupPermissions(String group) {
        return loadedGroups.computeIfAbsent(group, g -> {
            Supplier<Set<String>> loader = groupLoaders.get(g);
            return loader != null ? loader.get() : Set.of();
        });
    }
}
```

### Batch Operations

```java
// Bad - multiple events, multiple saves
for (String perm : permissions) {
    permissionsModule.addUserPermission(uuid, Set.of(perm));
}

// Good - single event, single save
permissionsModule.addUserPermission(uuid, permissions);
```

---

## Common Anti-Patterns

### 1. Permission Check in Event Handler Loop

```java
// Bad - checking permission for every block break
@Subscribe
public void onBlockBreak(BlockBreakEvent event) {
    if (!player.hasPermission("build.block." + block.getType().name())) {
        event.cancel();
    }
}

// Good - cache result or use category permission
@Subscribe
public void onBlockBreak(BlockBreakEvent event) {
    Boolean cached = buildPermCache.get(player.getUuid());
    if (cached == null) {
        cached = player.hasPermission("build.*");
        buildPermCache.put(player.getUuid(), cached);
    }
    if (!cached) {
        event.cancel();
    }
}
```

### 2. Modifying Permissions in Permission Events

```java
// Bad - can cause infinite loop
@Subscribe
public void onPermissionAdded(PermissionsAdded event) {
    if (event.getAddedPermissions().contains("vip")) {
        // This fires another event!
        permissionsModule.addUserPermission(event.getPlayerUuid(),
            Set.of("vip.feature1", "vip.feature2"));
    }
}

// Good - use flags or delayed execution
@Subscribe
public void onPermissionAdded(PermissionsAdded event) {
    if (event.getAddedPermissions().contains("vip")) {
        scheduler.runLater(() -> {
            grantVipPerks(event.getPlayerUuid());
        }, 1);
    }
}
```

### 3. Hardcoding Permission Strings

```java
// Bad - scattered magic strings
if (player.hasPermission("myplugin.admin.ban")) { ... }
if (player.hasPermission("myplugin.admin.ban")) { ... }  // Typo risk

// Good - centralized constants
public final class Permissions {
    public static final String ADMIN_BAN = "myplugin.admin.ban";
    public static final String ADMIN_KICK = "myplugin.admin.kick";
}

if (player.hasPermission(Permissions.ADMIN_BAN)) { ... }
```

### 4. Ignoring Default Values

```java
// Bad - assumes false
if (player.hasPermission("feature.use")) {
    enableFeature();
}

// Good - explicit about defaults for features that should be on by default
if (player.hasPermission("feature.use", true)) {
    enableFeature();
}
```

### 5. Not Cleaning Up Empty Sets

```java
// Bad - accumulates empty sets
@Override
public void removeUserPermissions(UUID uuid, Set<String> permissions) {
    userPermissions.get(uuid).removeAll(permissions);
    // Empty set still in map
}

// Good - clean up empty entries
@Override
public void removeUserPermissions(UUID uuid, Set<String> permissions) {
    Set<String> current = userPermissions.get(uuid);
    if (current != null) {
        current.removeAll(permissions);
        if (current.isEmpty()) {
            userPermissions.remove(uuid);
        }
    }
}
```

---

## Testing Strategies

### Unit Test Permission Resolution

```java
@Test
void wildcardGrantsPermission() {
    Set<String> nodes = Set.of("*");
    assertTrue(PermissionsModule.hasPermission(nodes, "any.permission"));
}

@Test
void negationDeniesPermission() {
    Set<String> nodes = Set.of("*", "-admin.dangerous");
    assertFalse(PermissionsModule.hasPermission(nodes, "admin.dangerous"));
    assertTrue(PermissionsModule.hasPermission(nodes, "admin.safe"));
}

@Test
void prefixWildcardWorks() {
    Set<String> nodes = Set.of("plugin.feature.*");
    assertTrue(PermissionsModule.hasPermission(nodes, "plugin.feature.one"));
    assertTrue(PermissionsModule.hasPermission(nodes, "plugin.feature.two"));
    assertFalse(PermissionsModule.hasPermission(nodes, "plugin.other"));
}
```

### Integration Test Provider

```java
@Test
void providerPersistsPermissions() {
    MyProvider provider = new MyProvider(testDatabase);
    UUID uuid = UUID.randomUUID();

    provider.addUserPermissions(uuid, Set.of("test.perm"));

    // Recreate provider (simulates restart)
    MyProvider reloaded = new MyProvider(testDatabase);

    assertTrue(reloaded.getUserPermissions(uuid).contains("test.perm"));
}
```

### Load Test Concurrent Access

```java
@Test
void handlesConurrentAccess() throws Exception {
    MyProvider provider = new MyProvider();
    UUID uuid = UUID.randomUUID();
    int threads = 10;
    int iterations = 1000;

    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads * iterations);

    for (int i = 0; i < threads * iterations; i++) {
        final int perm = i;
        executor.submit(() -> {
            try {
                provider.addUserPermissions(uuid, Set.of("perm." + perm));
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(30, TimeUnit.SECONDS);
    assertEquals(threads * iterations, provider.getUserPermissions(uuid).size());
}
```

---

*Best practices for Hytale permissions development*
