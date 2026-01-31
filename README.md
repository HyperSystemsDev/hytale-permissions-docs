# Hytale Permissions Documentation

> **The definitive reference for Hytale server permissions system**
>
> Comprehensive documentation derived from Vineflower-decompiled sources.
> Essential for HyperPerms and custom permission plugin development.

---

## Quick Start

### New to Hytale Permissions?

1. **Read the [Quick Reference](QUICK_REFERENCE.md)** - One-page cheat sheet
2. **Explore [Permission Nodes](PERMISSION_NODES.md)** - All known permissions
3. **Check the [Glossary](GLOSSARY.md)** - Understand the terminology

### Building a Permission Plugin?

1. **Study [Full Documentation](PERMISSIONS_SYSTEM.md)** - Complete system reference
2. **Follow [Best Practices](BEST_PRACTICES.md)** - Design patterns and guidelines
3. **Use the [Provider Template](examples/CustomProviderTemplate.java)** - Starter code
4. **Review [Edge Cases](testing/EDGE_CASES.md)** - For comprehensive testing

### Debugging Issues?

1. **Check [Troubleshooting](TROUBLESHOOTING.md)** - Common issues and solutions
2. **Review [Resolution Flow](diagrams/PERMISSION_RESOLUTION_FLOW.md)** - Visual algorithm

---

## Documentation Index

### Core Documentation

| Document | Description |
|----------|-------------|
| [PERMISSIONS_SYSTEM.md](PERMISSIONS_SYSTEM.md) | Complete system documentation with API reference |
| [QUICK_REFERENCE.md](QUICK_REFERENCE.md) | One-page cheat sheet for daily use |
| [PERMISSION_NODES.md](PERMISSION_NODES.md) | Comprehensive registry of all permission nodes |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | FAQ and debugging guide |

### Reference

| Document | Description |
|----------|-------------|
| [BEST_PRACTICES.md](BEST_PRACTICES.md) | Design patterns, naming conventions, anti-patterns |
| [GLOSSARY.md](GLOSSARY.md) | Term definitions |
| [VERSION.md](VERSION.md) | Source version and decompilation details |
| [CHANGELOG.md](CHANGELOG.md) | Documentation update history |

### Guides

| Document | Description |
|----------|-------------|
| [guides/MIGRATION_GUIDE.md](guides/MIGRATION_GUIDE.md) | Importing from vanilla permissions.json |
| [guides/COMPARISON_MATRIX.md](guides/COMPARISON_MATRIX.md) | Comparison with LuckPerms/PermissionsEx patterns |

### Examples

| File | Description |
|------|-------------|
| [examples/CustomProviderTemplate.java](examples/CustomProviderTemplate.java) | Starter template for custom providers |
| [examples/EventSubscriptionExamples.java](examples/EventSubscriptionExamples.java) | Event listener examples |

### Diagrams

| Document | Description |
|----------|-------------|
| [diagrams/PERMISSION_RESOLUTION_FLOW.md](diagrams/PERMISSION_RESOLUTION_FLOW.md) | Visual flowchart (Mermaid) |

### Testing

| Document | Description |
|----------|-------------|
| [testing/EDGE_CASES.md](testing/EDGE_CASES.md) | Test cases and expected behavior |

---

## Repository Structure

```
hytale-permissions-docs/
├── README.md                          # This file - repository index
├── PERMISSIONS_SYSTEM.md              # Complete system documentation
├── QUICK_REFERENCE.md                 # One-page cheat sheet
├── PERMISSION_NODES.md                # All known permission nodes
├── TROUBLESHOOTING.md                 # FAQ and debugging
├── BEST_PRACTICES.md                  # Patterns and guidelines
├── GLOSSARY.md                        # Term definitions
├── VERSION.md                         # Source and version info
├── CHANGELOG.md                       # Documentation history
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
└── testing/
    └── EDGE_CASES.md                  # Test scenarios
```

---

## Key Concepts

| Concept | Description |
|---------|-------------|
| **Provider Chain** | Multiple providers checked in order; first definitive match wins |
| **Resolution Order** | User perms → Group perms → Virtual groups → Default value |
| **Wildcards** | `*`, `prefix.*`, `-*`, `-prefix.*` for grant/deny patterns |
| **Events** | All permission changes fire events via EventBus |
| **Thread Safety** | Default provider uses ReadWriteLock; custom providers must be thread-safe |

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

// Add to group
perms.addUserToGroup(uuid, "VIP");

// Modify group
perms.addGroupPermission("VIP", Set.of("vip.feature"));
```

---

## Source Packages

All documentation derived from these decompiled packages:

```
com.hypixel.hytale.server.core.permissions
com.hypixel.hytale.server.core.permissions.provider
com.hypixel.hytale.server.core.permissions.commands
com.hypixel.hytale.server.core.permissions.commands.op
com.hypixel.hytale.server.core.event.events.permissions
com.hypixel.hytale.server.core.command.system.exceptions
```

---

## Related Projects

- **[HyperPerms](https://github.com/HyperSystemsDev/HyperPerms)** - Advanced permissions plugin for Hytale
- **[HyperHomes](https://github.com/HyperSystemsDev/HyperHomes)** - Home teleportation plugin
- **[HyperFactions](https://github.com/HyperSystemsDev/HyperFactions)** - Factions plugin

---

## Contributing

Found an error or want to add examples? This documentation is maintained by HyperSystemsDev.

**Discord:** [https://discord.gg/SNPjyfkYPc](https://discord.gg/SNPjyfkYPc)

---

## License

This project is licensed under the [MIT License](LICENSE).

Free to use, modify, and distribute for any purpose.

---

*The definitive reference for Hytale server permissions system*
