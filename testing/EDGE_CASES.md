# Permission System Edge Cases & Expected Behavior

This document defines test cases and expected behavior for edge cases in the Hytale permission system. Use these for testing custom providers and understanding system behavior.

---

## Test Case Format

Each test case includes:
- **Setup**: Initial state
- **Action**: What is being checked
- **Expected**: The expected result
- **Rationale**: Why this is the expected behavior

---

## 1. Wildcard Conflicts

### 1.1 Global Grant vs Specific Deny

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["*", "-hytale.command.ban"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "hytale.command.ban")`

**Expected:** `true`

**Rationale:** The `*` wildcard is checked BEFORE specific permissions. Since `*` matches first, permission is granted. The `-hytale.command.ban` is never reached.

**Resolution order:**
1. Check `*` → MATCH → return TRUE
2. `-hytale.command.ban` never checked

---

### 1.2 Specific Grant vs Global Deny

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["-*", "hytale.command.help"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "hytale.command.help")`

**Expected:** `false`

**Rationale:** `-*` is checked before exact permissions. Deny-all matches first.

**Implication:** To grant specific permissions while denying others, do NOT use `-*`. Instead, simply don't grant the permissions you want denied.

---

### 1.3 Prefix Grant vs Specific Deny (Same Prefix)

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["hytale.command.*", "-hytale.command.ban"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "hytale.command.ban")`

**Expected:** `false`

**Rationale:** The exact deny (`-hytale.command.ban`) is checked BEFORE prefix wildcards (`hytale.command.*`). Resolution order:
1. `*` → not present
2. `-*` → not present
3. `hytale.command.ban` (exact grant) → not present
4. `-hytale.command.ban` (exact deny) → **MATCH → return FALSE**
5. `hytale.command.*` (prefix wildcard) → never reached

---

### 1.4 Nested Wildcard Precedence

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["hytale.*", "-hytale.command.*"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "hytale.command.ban")`

**Expected:** `true`

**Rationale:** Wildcard checking builds the prefix incrementally:
1. Check `hytale.*` → MATCH → return TRUE
2. `hytale.command.*` never reached

The shorter wildcard is checked first.

---

## 2. Group Inheritance

### 2.1 User Permission vs Group Permission

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["-fly.enabled"],
      "groups": ["VIP"]
    }
  },
  "groups": {
    "VIP": ["fly.enabled", "vip.chat"]
  }
}
```

**Action:** `hasPermission(uuid-1, "fly.enabled")`

**Expected:** `false`

**Rationale:** User direct permissions are checked BEFORE group permissions. The deny on user level takes precedence.

---

### 2.2 First Group vs Second Group

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "groups": ["Moderator", "Builder"]
    }
  },
  "groups": {
    "Moderator": ["-build.enabled"],
    "Builder": ["build.enabled"]
  }
}
```

**Action:** `hasPermission(uuid-1, "build.enabled")`

**Expected:** **NONDETERMINISTIC** — result depends on `HashSet` iteration order

**Rationale:** Groups are checked in iteration order, but `getGroupsForUser()` returns a `HashSet`, whose iteration order is undefined and can change between JVM runs.

> **CRITICAL WARNING:** This test case demonstrates a real-world footgun. If a user belongs to multiple groups with conflicting permissions, the outcome is unpredictable. **Do not rely on group iteration order for permission resolution.** Either:
> - Ensure groups assigned to the same user never have conflicting permissions
> - Use explicit user-level permissions for overrides
> - Implement deterministic ordering in a custom provider

---

### 2.3 Empty Group

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "groups": ["EmptyGroup"]
    }
  },
  "groups": {
    "EmptyGroup": []
  }
}
```

**Action:** `hasPermission(uuid-1, "any.permission")`

**Expected:** `false` (default)

**Rationale:** Empty permission set means no matches. Falls through to default.

---

## 3. Default Group Behavior

### 3.1 User Without Explicit Groups

**Setup:**
```json
{
  "groups": {
    "Default": ["default.perm"]
  }
}
```
(User uuid-1 not in users object)

**Action:** `hasPermission(uuid-1, "default.perm")`

**Expected:** `true`

**Rationale:** `getGroupsForUser()` returns `["Default"]` when user has no explicit groups.

---

### 3.2 User With Explicit Groups (No Default)

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "groups": ["VIP"]
    }
  },
  "groups": {
    "VIP": ["vip.perm"],
    "Default": ["default.perm"]
  }
}
```

**Action:** `hasPermission(uuid-1, "default.perm")`

**Expected:** `false`

**Rationale:** When user has explicit groups, they do NOT automatically get "Default" group. Only their explicit groups are used.

---

### 3.3 OP/Default Group Custom Permissions Lost on Restart

**Setup:**
```json
{
  "groups": {
    "OP": ["*", "myplugin.admin.*"],
    "Default": ["myplugin.basic.use"]
  }
}
```

**Action:**
1. Before restart: `hasPermission(op-user, "myplugin.admin.reload")` → Check result
2. Restart server
3. After restart: `hasPermission(op-user, "myplugin.admin.reload")` → Check result

**Expected:**
- Before restart: `true` (OP has `*` and `myplugin.admin.*`)
- After restart: `true` (OP still has `*`, but `myplugin.admin.*` was silently removed)

**Rationale:** The server forcibly re-inserts DEFAULT_GROUPS using `put()` on load, replacing any custom OP permissions with just `["*"]` and Default with `[]`. In this specific case, the OP user still passes because `*` grants everything — but any custom permissions added to Default are completely lost.

**The real danger:** If you added permissions to the Default group (e.g., `myplugin.basic.use`), those are silently lost on restart. Users in the Default group will lose access without any error.

---

## 4. Multiple Providers

### 4.1 First Provider Denies, Second Grants

**Setup:**
- Provider[0]: User has `-some.perm`
- Provider[1]: User has `some.perm`

**Action:** `hasPermission(uuid-1, "some.perm")`

**Expected:** `false`

**Rationale:** Provider[0] is checked first. Denial is found and returned immediately.

---

### 4.2 First Provider Empty, Second Grants

**Setup:**
- Provider[0]: User has no permissions or groups
- Provider[1]: User has `some.perm`

**Action:** `hasPermission(uuid-1, "some.perm")`

**Expected:** `true`

**Rationale:** Provider[0] returns NULL (no match), so Provider[1] is checked.

---

### 4.3 Groups Aggregated Across Providers

**Setup:**
- Provider[0]: User in group "A"
- Provider[1]: User in group "B"

**Action:** `getGroupsForUser(uuid-1)`

**Expected:** `["A", "B"]` (combined from both providers)

**Rationale:** `PermissionsModule.getGroupsForUser()` aggregates groups from ALL providers.

---

## 5. Virtual Groups

### 5.1 Virtual Group Permissions

**Setup:**
- User in group "Creative"
- Virtual groups: `{"Creative": ["hytale.editor.builderTools"]}`
- Group "Creative" has no direct permissions

**Action:** `hasPermission(uuid-1, "hytale.editor.builderTools")`

**Expected:** `true`

**Rationale:** After checking group's own permissions, virtual group permissions are checked.

---

### 5.2 Virtual Group vs Direct Group Permission

**Setup:**
- User in group "Creative"
- Group "Creative" permissions: `["-hytale.editor.builderTools"]`
- Virtual groups: `{"Creative": ["hytale.editor.builderTools"]}`

**Action:** `hasPermission(uuid-1, "hytale.editor.builderTools")`

**Expected:** `false`

**Rationale:** Direct group permissions are checked BEFORE virtual group permissions.

---

## 6. Permission String Edge Cases

### 6.1 Permission with Leading/Trailing Dots

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": [".weird.perm."]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, ".weird.perm.")`

**Expected:** `true`

**Rationale:** Exact string matching. The dots are part of the permission string.

**Note:** This is technically valid but unconventional. Avoid leading/trailing dots.

---

### 6.2 Case Sensitivity

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["My.Permission"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "my.permission")`

**Expected:** `false`

**Rationale:** Permission checks are CASE-SENSITIVE. "My.Permission" ≠ "my.permission"

**Best Practice:** Always use lowercase permission nodes.

---

### 6.3 Empty Permission String

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": [""]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "")`

**Expected:** `true`

**Rationale:** Empty string matches empty string exactly.

**Note:** This is a degenerate case. Avoid empty permission strings.

---

### 6.4 Wildcard as Permission Name

**Setup:**
```json
{
  "users": {
    "uuid-1": {
      "permissions": ["my.*.perm"]
    }
  }
}
```

**Action:** `hasPermission(uuid-1, "my.anything.perm")`

**Expected:** `false`

**Rationale:** The `*` in `my.*.perm` is NOT a wildcard pattern. It's the literal character `*`. Wildcards only work as `*` alone or `prefix.*` at the END.

---

## 7. Concurrent Modification

### 7.1 Permission Added During Check

**Scenario:** Thread A is in the middle of `hasPermission()` checking providers, Thread B adds a permission.

**Expected Behavior:** Thread-safe operation. The ReadWriteLock in HytalePermissionsProvider ensures:
- Thread A sees a consistent snapshot
- Thread B's changes may or may not be visible depending on timing

**Implication:** Don't rely on real-time permission updates during a single check.

---

### 7.2 Provider Added During Check

**Scenario:** `addProvider()` called while `hasPermission()` is iterating providers.

**Expected Behavior:** Safe due to `CopyOnWriteArrayList`. The new provider may or may not be included in the current iteration.

---

## 8. Null and Invalid Input

### 8.1 Null UUID

**Action:** `hasPermission(null, "some.perm")`

**Expected:** `NullPointerException` (due to `@Nonnull` annotation)

---

### 8.2 Null Permission String

**Action:** `hasPermission(uuid, null)`

**Expected:** `NullPointerException` (due to `@Nonnull` annotation)

---

### 8.3 Empty Permission Set in Add

**Action:** `addUserPermission(uuid, Set.of())`

**Expected:** No-op. Empty set adds nothing.

---

## Test Matrix Summary

| Test Case | Input | Expected | Key Insight |
|-----------|-------|----------|-------------|
| Global * vs specific - | `["*", "-x"]` check x | TRUE | * checked first |
| Global -* vs specific + | `["-*", "x"]` check x | FALSE | -* checked first |
| Prefix grant vs exact deny | `["a.*", "-a.b"]` check a.b | FALSE | Exact deny before wildcards |
| Multi-group conflict | Group A: `-x`, Group B: `x` | NONDETERMINISTIC | HashSet iteration order undefined |
| User deny vs group grant | User: `-x`, Group: `x` | FALSE | User checked before group |
| OP custom perms after restart | OP: `["*", "custom"]` → restart | `["*"]` only | DEFAULT_GROUPS overwrite on load |
| No explicit groups | (empty) | Default group used | Fallback behavior |
| Case sensitivity | `["A.B"]` check `a.b` | FALSE | Case-sensitive |
| Literal * in name | `["a.*.b"]` check `a.x.b` | FALSE | Only trailing * is wildcard |

---

*See [PERMISSIONS_SYSTEM.md](../PERMISSIONS_SYSTEM.md) for complete algorithm documentation*
