# Troubleshooting Guide

Common issues, debugging steps, and solutions for Hytale permission problems.

---

## Table of Contents

1. [Permission Not Working](#1-permission-not-working)
2. [Commands Denied](#2-commands-denied)
3. [Groups Not Applied](#3-groups-not-applied)
4. [Custom Provider Issues](#4-custom-provider-issues)
5. [File/Storage Issues](#5-filestorage-issues)
6. [Event Listener Issues](#6-event-listener-issues)
7. [Debug Techniques](#7-debug-techniques)
8. [Common Mistakes](#8-common-mistakes)

---

## 1. Permission Not Working

### Symptom
Player should have a permission but `hasPermission()` returns `false`.

### Diagnostic Steps

#### Step 1: Verify the Permission String

```java
// Check for typos, case sensitivity
String expected = "my.permission.node";
String actual = getThePermissionYoureChecking();
System.out.println("Expected: [" + expected + "]");
System.out.println("Actual:   [" + actual + "]");
System.out.println("Equal: " + expected.equals(actual));
```

**Common issues:**
- Case mismatch: `My.Permission` vs `my.permission`
- Extra spaces: `"my.perm "` vs `"my.perm"`
- Typos: `hytale.comand.*` vs `hytale.command.*`

#### Step 2: Check User's Direct Permissions

```java
UUID uuid = player.getUuid();
for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
    System.out.println("Provider: " + provider.getName());
    System.out.println("User perms: " + provider.getUserPermissions(uuid));
}
```

#### Step 3: Check User's Groups

```java
Set<String> groups = PermissionsModule.get().getGroupsForUser(uuid);
System.out.println("User groups: " + groups);

for (String group : groups) {
    for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
        System.out.println(group + " perms: " + provider.getGroupPermissions(group));
    }
}
```

#### Step 4: Test Permission Resolution

```java
// Use the built-in test command
// /perm test my.permission.node
```

### Solutions

| Problem | Solution |
|---------|----------|
| Permission not in user or groups | Add the permission |
| Wrong case | Standardize to lowercase |
| Denied by `-` prefix | Remove the denial |
| Wildcard not matching | Check wildcard is at END (`prefix.*`) |

---

## 2. Commands Denied

### Symptom
Player gets "You don't have permission" when running a command.

### Diagnostic Steps

#### Step 1: Find Required Permission

Commands require `hytale.command.<name>` or custom permissions:

```java
// Check command registration
// Look for: this.requirePermission("permission.node");
```

#### Step 2: Check NoPermissionException

The exception tells you exactly what permission is missing:

```
You don't have permission: hytale.command.gamemode
```

#### Step 3: Verify OP Status

If user should be OP:
```java
Set<String> groups = PermissionsModule.get().getGroupsForUser(uuid);
System.out.println("Is OP: " + groups.contains("OP"));
```

### Solutions

| Problem | Solution |
|---------|----------|
| Missing command permission | Add `hytale.command.<name>` |
| Not in OP group | `/op add <player>` or add to "OP" group |
| Custom provider issue | Check provider returns correct data |

---

## 3. Groups Not Applied

### Symptom
User is in a group but doesn't have group permissions.

### Diagnostic Steps

#### Step 1: Verify Group Membership

```java
UUID uuid = player.getUuid();
for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
    System.out.println(provider.getName() + " groups: " +
        provider.getGroupsForUser(uuid));
}
```

#### Step 2: Verify Group Has Permissions

```java
String groupName = "VIP";
for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
    System.out.println(provider.getName() + " " + groupName + " perms: " +
        provider.getGroupPermissions(groupName));
}
```

#### Step 3: Check Group Name Exact Match

```java
// Group names are case-sensitive!
// "VIP" != "vip" != "Vip"
```

### Solutions

| Problem | Solution |
|---------|----------|
| Group name mismatch | Standardize group names |
| Group has no permissions | Add permissions to group |
| User not in group | Add user to group |
| User has explicit groups (no Default) | Ensure user is in intended groups |

---

## 4. Custom Provider Issues

### Symptom
Custom permission provider not working as expected.

### Diagnostic Steps

#### Step 1: Verify Provider Registered

```java
List<PermissionProvider> providers = PermissionsModule.get().getProviders();
for (int i = 0; i < providers.size(); i++) {
    System.out.println("[" + i + "] " + providers.get(i).getName());
}
```

#### Step 2: Check Provider Order

First provider receives all write operations!

```java
PermissionProvider first = PermissionsModule.get().getFirstPermissionProvider();
System.out.println("First provider: " + first.getName());
// Is this your provider or vanilla?
```

#### Step 3: Test Provider Directly

```java
MyProvider myProvider = getMyProvider();
UUID testUuid = UUID.randomUUID();

// Test add
myProvider.addUserPermissions(testUuid, Set.of("test.perm"));

// Test retrieve
Set<String> perms = myProvider.getUserPermissions(testUuid);
System.out.println("Retrieved: " + perms);
assert perms.contains("test.perm") : "Add/get failed";
```

### Solutions

| Problem | Solution |
|---------|----------|
| Provider not registered | Call `addProvider()` in `start()` |
| Wrong provider order | Remove vanilla and add yours first |
| Data not persisting | Check your save/load logic |
| Thread safety issues | Use proper synchronization |

---

## 5. File/Storage Issues

### Symptom
Permissions not persisting across restarts, or file corruption.

### Diagnostic Steps

#### Step 1: Check permissions.json Exists

```bash
ls -la permissions.json
cat permissions.json
```

#### Step 2: Validate JSON Syntax

```bash
# Use jq or online JSON validator
cat permissions.json | jq .
```

#### Step 3: Check File Permissions

```bash
ls -la permissions.json
# Should be readable/writable by server process
```

### Common JSON Errors

```json
// ERROR: Trailing comma
{
  "groups": {
    "VIP": ["perm1", "perm2"],  // <- Remove this comma
  }
}

// ERROR: Single quotes
{
  "groups": {
    'VIP': ['perm1']  // <- Use double quotes
  }
}

// ERROR: Unescaped characters
{
  "groups": {
    "VIP": ["say "hello""]  // <- Escape: "say \"hello\""
  }
}
```

### Solutions

| Problem | Solution |
|---------|----------|
| File doesn't exist | Server creates on first run |
| Invalid JSON | Fix syntax errors |
| File not writable | Check file permissions |
| Data not saving | Check for exceptions in logs |

### Why Are My OP Group Custom Permissions Disappearing?

The server forcibly re-inserts the default OP and Default groups on every load using `put()` (not `putIfAbsent()`). This **destroys** any custom permissions you added to these groups in `permissions.json`.

**Example:** You add `myplugin.vip.*` to the OP group in `permissions.json`. After server restart, the OP group is reset to `["*"]` only.

**Solution:** Never add custom permissions to the OP or Default groups. Instead, create custom groups:

```json
{
  "groups": {
    "OP": ["*"],
    "Default": [],
    "SuperOP": ["*", "myplugin.admin.*"],
    "Member": ["myplugin.basic.*"]
  }
}
```

Assign users to your custom groups instead of modifying OP/Default.

---

## 6. Event Listener Issues

### Symptom
Event handlers not firing or firing multiple times.

### Diagnostic Steps

#### Step 1: Verify Subscription

```java
@Override
protected void start() {
    EventBus bus = HytaleServer.get().getEventBus();

    bus.subscribe(PlayerGroupEvent.Added.class, event -> {
        System.out.println("EVENT FIRED: " + event.getPlayerUuid() +
            " added to " + event.getGroupName());
    });
}
```

#### Step 2: Check Event Source

Events only fire when using `PermissionsModule` methods, NOT direct provider calls:

```java
// This FIRES events:
PermissionsModule.get().addUserToGroup(uuid, "VIP");

// This does NOT fire events:
provider.addUserToGroup(uuid, "VIP");
```

### Solutions

| Problem | Solution |
|---------|----------|
| Handler not called | Ensure using PermissionsModule methods |
| Called multiple times | Check not subscribing multiple times |
| Wrong event type | Check event class hierarchy |

---

## 7. Debug Techniques

### Verbose Permission Checking

```java
public boolean debugHasPermission(UUID uuid, String permission) {
    System.out.println("=== Permission Check Debug ===");
    System.out.println("UUID: " + uuid);
    System.out.println("Permission: " + permission);

    for (PermissionProvider provider : PermissionsModule.get().getProviders()) {
        System.out.println("\n--- Provider: " + provider.getName() + " ---");

        // User permissions
        Set<String> userPerms = provider.getUserPermissions(uuid);
        System.out.println("User perms: " + userPerms);

        Boolean userResult = PermissionsModule.hasPermission(userPerms, permission);
        System.out.println("User check result: " + userResult);
        if (userResult != null) {
            System.out.println("MATCH FOUND at user level: " + userResult);
            return userResult;
        }

        // Group permissions
        for (String group : provider.getGroupsForUser(uuid)) {
            Set<String> groupPerms = provider.getGroupPermissions(group);
            System.out.println("Group [" + group + "] perms: " + groupPerms);

            Boolean groupResult = PermissionsModule.hasPermission(groupPerms, permission);
            System.out.println("Group [" + group + "] check result: " + groupResult);
            if (groupResult != null) {
                System.out.println("MATCH FOUND at group level: " + groupResult);
                return groupResult;
            }
        }
    }

    System.out.println("No match found, returning default: false");
    return false;
}
```

### Permission Audit Command

```java
public class PermAuditCommand extends CommandBase {
    private final RequiredArg<UUID> uuidArg = ...;
    private final RequiredArg<String> permArg = ...;

    @Override
    protected void executeSync(CommandContext context) {
        UUID uuid = uuidArg.get(context);
        String perm = permArg.get(context);

        context.sendMessage(Message.raw("=== Audit: " + perm + " ==="));

        // Check each provider
        for (PermissionProvider p : PermissionsModule.get().getProviders()) {
            context.sendMessage(Message.raw("[" + p.getName() + "]"));

            // Direct
            if (p.getUserPermissions(uuid).contains(perm)) {
                context.sendMessage(Message.raw("  FOUND in user direct"));
            }
            if (p.getUserPermissions(uuid).contains("-" + perm)) {
                context.sendMessage(Message.raw("  DENIED in user direct"));
            }

            // Groups
            for (String g : p.getGroupsForUser(uuid)) {
                if (p.getGroupPermissions(g).contains(perm)) {
                    context.sendMessage(Message.raw("  FOUND in group: " + g));
                }
            }
        }

        // Final result
        boolean result = PermissionsModule.get().hasPermission(uuid, perm);
        context.sendMessage(Message.raw("Result: " + result));
    }
}
```

---

## 8. Common Mistakes

### Mistake 1: Case Sensitivity

```java
// WRONG: Different cases
perms.add("My.Permission");
hasPermission(uuid, "my.permission"); // false!

// RIGHT: Consistent lowercase
perms.add("my.permission");
hasPermission(uuid, "my.permission"); // true
```

### Mistake 2: Wildcard Position

```java
// WRONG: Wildcard in middle
perms.add("my.*.permission"); // Literal asterisk, not wildcard

// RIGHT: Wildcard at end
perms.add("my.permission.*"); // Matches my.permission.anything
```

### Mistake 3: Expecting -* to Allow Specific

`-*` is checked before exact matches in the resolution algorithm, so `-*` blocks ALL permissions regardless of specific grants in the same set. Conversely, `*` is checked before `-*`, so if BOTH exist, `*` wins.

```java
// WRONG: Expecting specific to override -*
perms.addAll(Set.of("-*", "allowed.perm"));
hasPermission(uuid, "allowed.perm"); // false! -* is checked before exact match

// ALSO NOTE: * beats -*
perms.addAll(Set.of("*", "-*"));
hasPermission(uuid, "anything"); // true! * is checked before -*

// RIGHT: Don't use -* if you want specific grants
perms.add("allowed.perm");
// Simply don't grant anything else
```

### Mistake 4: Direct Provider Calls

```java
// WRONG: Bypasses events
provider.addUserPermissions(uuid, perms);

// RIGHT: Uses module (fires events)
PermissionsModule.get().addUserPermission(uuid, perms);
```

### Mistake 5: Modifying Returned Sets

```java
// WRONG: Modifying unmodifiable set
Set<String> perms = provider.getUserPermissions(uuid);
perms.add("new.perm"); // UnsupportedOperationException!

// RIGHT: Create new set for modifications
Set<String> perms = new HashSet<>(provider.getUserPermissions(uuid));
perms.add("new.perm");
provider.addUserPermissions(uuid, Set.of("new.perm"));
```

### Mistake 6: Assuming Default Group

```java
// WRONG: Assuming user has Default permissions
// User with explicit groups does NOT get Default

// RIGHT: Check actual groups
Set<String> groups = module.getGroupsForUser(uuid);
if (groups.isEmpty() || groups.equals(Set.of("Default"))) {
    // User is in default state
}
```

### Mistake 7: Thread Unsafe Custom Provider

```java
// WRONG: Not thread safe
private Map<UUID, Set<String>> perms = new HashMap<>();

// RIGHT: Thread safe
private Map<UUID, Set<String>> perms = new ConcurrentHashMap<>();
// Or use ReadWriteLock like vanilla
```

---

## Quick Fixes Checklist

- [ ] Permission string matches exactly (case, spelling)
- [ ] User is in correct group
- [ ] Group has the permission
- [ ] No denial (`-`) blocking the permission
- [ ] Wildcard at END of pattern (`prefix.*`)
- [ ] Custom provider is registered
- [ ] Using PermissionsModule methods (not direct provider)
- [ ] permissions.json is valid JSON
- [ ] Server has write access to permissions.json

---

*See [PERMISSIONS_SYSTEM.md](PERMISSIONS_SYSTEM.md) for complete documentation*
