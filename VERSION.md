# Version Information

> **Source documentation for Hytale permissions system**

---

## Decompilation Details

| Property | Value |
|----------|-------|
| **Hytale Build** | Pre-release (January 2025) |
| **Decompiler** | Vineflower |
| **Decompilation Date** | January 2025 |
| **Documentation Version** | 1.0.0 |

---

## Source Packages

The following packages were analyzed:

```
com.hypixel.hytale.server.core.permissions
com.hypixel.hytale.server.core.permissions.provider
com.hypixel.hytale.server.core.permissions.commands
com.hypixel.hytale.server.core.permissions.commands.op
com.hypixel.hytale.server.core.event.events.permissions
com.hypixel.hytale.server.core.command.system.exceptions
```

---

## Source File Count

| Category | Files |
|----------|-------|
| Core Permissions | 3 |
| Providers | 2 |
| Commands | 8 |
| Events | 3 |
| Exceptions | 1 |
| **Total** | **17** |

---

## Analyzed Files

### Core Permissions
- `PermissionHolder.java` - Base interface for permission holders
- `PermissionsModule.java` - Central permissions coordinator
- `HytalePermissions.java` - Built-in permission constants

### Providers
- `PermissionProvider.java` - Provider interface
- `HytalePermissionsProvider.java` - Default file-based implementation

### Commands
- `PermCommand.java` - Base /perm command
- `PermUserCommand.java` - User permission subcommands
- `PermGroupCommand.java` - Group permission subcommands
- `PermTestCommand.java` - Permission testing command
- `OpCommand.java` - Base /op command
- `OpSelfCommand.java` - Toggle own OP status
- `OpAddCommand.java` - Add player to OP
- `OpRemoveCommand.java` - Remove player from OP

### Events
- `PlayerPermissionChangeEvent.java` - User permission changes
- `GroupPermissionChangeEvent.java` - Group permission changes
- `PlayerGroupEvent.java` - Group membership changes

### Exceptions
- `NoPermissionException.java` - Permission denied exception

---

## Compatibility Notes

This documentation reflects the Hytale permissions system as of the decompilation date. Future Hytale updates may introduce breaking changes.

**Known stable interfaces:**
- `PermissionProvider` - Core provider interface
- `PermissionHolder` - Permission checking interface
- Event classes and their structure

**May change:**
- Built-in permission node names
- Command syntax and subcommands
- Default group configurations

---

## Update Policy

This documentation will be updated when:
1. New Hytale builds introduce permissions changes
2. Community discovers undocumented features
3. HyperPerms development requires clarification

---

*Last updated: January 2025*
