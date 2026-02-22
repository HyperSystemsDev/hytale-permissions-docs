# Migration Guide

This guide covers migrating from vanilla Hytale permissions to custom providers, and importing existing permission data.

---

## Table of Contents

1. [From Vanilla to Custom Provider](#1-from-vanilla-to-custom-provider)
2. [Importing permissions.json](#2-importing-permissionsjson)
3. [Database Schema Design](#3-database-schema-design)
4. [Migration Strategies](#4-migration-strategies)
5. [Rollback Plan](#5-rollback-plan)

---

## 1. From Vanilla to Custom Provider

### Understanding the Vanilla System

The vanilla `HytalePermissionsProvider`:
- Stores data in `permissions.json`
- Uses file-based persistence
- Has two default groups: `OP` (with `*`) and `Default` (empty)
- Users without groups get `Default` automatically

### Migration Steps

#### Step 1: Export Current Data

```java
public class DataExporter {
    public void exportVanillaData() {
        HytalePermissionsProvider vanilla = (HytalePermissionsProvider)
            PermissionsModule.get().getFirstPermissionProvider();

        // Read the permissions.json directly
        Path permFile = Paths.get("permissions.json");
        String json = Files.readString(permFile);

        // Parse and save to your format
        saveToDatabase(parseJson(json));
    }
}
```

#### Step 2: Implement Your Provider

See [CustomProviderTemplate.java](../examples/CustomProviderTemplate.java) for a complete template.

#### Step 3: Register Your Provider

**Option A: Add alongside vanilla (safest)**
```java
@Override
protected void start() {
    // Your provider added after vanilla
    PermissionsModule.get().addProvider(myProvider);
}
```

**Option B: Replace vanilla entirely**
```java
@Override
protected void start() {
    PermissionsModule perms = PermissionsModule.get();

    // Remove the vanilla provider
    PermissionProvider vanilla = perms.getFirstPermissionProvider();
    perms.removeProvider(vanilla);

    // Add your provider as the first (and only)
    perms.addProvider(myProvider);
}
```

#### Step 4: Test Thoroughly

```java
// Verify permissions work
UUID testUser = UUID.fromString("...");
boolean shouldBeTrue = PermissionsModule.get().hasPermission(testUser, "expected.perm");
assert shouldBeTrue : "Migration failed: expected permission not found";
```

---

## 2. Importing permissions.json

### JSON Structure

```json
{
  "users": {
    "550e8400-e29b-41d4-a716-446655440000": {
      "permissions": ["perm1", "perm2"],
      "groups": ["VIP", "Builder"]
    }
  },
  "groups": {
    "OP": ["*"],
    "Default": [],
    "VIP": ["vip.feature", "vip.chat"],
    "Builder": ["build.*"]
  }
}
```

### Java Import Code

```java
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PermissionImporter {

    private final PermissionProvider targetProvider;

    public PermissionImporter(PermissionProvider target) {
        this.targetProvider = target;
    }

    public ImportResult importFromJson(Path jsonFile) throws IOException {
        String content = Files.readString(jsonFile);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        int usersImported = 0;
        int groupsImported = 0;

        // Import groups first
        if (root.has("groups")) {
            JsonObject groups = root.getAsJsonObject("groups");
            for (var entry : groups.entrySet()) {
                String groupName = entry.getKey();
                Set<String> perms = new HashSet<>();
                entry.getValue().getAsJsonArray().forEach(e -> perms.add(e.getAsString()));

                if (!perms.isEmpty()) {
                    targetProvider.addGroupPermissions(groupName, perms);
                }
                groupsImported++;
            }
        }

        // Import users
        if (root.has("users")) {
            JsonObject users = root.getAsJsonObject("users");
            for (var entry : users.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                JsonObject userData = entry.getValue().getAsJsonObject();

                // Import user permissions
                if (userData.has("permissions")) {
                    Set<String> perms = new HashSet<>();
                    userData.getAsJsonArray("permissions").forEach(e -> perms.add(e.getAsString()));
                    if (!perms.isEmpty()) {
                        targetProvider.addUserPermissions(uuid, perms);
                    }
                }

                // Import user groups
                if (userData.has("groups")) {
                    userData.getAsJsonArray("groups").forEach(e -> {
                        targetProvider.addUserToGroup(uuid, e.getAsString());
                    });
                }
                usersImported++;
            }
        }

        return new ImportResult(usersImported, groupsImported);
    }

    public record ImportResult(int users, int groups) {}
}
```

> **Warning: OP/Default Group Data Loss**
>
> Any custom permissions on the OP or Default groups in the source `permissions.json` will be lost after import + server restart. The server forcibly resets OP to `["*"]` and Default to `[]` on every load. Before migrating, move any custom permissions from OP/Default to custom group names (e.g., `"SuperOP"`, `"Member"`).

### Command Integration

```java
public class ImportCommand extends CommandBase {
    public ImportCommand() {
        super("import", "Import permissions from vanilla JSON");
        this.requirePermission("hyperperms.admin.import");
    }

    @Override
    protected void executeSync(CommandContext context) {
        Path jsonFile = Paths.get("permissions.json");
        if (!Files.exists(jsonFile)) {
            context.sendMessage(Message.raw("permissions.json not found!"));
            return;
        }

        try {
            PermissionImporter importer = new PermissionImporter(myProvider);
            var result = importer.importFromJson(jsonFile);
            context.sendMessage(Message.raw(
                "Imported " + result.users() + " users and " + result.groups() + " groups"
            ));
        } catch (Exception e) {
            context.sendMessage(Message.raw("Import failed: " + e.getMessage()));
        }
    }
}
```

---

## 3. Database Schema Design

### Recommended SQL Schema

```sql
-- Groups table
CREATE TABLE permission_groups (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Group permissions
CREATE TABLE group_permissions (
    group_id INT REFERENCES permission_groups(id) ON DELETE CASCADE,
    permission VARCHAR(255) NOT NULL,
    PRIMARY KEY (group_id, permission)
);

-- Users (UUIDs)
CREATE TABLE permission_users (
    uuid UUID PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User direct permissions
CREATE TABLE user_permissions (
    user_uuid UUID REFERENCES permission_users(uuid) ON DELETE CASCADE,
    permission VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_uuid, permission)
);

-- User group memberships
CREATE TABLE user_groups (
    user_uuid UUID REFERENCES permission_users(uuid) ON DELETE CASCADE,
    group_id INT REFERENCES permission_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_uuid, group_id)
);

-- Indexes for common queries
CREATE INDEX idx_user_groups_user ON user_groups(user_uuid);
CREATE INDEX idx_user_groups_group ON user_groups(group_id);
```

### NoSQL Alternative (MongoDB)

```javascript
// Users collection
{
    "_id": "550e8400-e29b-41d4-a716-446655440000",
    "permissions": ["perm1", "perm2"],
    "groups": ["VIP", "Builder"],
    "createdAt": ISODate("2024-01-01T00:00:00Z")
}

// Groups collection
{
    "_id": "VIP",
    "permissions": ["vip.feature", "vip.chat.color"],
    "createdAt": ISODate("2024-01-01T00:00:00Z")
}
```

---

## 4. Migration Strategies

### Strategy A: Parallel Operation (Recommended)

Run both vanilla and custom providers simultaneously during transition.

```
Week 1-2: Both providers active
          - Vanilla is primary (index 0)
          - Custom reads from database
          - All writes go to vanilla (for safety)

Week 3-4: Switch primary
          - Custom becomes index 0
          - Vanilla as backup
          - Monitor for issues

Week 5+:  Remove vanilla
          - Custom provider only
          - Keep vanilla backup file
```

**Pros:** Safe, reversible, no downtime
**Cons:** Longer migration period, potential data divergence

### Strategy B: Full Cutover

Complete switch at a scheduled maintenance window.

```
1. Announce maintenance window
2. Stop server
3. Export vanilla data
4. Import to database
5. Configure custom provider
6. Start server
7. Test critical permissions
8. Done (or rollback)
```

**Pros:** Clean cut, no divergence
**Cons:** Requires downtime, higher risk

### Strategy C: Shadow Mode

Custom provider shadows vanilla (read-only) before taking over.

```java
public class ShadowProvider implements PermissionProvider {
    private final CustomProvider custom;
    private final Logger logger;

    @Override
    public Set<String> getUserPermissions(UUID uuid) {
        Set<String> customResult = custom.getUserPermissions(uuid);

        // Also check vanilla and log differences
        Set<String> vanillaResult = getVanillaProvider().getUserPermissions(uuid);
        if (!customResult.equals(vanillaResult)) {
            logger.warn("Mismatch for " + uuid + ": custom=" + customResult + ", vanilla=" + vanillaResult);
        }

        return customResult;
    }
    // ... other methods
}
```

**Pros:** Identifies issues before cutover
**Cons:** More complex, performance overhead

---

## 5. Rollback Plan

### Backup Before Migration

```bash
# Backup vanilla permissions
cp permissions.json permissions.json.backup.$(date +%Y%m%d)

# If using database, also dump
pg_dump -t 'permission*' mydb > permissions_backup.sql
```

### Quick Rollback Steps

1. **Stop the server**

2. **Restore vanilla config**
   ```java
   // In plugin start, skip custom provider registration
   if (System.getProperty("hyperperms.rollback") != null) {
       logger.warn("Rollback mode - using vanilla permissions");
       return; // Don't register custom provider
   }
   ```

3. **Restore permissions.json if needed**
   ```bash
   cp permissions.json.backup permissions.json
   ```

4. **Start server with rollback flag**
   ```bash
   java -Dhyperperms.rollback=true -jar server.jar
   ```

### Rollback Verification

```java
// Verify vanilla is in control
PermissionsModule perms = PermissionsModule.get();
assert perms.getProviders().size() == 1 : "Should only have vanilla";
assert perms.getFirstPermissionProvider() instanceof HytalePermissionsProvider : "Should be vanilla";
```

---

## Checklist

### Pre-Migration

- [ ] Backup `permissions.json`
- [ ] Document current permission setup
- [ ] Test custom provider in dev environment
- [ ] Create rollback procedure
- [ ] Schedule maintenance window (if needed)

### During Migration

- [ ] Export vanilla data
- [ ] Import to new backend
- [ ] Verify import counts match
- [ ] Register custom provider
- [ ] Test critical permissions (OP, admin, etc.)

### Post-Migration

- [ ] Monitor for permission errors
- [ ] Check logs for access denied
- [ ] Verify all player ranks working
- [ ] Remove vanilla provider (after confidence period)
- [ ] Archive backup files

---

*See [PERMISSIONS_SYSTEM.md](../PERMISSIONS_SYSTEM.md) for system documentation*
