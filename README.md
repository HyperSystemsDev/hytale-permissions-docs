# Hytale Permissions Documentation

> **The definitive reference for Hytale server permissions system**
>
> Comprehensive documentation derived from Vineflower-decompiled Hytale server sources.
> Essential for HyperPerms and custom permission plugin development.
>
> **Documentation Version:** 1.1.0 | **Hytale Build:** Pre-release (February 2026) | **Permissions Code:** Unchanged since January 2025

---

## What's New in v1.1.0

This release focuses on documentation accuracy. After cross-referencing all existing docs against the latest decompiled source (Feb 2026), we found multiple inaccuracies, missing information, and one critical behavioral issue that was incorrectly documented.

### Critical Fixes

- **OP/Default Group Overwrite (CRITICAL):** The server forcibly resets OP and Default group permissions on every load using `put()` — any custom permissions added to these groups are silently destroyed on restart. Previously documented as "restored," which undersells the destructive impact. See [PERMISSIONS_SYSTEM.md](PERMISSIONS_SYSTEM.md#notes) for details.
- **`create()` behavior:** Initial `permissions.json` is `{}` (empty object), not the previously documented minimal file with OP/Default groups.

### New Content

- **`hytale.mods.outdated.notify`** permission node — previously undocumented, exists since at least build-10
- **Nondeterministic group iteration warning** — `HashSet` iteration order is undefined; multi-group conflicts produce unpredictable results
- **Multi-provider Default group aggregation** — users can end up in "Default" even with explicit groups if another provider has no data
- **FastUtil internal map types** — internal storage uses `Object2ObjectOpenHashMap`, not standard `HashMap`

See [CHANGELOG.md](CHANGELOG.md) for the complete list.

---

## Quick Start

### New to Hytale Permissions?

1. **Read the [Quick Reference](QUICK_REFERENCE.md)** — One-page cheat sheet with all APIs and patterns
2. **Explore [Permission Nodes](PERMISSION_NODES.md)** — Complete registry of all 20 built-in permission nodes
3. **Check the [Glossary](GLOSSARY.md)** — Understand the terminology

### Building a Permission Plugin?

1. **Study [Full Documentation](PERMISSIONS_SYSTEM.md)** — 12-section complete system reference
2. **Follow [Best Practices](BEST_PRACTICES.md)** — Design patterns, security, and anti-patterns
3. **Use the [Provider Template](examples/CustomProviderTemplate.java)** — Thread-safe starter code
4. **Review [Edge Cases](testing/EDGE_CASES.md)** — 30+ test cases with expected behavior

### Migrating from Other Systems?

1. **Follow the [Migration Guide](guides/MIGRATION_GUIDE.md)** — Step-by-step migration with rollback plan
2. **Check the [Comparison Matrix](guides/COMPARISON_MATRIX.md)** — Side-by-side with LuckPerms/PermissionsEx

### Debugging Issues?

1. **Check [Troubleshooting](TROUBLESHOOTING.md)** — 8 categories of common issues with solutions
2. **Review [Resolution Flow](diagrams/PERMISSION_RESOLUTION_FLOW.md)** — Visual algorithm flowchart

---

## Architecture Overview

```
                     PermissionsModule (Singleton)
                    ┌──────────────────────────────┐
                    │                              │
  hasPermission()   │   Provider Chain             │   setVirtualGroups()
  ─────────────────>│   ┌────────────────────┐     │<──────────────────
                    │   │ Provider[0]         │     │
                    │   │ (HytalePermsProvider)│     │   Virtual Groups
                    │   └────────────────────┘     │   ┌────────────────┐
                    │   ┌────────────────────┐     │   │ Creative →     │
                    │   │ Provider[1]         │     │   │  builderTools  │
                    │   │ (Your Plugin)       │     │   └────────────────┘
                    │   └────────────────────┘     │
                    │                              │
                    │   Events ──> EventBus        │
                    └──────────────────────────────┘
                              │
        ┌─────────────────────┼──────────────────────┐
        ▼                     ▼                      ▼
  PermissionHolder     Commands (/op, /perm)    permissions.json
  (Players, Senders)
```

### Resolution Order

```
User Direct Permissions  →  first definitive match wins
  ↓ (no match)
Group[0] Permissions     →  first definitive match wins
  ↓ (no match)
Group[0] Virtual Perms   →  first definitive match wins
  ↓ (no match)
Group[1] Permissions     →  ...
  ↓ (no match)
Provider[1] (repeat)     →  ...
  ↓ (no match)
Default value (false)
```

---

## Critical Warnings

These behaviors are verified against the decompiled source and affect anyone working with Hytale permissions:

### 1. OP/Default Groups Are Overwritten on Every Server Load

```java
// HytalePermissionsProvider.java:257-259 — after loading JSON:
for (Entry<String, Set<String>> entry : DEFAULT_GROUPS.entrySet()) {
    this.groupPermissions.put(entry.getKey(), new HashSet<>(entry.getValue()));
}
```

This uses `put()`, not `putIfAbsent()`. Any custom permissions you added to the OP or Default groups are **silently destroyed** on every server restart. Only `["*"]` for OP and `[]` for Default survive.

**Fix:** Use custom group names instead of modifying OP/Default.

### 2. Multi-Group Iteration Order Is Nondeterministic

If a user belongs to multiple groups, the iteration order is determined by `HashSet`, which is **undefined**. Groups with conflicting permissions (e.g., Group A grants, Group B denies) produce unpredictable results.

**Fix:** Ensure groups don't conflict, or use explicit user-level permissions.

### 3. Multi-Provider Group Aggregation

`getGroupsForUser()` aggregates from ALL providers. A user with `["VIP"]` in your provider can also end up in `["Default"]` if the vanilla provider has no data for them.

---

## Built-in Permission Nodes

All 20 built-in permission nodes defined in `HytalePermissions.java`:

```
hytale.
├── command.                          # Command execution
│   ├── op.add                        # /op add
│   ├── op.remove                     # /op remove
│   └── <name>                        # Any registered command
├── editor.                           # Creative/editor tools
│   ├── asset                         # Asset editor
│   ├── builderTools                  # Builder tools
│   ├── brush.use / brush.config      # Brush tools
│   ├── prefab.use / prefab.manage    # Prefab tools
│   ├── selection.use / clipboard / modify  # Selection tools
│   ├── history                       # Undo/redo
│   └── packs.create / edit / delete  # Content packs
├── camera.
│   └── flycam                        # Fly camera mode
├── world_map.
│   └── teleport.coordinate / marker  # Map teleportation
├── system.
│   └── update.notify                 # Update notifications
└── mods.
    └── outdated.notify               # Outdated mod notifications
```

### Wildcard Patterns

| Pattern | Effect | Priority |
|---------|--------|----------|
| `*` | Grant ALL | Checked first |
| `-*` | Deny ALL | Checked second |
| `exact.perm` | Grant exact | Checked third |
| `-exact.perm` | Deny exact | Checked fourth |
| `prefix.*` | Grant under prefix | Checked last (shorter prefixes first) |
| `-prefix.*` | Deny under prefix | Checked last (shorter prefixes first) |

Middle wildcards (e.g., `hytale.*.ban`) are **not supported** — the `*` is treated as a literal character.

---

## Common Code Snippets

### Check a Permission

```java
// Via PermissionsModule (works for offline players)
boolean hasPerm = PermissionsModule.get().hasPermission(uuid, "my.permission");

// Via PermissionHolder (for online players)
boolean hasPerm = player.hasPermission("my.permission");

// With custom default value
boolean hasPerm = player.hasPermission("my.permission", true);
```

### Register a Custom Provider

```java
// In your plugin's start() method
PermissionsModule.get().addProvider(myProvider);
```

### Listen for Permission Changes

```java
// Subscribe to PlayerGroupEvent.Added (NOT PlayerPermissionChangeEvent.GroupAdded)
// PlayerGroupEvent.Added/Removed are the events actually dispatched by the system
HytaleServer.get().getEventBus()
    .subscribe(PlayerGroupEvent.Added.class, event -> {
        UUID uuid = event.getPlayerUuid();
        String group = event.getGroupName();
        // React to group membership change
    });

// Listen for direct permission changes
HytaleServer.get().getEventBus()
    .subscribe(PlayerPermissionChangeEvent.PermissionsAdded.class, event -> {
        UUID uuid = event.getPlayerUuid();
        Set<String> added = event.getAddedPermissions();
        // React to changes
    });
```

### Modify Permissions

```java
PermissionsModule perms = PermissionsModule.get();

// Add permissions
perms.addUserPermission(uuid, Set.of("my.permission"));

// Add to group (use custom group names, NOT "OP" or "Default")
perms.addUserToGroup(uuid, "VIP");

// Modify group
perms.addGroupPermission("VIP", Set.of("vip.feature"));
```

---

## Key Concepts

| Concept | Description |
|---------|-------------|
| **Provider Chain** | Multiple providers checked in order; first definitive match (grant OR deny) wins |
| **Resolution Order** | User perms → Group perms → Virtual groups → Default value |
| **Wildcards** | `*`, `prefix.*`, `-*`, `-prefix.*` for grant/deny patterns (trailing only) |
| **Events** | All permission changes fire events via EventBus; use `PlayerGroupEvent`, not inner classes |
| **Thread Safety** | Default provider uses `ReadWriteLock`; internal maps use FastUtil `Object2ObjectOpenHashMap` |
| **Default Groups** | OP (`["*"]`) and Default (`[]`) are forcibly reset on every server load |
| **Virtual Groups** | Context-based permissions; only Creative mode has one by default (`builderTools`) |

---

## Documentation Index

### Core Documentation

| Document | Description |
|----------|-------------|
| [PERMISSIONS_SYSTEM.md](PERMISSIONS_SYSTEM.md) | Complete system documentation — 12 sections covering architecture, API, resolution algorithm, events, commands, storage format |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | One-page cheat sheet for daily use |
| [PERMISSION_NODES.md](PERMISSION_NODES.md) | Complete registry of all 20 built-in permission nodes with constants, wildcards, and generation helpers |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | 8-section debugging guide with diagnostic steps, solutions, and common mistakes |

### Reference

| Document | Description |
|----------|-------------|
| [BEST_PRACTICES.md](BEST_PRACTICES.md) | Naming conventions, provider patterns, security, performance, anti-patterns, testing |
| [GLOSSARY.md](GLOSSARY.md) | 25+ term definitions with quick reference table |
| [VERSION.md](VERSION.md) | Source version, decompilation details, 17 analyzed files |
| [CHANGELOG.md](CHANGELOG.md) | Documentation update history (Keep a Changelog format) |

### Guides

| Document | Description |
|----------|-------------|
| [guides/MIGRATION_GUIDE.md](guides/MIGRATION_GUIDE.md) | Full migration guide: export, import, database schema, strategies, rollback plan |
| [guides/COMPARISON_MATRIX.md](guides/COMPARISON_MATRIX.md) | Feature-by-feature comparison with LuckPerms/PermissionsEx/UltraPermissions |

### Examples

| File | Description |
|------|-------------|
| [examples/CustomProviderTemplate.java](examples/CustomProviderTemplate.java) | Production-ready template with thread safety notes and Default group semantics |
| [examples/EventSubscriptionExamples.java](examples/EventSubscriptionExamples.java) | All 6 event types with real-world usage patterns and anti-patterns |

### Diagrams & Testing

| Document | Description |
|----------|-------------|
| [diagrams/PERMISSION_RESOLUTION_FLOW.md](diagrams/PERMISSION_RESOLUTION_FLOW.md) | Mermaid flowcharts for resolution algorithm, wildcard checking, provider chain |
| [testing/EDGE_CASES.md](testing/EDGE_CASES.md) | 30+ test cases: wildcard conflicts, group inheritance, multi-provider, concurrency, null handling |

---

## Repository Structure

```
hytale-permissions-docs/
├── README.md                          # This file
├── PERMISSIONS_SYSTEM.md              # Complete system documentation (1060+ lines)
├── QUICK_REFERENCE.md                 # One-page cheat sheet
├── PERMISSION_NODES.md                # All 20 built-in permission nodes
├── TROUBLESHOOTING.md                 # FAQ and debugging
├── BEST_PRACTICES.md                  # Patterns and guidelines
├── GLOSSARY.md                        # Term definitions
├── VERSION.md                         # Source and version info
├── CHANGELOG.md                       # Documentation history
├── .gitignore                         # Excludes decompiled/ from VCS
│
├── guides/
│   ├── MIGRATION_GUIDE.md             # Vanilla to custom migration
│   └── COMPARISON_MATRIX.md           # LuckPerms/PEX comparison
│
├── examples/
│   ├── CustomProviderTemplate.java    # Provider starter template
│   └── EventSubscriptionExamples.java # Event handling examples
│
├── diagrams/
│   └── PERMISSION_RESOLUTION_FLOW.md  # Visual algorithm flowchart
│
├── testing/
│   └── EDGE_CASES.md                  # Test scenarios and expected behavior
│
└── decompiled/                        # Local only (gitignored)
    └── latest/                        # Decompiled HytaleServer.jar sources
```

---

## Source Packages

All documentation derived from 17 decompiled source files across these packages:

```
com.hypixel.hytale.server.core.permissions           # PermissionsModule, HytalePermissions, PermissionHolder
com.hypixel.hytale.server.core.permissions.provider   # PermissionProvider, HytalePermissionsProvider
com.hypixel.hytale.server.core.permissions.commands    # /perm user, /perm group, /perm test
com.hypixel.hytale.server.core.permissions.commands.op # /op self, /op add, /op remove
com.hypixel.hytale.server.core.event.events.permissions # PlayerPermissionChangeEvent, PlayerGroupEvent, GroupPermissionChangeEvent
com.hypixel.hytale.server.core.command.system.exceptions # NoPermissionException
```

**JAR Location:** `AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`

---

## Related Projects

- **[HyperPerms](https://github.com/HyperSystemsDev/HyperPerms)** — Advanced permissions plugin for Hytale (database-backed, inheritance, temporary perms)
- **[HyperHomes](https://github.com/HyperSystemsDev/HyperHomes)** — Home teleportation plugin
- **[HyperFactions](https://github.com/HyperSystemsDev/HyperFactions)** — Factions plugin

---

## Contributing

Found an error or want to add examples? This documentation is maintained by HyperSystemsDev.

Every claim in this documentation is verified against the decompiled source. If you find a discrepancy, please report it — this repo is the source of truth for HyperPerms.

**Discord:** [https://discord.gg/SNPjyfkYPc](https://discord.gg/SNPjyfkYPc)

---

## License

This project is licensed under the [MIT License](LICENSE).

Free to use, modify, and distribute for any purpose.

---

*The definitive reference for Hytale server permissions system — v1.1.0*
